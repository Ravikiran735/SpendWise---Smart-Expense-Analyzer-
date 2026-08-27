/**
 * SpendWise — Smart Import Hub & Processing Module
 * Manages file uploads, preview modals, column mapping, and Cloud Firestore commits.
 */

let smartImportState = {
    currentRawTable: [],
    currentMapping: null,
    currentFileName: '',
    currentSourceType: 'CSV',
    currentCandidates: [],
    selectedFilter: 'all',
    historyUnsubscriber: null
};

// Initialize Smart Import Hub
function initSmartImport() {
    initDropzone();
    initReviewModalEvents();
    initColumnMappingEvents();
}

function initDropzone() {
    const dropzone = document.getElementById('smart-import-dropzone');
    const fileInput = document.getElementById('smart-import-file-input');

    if (dropzone && fileInput) {
        dropzone.addEventListener('click', () => fileInput.click());

        dropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropzone.classList.add('drag-active');
        });

        dropzone.addEventListener('dragleave', () => {
            dropzone.classList.remove('drag-active');
        });

        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropzone.classList.remove('drag-active');
            if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                processUploadedFile(e.dataTransfer.files[0]);
            }
        });

        fileInput.addEventListener('change', (e) => {
            if (e.target.files && e.target.files[0]) {
                processUploadedFile(e.target.files[0]);
            }
        });
    }
}

async function processUploadedFile(file) {
    if (!file) return;
    const fileName = file.name;
    const isExcel = fileName.endsWith('.xlsx') || fileName.endsWith('.xls');
    const sourceType = isExcel ? 'EXCEL' : 'CSV';

    smartImportState.currentFileName = fileName;
    smartImportState.currentSourceType = sourceType;

    showToast(`Reading ${fileName}...`, 'info');

    const reader = new FileReader();

    if (isExcel) {
        reader.onload = async (e) => {
            try {
                const arrayBuffer = e.target.result;
                const rawTable = window.ImportAnalyzer.parseExcel(arrayBuffer);
                handleParsedRawData(rawTable, fileName, sourceType);
            } catch (err) {
                console.error(err);
                showToast(`Excel parsing failed: ${err.message}`, 'danger');
            }
        };
        reader.readAsArrayBuffer(file);
    } else {
        reader.onload = async (e) => {
            try {
                const text = e.target.result;
                const rawTable = window.ImportAnalyzer.parseCsv(text);
                handleParsedRawData(rawTable, fileName, sourceType);
            } catch (err) {
                console.error(err);
                showToast(`CSV parsing failed: ${err.message}`, 'danger');
            }
        };
        reader.readAsText(file);
    }
}

async function handleParsedRawData(rawTable, fileName, sourceType) {
    if (!rawTable || rawTable.length === 0) {
        showToast('The uploaded file contains no data.', 'warning');
        return;
    }

    smartImportState.currentRawTable = rawTable;
    const headers = rawTable[0] || [];
    const mapping = window.ImportAnalyzer.detectColumnMapping(headers);
    smartImportState.currentMapping = mapping;

    if (mapping.isValid) {
        await executeAnalysis(rawTable, mapping, sourceType, fileName);
    } else {
        openColumnMappingModal(headers, mapping);
    }
}

async function executeAnalysis(rawTable, mapping, sourceType, fileName) {
    showToast('Analyzing transactions and checking duplicates...', 'info');

    const prefs = appState.userSettings || {};
    const candidates = await window.ImportAnalyzer.analyzeRows(
        rawTable,
        mapping,
        sourceType,
        appState.expenses || [],
        appState.incomes || [],
        prefs
    );

    smartImportState.currentCandidates = candidates;
    openImportReviewModal(candidates, fileName);
}

/* ==========================================================================
   Column Mapping Modal
   ========================================================================== */
