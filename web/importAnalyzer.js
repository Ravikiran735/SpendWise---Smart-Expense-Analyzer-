/**
 * SpendWise — Smart Import Analyzer & File Parser
 * Supports CSV and Excel (.xlsx, .xls) parsing, column auto-detection,
 * date/amount normalization, duplicate detection, and category scoring.
 */

const ImportAnalyzer = {
    /**
     * Parse raw CSV text according to RFC-4180
     */
    parseCsv(csvText) {
        const rows = [];
        let currentRow = [];
        let currentField = '';
        let inQuotes = false;
        let i = 0;

        while (i < csvText.length) {
            const char = csvText[i];
            const nextChar = csvText[i + 1];

            if (char === '"') {
                if (inQuotes && nextChar === '"') {
                    currentField += '"';
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (char === ',' && !inQuotes) {
                currentRow.push(currentField.trim());
                currentField = '';
            } else if ((char === '\r' || char === '\n') && !inQuotes) {
                if (char === '\r' && nextChar === '\n') i++;
                currentRow.push(currentField.trim());
                if (currentRow.some(f => f !== '')) {
                    rows.push(currentRow);
                }
                currentRow = [];
                currentField = '';
            } else {
                currentField += char;
            }
            i++;
        }

        if (currentField !== '' || currentRow.length > 0) {
            currentRow.push(currentField.trim());
            if (currentRow.some(f => f !== '')) {
                rows.push(currentRow);
            }
        }

        return rows;
    },

    /**
     * Parse Excel (.xlsx, .xls) using SheetJS library
     */
    parseExcel(arrayBuffer) {
        if (typeof XLSX === 'undefined') {
            throw new Error('Excel parsing library (SheetJS) is not loaded.');
        }
        const workbook = XLSX.read(arrayBuffer, { type: 'array', cellDates: true });
        const firstSheetName = workbook.SheetNames[0];
        const worksheet = workbook.Sheets[firstSheetName];
        const rawTable = XLSX.utils.sheet_to_json(worksheet, { header: 1, defval: '' });
        // Clean and filter empty rows
        return rawTable
            .map(row => row.map(cell => (cell !== null && cell !== undefined ? String(cell).trim() : '')))
            .filter(row => row.some(cell => cell !== ''));
    },

    /**
     * Automatically detect column indices from header names
     */
    detectColumnMapping(headers) {
        let dateIdx = -1;
        let descIdx = -1;
        let amountIdx = -1;
        let debitIdx = -1;
        let creditIdx = -1;
        let categoryIdx = -1;
        let paymentMethodIdx = -1;
        let typeIdx = -1;

        headers.forEach((header, index) => {
            const h = (header || '').toLowerCase().trim().replace(/[_-]/g, ' ');
            if (dateIdx === -1 && (h.includes('date') || h.includes('time') || h === 'dt')) {
                dateIdx = index;
            } else if (descIdx === -1 && (h.includes('desc') || h.includes('particular') || h.includes('narration') ||
                h.includes('merchant') || h.includes('payee') || h.includes('detail') ||
                h.includes('remark') || h.includes('note') || h === 'name' || h === 'title')) {
                descIdx = index;
            } else if (debitIdx === -1 && (h === 'debit' || h === 'dr' || h.includes('withdrawal') || h.includes('spent') || h === 'expense')) {
                debitIdx = index;
            } else if (creditIdx === -1 && (h === 'credit' || h === 'cr' || h.includes('deposit') || h.includes('received') || h === 'income')) {
                creditIdx = index;
            } else if (amountIdx === -1 && (h === 'amount' || h.includes('amount') || h === 'sum' || h === 'value' || h === 'total')) {
                amountIdx = index;
            } else if (categoryIdx === -1 && (h.includes('category') || h === 'cat' || h === 'tag')) {
                categoryIdx = index;
            } else if (paymentMethodIdx === -1 && (h.includes('payment') || h.includes('mode') || h.includes('channel') || h.includes('method'))) {
                paymentMethodIdx = index;
            } else if (typeIdx === -1 && (h === 'type' || h === 'transaction type' || h === 'txn type' || h === 'cr/dr')) {
                typeIdx = index;
            }
        });

        const isValid = dateIdx >= 0 && (amountIdx >= 0 || (debitIdx >= 0 || creditIdx >= 0));

        return {
            dateIdx,
            descIdx,
            amountIdx,
            debitIdx,
            creditIdx,
            categoryIdx,
            paymentMethodIdx,
            typeIdx,
            isValid
        };
    },

    /**
     * Parse Date across standard formats
     */
    parseDate(dateStr) {
        if (!dateStr) return null;
        if (dateStr instanceof Date && !isNaN(dateStr.getTime())) return dateStr;

        const str = String(dateStr).trim();
        // Try native Date.parse
        const nativeParsed = new Date(str);
        if (!isNaN(nativeParsed.getTime()) && nativeParsed.getFullYear() > 1990 && nativeParsed.getFullYear() < 2100) {
            return nativeParsed;
        }

        // Try dd/mm/yyyy or dd-mm-yyyy or yyyy-mm-dd
        const dmy = str.match(/^(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{4})$/);
        if (dmy) {
            const day = parseInt(dmy[1], 10);
            const month = parseInt(dmy[2], 10) - 1;
            const year = parseInt(dmy[3], 10);
            const d = new Date(year, month, day);
            if (!isNaN(d.getTime())) return d;
        }

        const ymd = str.match(/^(\d{4})[\/\-](\d{1,2})[\/\-](\d{1,2})$/);
        if (ymd) {
            const year = parseInt(ymd[1], 10);
            const month = parseInt(ymd[2], 10) - 1;
            const day = parseInt(ymd[3], 10);
            const d = new Date(year, month, day);
            if (!isNaN(d.getTime())) return d;
        }

        return null;
    },

    /**
     * Parse Amount from messy formatted string
     */
    parseAmount(amtStr) {
        if (!amtStr) return 0;
        if (typeof amtStr === 'number') return Math.abs(amtStr);
        const clean = String(amtStr)
            .replace(/[₹$€£,]/g, '')
            .replace(/\s+/g, '')
            .replace(/CR/gi, '')
            .replace(/DR/gi, '')
            .trim();
        const num = parseFloat(clean);
        return isNaN(num) ? 0 : Math.abs(num);
    },

    /**
     * Full Pipeline: Process raw rows into candidate transactions
     */
    async analyzeRows(rawRows, mapping, sourceType, existingExpenses = [], existingIncomes = [], prefs = {}) {
        if (!rawRows || rawRows.length === 0) return [];

        const enableDuplicateDetection = prefs.duplicateDetection !== false;
        const enableAutoCategorization = prefs.autoCategorization !== false;

        // Skip header row if present
        let dataRows = rawRows;
        if (rawRows.length > 0 && mapping.dateIdx >= 0) {
            const firstCell = String(rawRows[0][mapping.dateIdx] || '').toLowerCase();
            if (firstCell.includes('date') || firstCell.includes('txn') || !this.parseDate(firstCell)) {
                dataRows = rawRows.slice(1);
            }
        }

        // Build existing fingerprints
        const existingFingerprints = enableDuplicateDetection
            ? await window.DuplicateDetector.buildExistingFingerprints(existingExpenses, existingIncomes)
            : new Set();

        const inBatchFingerprints = new Set();
        const candidates = [];

        for (let i = 0; i < dataRows.length; i++) {
            const row = dataRows[i];
            const rawDateStr = mapping.dateIdx >= 0 ? row[mapping.dateIdx] : '';
            const rawDesc = (mapping.descIdx >= 0 ? row[mapping.descIdx] : '').trim();

            let rawAmount = 0;
            let txType = 'Expense';

            if (mapping.debitIdx >= 0 && mapping.creditIdx >= 0) {
                const debitVal = this.parseAmount(row[mapping.debitIdx]);
                const creditVal = this.parseAmount(row[mapping.creditIdx]);
                if (creditVal > 0) {
                    rawAmount = creditVal;
                    txType = 'Income';
                } else if (debitVal > 0) {
                    rawAmount = debitVal;
                    txType = 'Expense';
                }
            } else if (mapping.amountIdx >= 0) {
                const amtRaw = String(row[mapping.amountIdx] || '');
                rawAmount = this.parseAmount(amtRaw);
                if (amtRaw.includes('-') || amtRaw.toLowerCase().includes('dr')) {
                    txType = 'Expense';
                } else if (amtRaw.includes('+') || amtRaw.toLowerCase().includes('cr')) {
                    txType = 'Income';
                } else if (mapping.typeIdx >= 0) {
                    const typeRaw = String(row[mapping.typeIdx] || '').toLowerCase();
                    if (typeRaw.includes('credit') || typeRaw.includes('income') || typeRaw === 'cr') {
                        txType = 'Income';
                    }
                }
            }

            const parsedDate = this.parseDate(rawDateStr) || new Date();
            const isDateValid = Boolean(rawDateStr && this.parseDate(rawDateStr));
            const isAmountValid = rawAmount > 0;

            // Payment Method
            const rawPaymentMethod = (mapping.paymentMethodIdx >= 0 ? row[mapping.paymentMethodIdx] : '').trim();
            const paymentMethod = rawPaymentMethod
                ? window.TransactionCategorizer.normalizePaymentMethod(rawPaymentMethod)
                : window.TransactionCategorizer.detectPaymentMethod(rawDesc);

            // Categorization
            const rawCategory = (mapping.categoryIdx >= 0 ? row[mapping.categoryIdx] : '').trim();
            let category = 'Other';
            let confidence = 0.5;
            let finalType = txType;

            if (rawCategory && rawCategory.toLowerCase() !== 'other') {
                category = rawCategory;
                confidence = 0.95;
            } else if (enableAutoCategorization) {
                const catResult = window.TransactionCategorizer.categorize(rawDesc, txType);
                category = catResult.category;
                confidence = catResult.confidence;
                finalType = catResult.type;
            }

            // Status Determination
            let status = 'NEW';
            let statusReason = '';

            if (!isDateValid || !isAmountValid || !rawDesc) {
                status = 'INVALID';
                statusReason = !isAmountValid ? 'Invalid or zero amount' : (!isDateValid ? 'Unrecognized date format' : 'Missing description');
            } else if (enableDuplicateDetection) {
                const fp = await window.DuplicateDetector.computeFingerprint(parsedDate, rawAmount, rawDesc, finalType, paymentMethod);
                if (existingFingerprints.has(fp)) {
                    status = 'DUPLICATE';
                    statusReason = 'Already exists in SpendWise';
                } else if (inBatchFingerprints.has(fp)) {
                    status = 'DUPLICATE';
                    statusReason = 'Duplicate within import file';
                } else {
                    inBatchFingerprints.add(fp);
                    if (confidence < 0.65 || category === 'Other') {
                        status = 'NEEDS_REVIEW';
                        statusReason = 'Low categorization confidence';
                    }
                }
            } else if (confidence < 0.65 || category === 'Other') {
                status = 'NEEDS_REVIEW';
                statusReason = 'Low categorization confidence';
            }

            const yyyy = parsedDate.getFullYear();
            const mm = String(parsedDate.getMonth() + 1).padStart(2, '0');
            const dd = String(parsedDate.getDate()).padStart(2, '0');

            candidates.push({
                id: 'cand_' + Math.random().toString(36).substring(2, 9),
                date: parsedDate,
                dateStr: `${yyyy}-${mm}-${dd}`,
                description: rawDesc || `Imported Record #${i + 1}`,
                amount: rawAmount,
                type: finalType,
                category: category,
                paymentMethod: paymentMethod,
                status: status,
                confidence: confidence,
                source: sourceType,
                statusReason: statusReason,
                isSelected: status !== 'DUPLICATE' && status !== 'INVALID'
            });
        }

        return candidates;
    }
};

window.ImportAnalyzer = ImportAnalyzer;