function openColumnMappingModal(headers, initialMapping) {
    const modal = document.getElementById('column-mapping-modal');
    if (!modal) return;

    populateDropdown('map-col-date', headers, initialMapping.dateIdx >= 0 ? initialMapping.dateIdx : 0);
    populateDropdown('map-col-desc', headers, initialMapping.descIdx >= 0 ? initialMapping.descIdx : 1);
    populateDropdown('map-col-amount', headers, initialMapping.amountIdx >= 0 ? initialMapping.amountIdx : 2);
    populateDropdown('map-col-category', headers, initialMapping.categoryIdx, true);
    populateDropdown('map-col-payment', headers, initialMapping.paymentMethodIdx, true);

    modal.classList.add('active');
}

function populateDropdown(selectId, headers, selectedIdx, allowNone = false) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.innerHTML = '';

    if (allowNone) {
        const opt = document.createElement('option');
        opt.value = '-1';
        opt.textContent = '— None / Not in file —';
        if (selectedIdx === -1) opt.selected = true;
        select.appendChild(opt);
    }

    headers.forEach((h, idx) => {
        const opt = document.createElement('option');
        opt.value = idx;
        opt.textContent = `${h} (Column ${idx + 1})`;
        if (idx === selectedIdx) opt.selected = true;
        select.appendChild(opt);
    });
}

function initColumnMappingEvents() {
    const btnApply = document.getElementById('btn-apply-column-mapping');
    if (btnApply) {
        btnApply.addEventListener('click', async () => {
            const dateIdx = parseInt(document.getElementById('map-col-date').value, 10);
            const descIdx = parseInt(document.getElementById('map-col-desc').value, 10);
            const amountIdx = parseInt(document.getElementById('map-col-amount').value, 10);
            const categoryIdx = parseInt(document.getElementById('map-col-category').value, 10);
            const paymentMethodIdx = parseInt(document.getElementById('map-col-payment').value, 10);

            const mapping = {
                dateIdx,
                descIdx,
                amountIdx,
                categoryIdx,
                paymentMethodIdx,
                isValid: dateIdx >= 0 && amountIdx >= 0
            };

            smartImportState.currentMapping = mapping;
            closeModal('column-mapping-modal');
            await executeAnalysis(
                smartImportState.currentRawTable,
                mapping,
                smartImportState.currentSourceType,
                smartImportState.currentFileName
            );
        });
    }
}

/* ==========================================================================
   Import Review Modal
   ========================================================================== */
function openImportReviewModal(candidates, fileName) {
    const modal = document.getElementById('import-review-modal');
    if (!modal) return;

    document.getElementById('review-modal-filename').textContent = fileName;
    renderReviewTable(candidates, smartImportState.selectedFilter);
    modal.classList.add('active');
}

function renderReviewTable(candidates, filter) {
    const totalCount = candidates.length;
    const newCount = candidates.filter(c => c.status === 'NEW').length;
    const dupCount = candidates.filter(c => c.status === 'DUPLICATE').length;
    const revCount = candidates.filter(c => c.status === 'NEEDS_REVIEW').length;
    const invCount = candidates.filter(c => c.status === 'INVALID').length;
    const selCount = candidates.filter(c => c.isSelected && c.status !== 'DUPLICATE' && c.status !== 'INVALID').length;

    // Update Counters
    document.getElementById('rev-stat-total').textContent = totalCount;
    document.getElementById('rev-stat-new').textContent = newCount;
    document.getElementById('rev-stat-dup').textContent = dupCount;
    document.getElementById('rev-stat-rev').textContent = revCount;
    document.getElementById('rev-stat-inv').textContent = invCount;

    const btnCommit = document.getElementById('btn-commit-import');
    if (btnCommit) {
        btnCommit.disabled = selCount === 0;
        btnCommit.innerHTML = `<i class="fa-solid fa-circle-check"></i> Import Selected (${selCount})`;
    }

    const filtered = candidates.filter(c => {
        if (filter === 'new') return c.status === 'NEW';
        if (filter === 'needs_review') return c.status === 'NEEDS_REVIEW';
        if (filter === 'duplicates') return c.status === 'DUPLICATE';
        if (filter === 'invalid') return c.status === 'INVALID';
        return true;
    });

    const tbody = document.getElementById('review-table-tbody');
    tbody.innerHTML = '';

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center p-4 text-muted">No records match the selected filter.</td></tr>`;
        return;
    }

    const categoriesList = [
        'Food', 'Transport', 'Shopping', 'Rent', 'Utilities', 'Education',
        'Healthcare', 'Entertainment', 'Subscriptions', 'Travel', 'Investment',
        'Salary', 'Freelance', 'Business', 'Gift', 'Other'
    ];

    filtered.forEach(c => {
        const tr = document.createElement('tr');
        if (!c.isSelected && c.status !== 'DUPLICATE' && c.status !== 'INVALID') {
            tr.classList.add('row-deselected');
        }

        const isSelectable = c.status !== 'DUPLICATE' && c.status !== 'INVALID';
        const badgeClass = c.status === 'NEW' ? 'badge-emerald' :
            (c.status === 'DUPLICATE' ? 'badge-neutral' :
                (c.status === 'NEEDS_REVIEW' ? 'badge-amber' : 'badge-rose'));

        tr.innerHTML = `
            <td>
                <input type="checkbox" class="cand-checkbox" data-id="${c.id}" ${c.isSelected ? 'checked' : ''} ${!isSelectable ? 'disabled' : ''}>
            </td>
            <td>
                <span class="status-pill ${badgeClass}">${c.status.replace('_', ' ')}</span>
                ${c.statusReason ? `<small class="status-subtext">${c.statusReason}</small>` : ''}
            </td>
            <td>
                <input type="date" class="form-control form-control-sm cand-date" data-id="${c.id}" value="${c.dateStr}">
            </td>
            <td>
                <input type="text" class="form-control form-control-sm cand-desc" data-id="${c.id}" value="${escapeHtml(c.description)}">
            </td>
            <td>
                <select class="form-control form-control-sm cand-type" data-id="${c.id}">
                    <option value="Expense" ${c.type === 'Expense' ? 'selected' : ''}>Expense</option>
                    <option value="Income" ${c.type === 'Income' ? 'selected' : ''}>Income</option>
                </select>
            </td>
            <td>
                <select class="form-control form-control-sm cand-cat" data-id="${c.id}">
                    ${categoriesList.map(cat => `<option value="${cat}" ${cat.toLowerCase() === c.category.toLowerCase() ? 'selected' : ''}>${cat}</option>`).join('')}
                </select>
            </td>
            <td>
                <input type="number" step="0.01" class="form-control form-control-sm cand-amount" data-id="${c.id}" value="${c.amount.toFixed(2)}">
            </td>
        `;

        tbody.appendChild(tr);
    });

    // Attach row input change listeners
    tbody.querySelectorAll('.cand-checkbox').forEach(cb => {
        cb.addEventListener('change', (e) => {
            const id = e.target.getAttribute('data-id');
            const item = smartImportState.currentCandidates.find(x => x.id === id);
            if (item) {
                item.isSelected = e.target.checked;
                renderReviewTable(smartImportState.currentCandidates, smartImportState.selectedFilter);
            }
        });
    });

    tbody.querySelectorAll('.cand-desc').forEach(input => {
        input.addEventListener('change', (e) => {
            const id = e.target.getAttribute('data-id');
            const item = smartImportState.currentCandidates.find(x => x.id === id);
            if (item) item.description = e.target.value;
        });
    });

    tbody.querySelectorAll('.cand-cat').forEach(sel => {
        sel.addEventListener('change', (e) => {
            const id = e.target.getAttribute('data-id');
            const item = smartImportState.currentCandidates.find(x => x.id === id);
            if (item) {
                item.category = e.target.value;
                if (item.status === 'NEEDS_REVIEW') {
                    item.status = 'NEW';
                    item.statusReason = 'Manually verified';
                    renderReviewTable(smartImportState.currentCandidates, smartImportState.selectedFilter);
                }
            }
        });
    });

    tbody.querySelectorAll('.cand-type').forEach(sel => {
        sel.addEventListener('change', (e) => {
            const id = e.target.getAttribute('data-id');
            const item = smartImportState.currentCandidates.find(x => x.id === id);
            if (item) item.type = e.target.value;
        });
    });

    tbody.querySelectorAll('.cand-amount').forEach(input => {
        input.addEventListener('change', (e) => {
            const id = e.target.getAttribute('data-id');
            const item = smartImportState.currentCandidates.find(x => x.id === id);
            if (item) item.amount = Math.abs(parseFloat(e.target.value) || 0);
        });
    });
}

function initReviewModalEvents() {
    // Filter Buttons
    document.querySelectorAll('.rev-filter-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            document.querySelectorAll('.rev-filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            smartImportState.selectedFilter = btn.getAttribute('data-filter');
            renderReviewTable(smartImportState.currentCandidates, smartImportState.selectedFilter);
        });
    });

    // Select All / Deselect All
    const btnSelectAll = document.getElementById('btn-rev-select-all');
    if (btnSelectAll) {
        btnSelectAll.addEventListener('click', () => {
            smartImportState.currentCandidates.forEach(c => {
                if (c.status !== 'DUPLICATE' && c.status !== 'INVALID') {
                    c.isSelected = true;
                }
            });
            renderReviewTable(smartImportState.currentCandidates, smartImportState.selectedFilter);
        });
    }

    const btnDeselectAll = document.getElementById('btn-rev-deselect-all');
    if (btnDeselectAll) {
        btnDeselectAll.addEventListener('click', () => {
            smartImportState.currentCandidates.forEach(c => c.isSelected = false);
            renderReviewTable(smartImportState.currentCandidates, smartImportState.selectedFilter);
        });
    }

    // Commit Import Button
    const btnCommit = document.getElementById('btn-commit-import');
    if (btnCommit) {
        btnCommit.addEventListener('click', () => {
            commitImportToFirestore();
        });
    }
}

async function commitImportToFirestore() {
    if (!appState.currentUser) {
        showToast('Please sign in to import records.', 'warning');
        return;
    }

    const userId = appState.currentUser.uid;
    const candidates = smartImportState.currentCandidates;
    const toImport = candidates.filter(c => c.isSelected && c.status !== 'DUPLICATE' && c.status !== 'INVALID');

    if (toImport.length === 0) {
        showToast('No valid transactions selected.', 'warning');
        return;
    }

    const btnCommit = document.getElementById('btn-commit-import');
    btnCommit.disabled = true;
    btnCommit.innerHTML = `<span class="spinner-small"></span> Importing...`;

    try {
        const importId = 'imp_' + Math.random().toString(36).substring(2, 9) + Date.now();
        const dupCount = candidates.filter(c => c.status === 'DUPLICATE').length;
        const revCount = candidates.filter(c => c.status === 'NEEDS_REVIEW').length;

        // 1. Write Import History document
        const historyRecord = {
            importId,
            userId,
            fileName: smartImportState.currentFileName || 'Smart Import',
            sourceType: smartImportState.currentSourceType,
            totalRecords: candidates.length,
            newRecords: toImport.length,
            duplicateRecords: dupCount,
            reviewRecords: revCount,
            status: 'COMPLETED',
            createdAt: firebase.firestore.FieldValue.serverTimestamp()
        };

        await db.collection('users').doc(userId)
            .collection('importHistory').doc(importId)
            .set(historyRecord);

        // 2. Batch write transactions (chunks of 450)
        const chunks = [];
        for (let i = 0; i < toImport.length; i += 450) {
            chunks.push(toImport.slice(i, i + 450));
        }

        let expCount = 0;
        let incCount = 0;

        for (const chunk of chunks) {
            const batch = db.batch();
            for (const item of chunk) {
                const isIncome = item.type === 'Income';
                const colName = isIncome ? 'incomes' : 'expenses';
                const docRef = db.collection('users').doc(userId).collection(colName).doc();

                if (isIncome) {
                    batch.set(docRef, {
                        id: docRef.id,
                        userId,
                        amount: item.amount,
                        source: item.category || 'Other',
                        description: item.description,
                        date: firebase.firestore.Timestamp.fromDate(item.date),
                        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
                        updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
                        origin: item.source || 'CSV',
                        importId,
                        paymentMethod: item.paymentMethod || 'Bank Transfer',
                        reviewStatus: item.status === 'NEEDS_REVIEW' ? 'needs_review' : 'confirmed'
                    });
                    incCount++;
                } else {
                    batch.set(docRef, {
                        id: docRef.id,
                        userId,
                        amount: item.amount,
                        category: item.category || 'Other',
                        description: item.description,
                        paymentMethod: item.paymentMethod || 'UPI',
                        date: firebase.firestore.Timestamp.fromDate(item.date),
                        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
                        updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
                        source: item.source || 'CSV',
                        importId,
                        reviewStatus: item.status === 'NEEDS_REVIEW' ? 'needs_review' : 'confirmed'
                    });
                    expCount++;
                }
            }
            await batch.commit();
        }

        closeModal('import-review-modal');
        showToast(`Successfully imported ${toImport.length} transactions (${expCount} expenses, ${incCount} incomes). ${dupCount} duplicates prevented.`, 'success');

        // Clear local import buffer
        smartImportState.currentCandidates = [];
        smartImportState.currentRawTable = [];
    } catch (err) {
        console.error(err);
        showToast(`Import failed: ${err.message}`, 'danger');
    } finally {
        if (btnCommit) {
            btnCommit.disabled = false;
            btnCommit.innerHTML = `<i class="fa-solid fa-circle-check"></i> Import Selected`;
        }
    }
}

/* ==========================================================================
   Import History & Dashboard Live Synchronization
   ========================================================================== */
function setupImportHistoryListener(userId) {
    if (smartImportState.historyUnsubscriber) {
        smartImportState.historyUnsubscriber();
    }

    smartImportState.historyUnsubscriber = db.collection('users').doc(userId)
        .collection('importHistory')
        .orderBy('createdAt', 'desc')
        .onSnapshot(snapshot => {
            const history = [];
            snapshot.forEach(doc => {
                const data = doc.data();
                history.push({
                    id: doc.id,
                    ...data,
                    createdAt: data.createdAt ? data.createdAt.toDate() : new Date()
                });
            });

            appState.importHistory = history;
            renderSmartImportTab(history);
            renderDashboardSmartImportCard(history);
        }, err => {
            console.error('Error fetching import history:', err);
        });
}

function renderSmartImportTab(history) {
    // 1. Render Analytics Counters
    const totalImports = history.length;
    let totalImported = 0;
    let totalDuplicates = 0;
    let totalNeedsReview = 0;
    let csvCount = 0, excelCount = 0, receiptCount = 0, voiceCount = 0, manualCount = 0;

    history.forEach(h => {
        totalImported += (h.newRecords || 0);
        totalDuplicates += (h.duplicateRecords || 0);
        totalNeedsReview += (h.reviewRecords || 0);
        const src = (h.sourceType || 'CSV').toUpperCase();
        if (src === 'CSV') csvCount++;
        else if (src === 'EXCEL') excelCount++;
        else if (src === 'RECEIPT') receiptCount++;
        else if (src === 'VOICE') voiceCount++;
        else if (src === 'MANUAL') manualCount++;
    });

    const elTotalImp = document.getElementById('si-stat-total-imports');
    const elTxImp = document.getElementById('si-stat-tx-imported');
    const elDupPrev = document.getElementById('si-stat-dup-prevented');
    const elNeedsRev = document.getElementById('si-stat-needs-review');

    if (elTotalImp) elTotalImp.textContent = totalImports;
    if (elTxImp) elTxImp.textContent = totalImported;
    if (elDupPrev) elDupPrev.textContent = totalDuplicates;
    if (elNeedsRev) elNeedsRev.textContent = totalNeedsReview;

    const elBreakdown = document.getElementById('si-source-breakdown-row');
    if (elBreakdown) {
        elBreakdown.innerHTML = `
            <div class="source-chip"><span class="chip-src">CSV</span> <strong>${csvCount}</strong></div>
            <div class="source-chip"><span class="chip-src">Excel</span> <strong>${excelCount}</strong></div>
            <div class="source-chip"><span class="chip-src">Receipt</span> <strong>${receiptCount}</strong></div>
            <div class="source-chip"><span class="chip-src">Voice</span> <strong>${voiceCount}</strong></div>
            <div class="source-chip"><span class="chip-src">Manual</span> <strong>${manualCount}</strong></div>
        `;
    }

    // 2. Render Import History Ledger
    const historyContainer = document.getElementById('smart-import-history-list');
    if (!historyContainer) return;

    if (history.length === 0) {
        historyContainer.innerHTML = `
            <div class="empty-state-box">
                <i class="fa-solid fa-clock-rotate-left"></i>
                <p>No import sessions recorded yet.</p>
                <small>Import your first CSV or Excel statement above to start organizing.</small>
            </div>
        `;
        return;
    }

    historyContainer.innerHTML = history.map(h => {
        const iconClass = (h.sourceType || 'CSV').toUpperCase() === 'EXCEL' ? 'fa-file-excel text-emerald' :
            ((h.sourceType || 'CSV').toUpperCase() === 'RECEIPT' ? 'fa-receipt text-amber' :
                ((h.sourceType || 'CSV').toUpperCase() === 'VOICE' ? 'fa-microphone text-sky' : 'fa-file-csv text-primary'));

        const dateFormatted = h.createdAt.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });

        return `
            <div class="import-history-row-card">
                <div class="ih-left">
                    <div class="ih-icon"><i class="fa-solid ${iconClass}"></i></div>
                    <div class="ih-info">
                        <h4>${escapeHtml(h.fileName || 'Import Batch')}</h4>
                        <p>${h.totalRecords} records • ${h.newRecords} imported • ${h.duplicateRecords} duplicates • ${h.reviewRecords} review</p>
                    </div>
                </div>
                <div class="ih-right">
                    <span class="status-pill badge-emerald">${h.status || 'COMPLETED'}</span>
                    <small class="ih-date">${dateFormatted}</small>
                </div>
            </div>
        `;
    }).join('');
}

function renderDashboardSmartImportCard(history) {
    const lastImport = history[0];
    const totalImported = history.reduce((sum, h) => sum + (h.newRecords || 0), 0);
    const totalDuplicates = history.reduce((sum, h) => sum + (h.duplicateRecords || 0), 0);
    const totalReview = history.reduce((sum, h) => sum + (h.reviewRecords || 0), 0);

    const elLastDate = document.getElementById('dash-si-last-date');
    const elTx = document.getElementById('dash-si-imported');
    const elDup = document.getElementById('dash-si-duplicates');
    const elRev = document.getElementById('dash-si-review');
    const elBadge = document.getElementById('dash-si-total-badge');

    if (elLastDate) {
        elLastDate.textContent = lastImport
            ? `${lastImport.createdAt.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })} (${lastImport.fileName || 'Session'})`
            : 'No imports yet';
    }
    if (elTx) elTx.textContent = totalImported;
    if (elDup) elDup.textContent = totalDuplicates;
    if (elRev) elRev.textContent = totalReview;
    if (elBadge) elBadge.textContent = `${history.length} Imports`;
}

function escapeHtml(str) {
    return String(str || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

window.initSmartImport = initSmartImport;
window.setupImportHistoryListener = setupImportHistoryListener;
