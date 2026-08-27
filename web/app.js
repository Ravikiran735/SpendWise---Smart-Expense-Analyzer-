/**
 * SpendWise — Cloud Firestore & Firebase Auth Interactive Web Platform
 * Fully synchronized with SpendWise Android architecture (Project spendwise-a207a)
 */

// 1. Firebase Configuration for spendwise-a207a
const firebaseConfig = {
    apiKey: "AIzaSyAXVo5-jVRqeryG9ACq4X26wNXopywnFCM",
    authDomain: "spendwise-a207a.firebaseapp.com",
    projectId: "spendwise-a207a",
    storageBucket: "spendwise-a207a.firebasestorage.app",
    messagingSenderId: "842068564853",
    appId: "1:842068564853:android:cddb20f37c0410db6f113c"
};

// Initialize Firebase
if (!firebase.apps.length) {
    firebase.initializeApp(firebaseConfig);
}
const auth = firebase.auth();
const db = firebase.firestore();

// Categories matching Android enums
const CATEGORIES = {
    Expense: ['Food', 'Transport', 'Shopping', 'Rent', 'Utilities', 'Education', 'Healthcare', 'Entertainment', 'Subscriptions', 'Travel', 'Investment', 'Other'],
    Income: ['Salary', 'Freelance', 'Business', 'Investment', 'Gift', 'Other']
};

// Application State (Synced with Cloud Firestore)
let appState = {
    currentUser: null,
    expenses: [],
    incomes: [],
    budgets: [],
    goals: [],
    importHistory: [],
    userSettings: {
        theme: localStorage.getItem('spendwise_theme') || 'dark',
        currency: 'INR',
        accentColor: 'indigo',
        financialMode: 'SAVE',
        primaryGoal: 'Save Money',
        dismissedAlertIds: [],
        notificationsEnabled: true,
        budgetAlerts: true,
        transactionAlerts: true,
        savingsReminders: true,
        financialInsights: true,
        autoCategorization: true,
        duplicateDetection: true,
        importNotifications: true
    },
    theme: localStorage.getItem('spendwise_theme') || 'dark',
    unsubscribers: [],
    chartTrend: null,
    chartCategory: null,
    activeAlertFilter: 'all'
};

// Dynamic Currency Formatter matching Android CurrencyFormatter
function getCurrencySymbol(currencyOverride = null) {
    const curr = (currencyOverride || appState.userSettings?.currency || 'INR').toUpperCase();
    if (curr === 'USD') return '$';
    if (curr === 'EUR') return '€';
    if (curr === 'GBP') return '£';
    return '₹';
}

function formatCurrency(num, currencyOverride = null) {
    const curr = (currencyOverride || appState.userSettings?.currency || 'INR').toUpperCase();
    let symbol = getCurrencySymbol(curr);
    let locale = 'en-IN';

    if (curr === 'USD') {
        locale = 'en-US';
    } else if (curr === 'EUR') {
        locale = 'de-DE';
    } else if (curr === 'GBP') {
        locale = 'en-GB';
    }

    return symbol + Number(num || 0).toLocaleString(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function updateCurrencyLabels() {
    const sym = getCurrencySymbol();
    const txAmountLabel = document.querySelector('label[for="tx-amount"]');
    if (txAmountLabel) txAmountLabel.textContent = `Amount (${sym}) *`;

    const budgetAmountLabel = document.querySelector('label[for="budget-amount"]');
    if (budgetAmountLabel) budgetAmountLabel.textContent = `Monthly Cap (${sym})`;

    const goalTargetLabel = document.querySelector('label[for="goal-target"]');
    if (goalTargetLabel) goalTargetLabel.textContent = `Target Amount (${sym})`;

    const goalCurrentLabel = document.querySelector('label[for="goal-current"]');
    if (goalCurrentLabel) goalCurrentLabel.textContent = `Current Savings (${sym})`;

    const affordLabel = document.querySelector('#ai-view-affordability label');
    if (affordLabel) affordLabel.textContent = `Purchase Amount (${sym})`;
}

/* ==========================================================================
   Initialization & Lifecycle
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initNavigation();
    initAuthModals();
    initForms();
    initSmartImport();
    initVoiceRecognition();
    initReceiptScanner();
    setupAuthStateListener();
});

// Theme & Settings Management
function initTheme() {
    const savedTheme = localStorage.getItem('spendwise_theme') || 'dark';
    applyTheme(savedTheme);

    const sidebarToggle = document.getElementById('theme-toggle');
    sidebarToggle?.addEventListener('change', (e) => {
        const newTheme = e.target.checked ? 'dark' : 'light';
        applyTheme(newTheme);
        updateUserPreferences({ theme: newTheme });
    });
}

function applyTheme(theme) {
    appState.theme = theme;
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('spendwise_theme', theme);

    const sidebarToggle = document.getElementById('theme-toggle');
    const settingsToggle = document.getElementById('settings-theme-toggle');
    if (sidebarToggle) sidebarToggle.checked = theme === 'dark';
    if (settingsToggle) settingsToggle.checked = theme === 'dark';

    if (appState.chartTrend || appState.chartCategory) {
        renderCharts();
    }
}

function handleSettingsThemeChange(isDark) {
    const theme = isDark ? 'dark' : 'light';
    applyTheme(theme);
    updateUserPreferences({ theme });
}
window.handleSettingsThemeChange = handleSettingsThemeChange;

function handleCurrencyChange(currency) {
    updateUserPreferences({ currency });
    document.querySelectorAll('.btn-currency-chip').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-currency') === currency);
    });
}
window.handleCurrencyChange = handleCurrencyChange;

function handleFinancialModeChange(mode) {
    updateUserPreferences({ financialMode: mode });
    document.querySelectorAll('.btn-mode-chip').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-mode') === mode);
    });
    const headerBadge = document.getElementById('header-mode-badge');
    if (headerBadge) headerBadge.textContent = `${mode} MODE`;
    showToast(`Financial Mode switched to ${mode}`);
}
window.handleFinancialModeChange = handleFinancialModeChange;

function handlePrimaryGoalChange(goal) {
    updateUserPreferences({ primaryGoal: goal });
    document.querySelectorAll('.btn-goal-chip').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-pgoal') === goal);
    });
    showToast(`Primary goal updated to "${goal}"`);
}
window.handlePrimaryGoalChange = handlePrimaryGoalChange;

function handleNotifPrefChange(key, enabled) {
    const updateObj = {};
    updateObj[key] = enabled;
    updateUserPreferences(updateObj);
}
window.handleNotifPrefChange = handleNotifPrefChange;

async function updateUserPreferences(prefs) {
    if (!appState.currentUser) return;
    try {
        const userId = appState.currentUser.uid;
        await db.collection('users').doc(userId)
            .collection('settings').doc('preferences')
            .set(prefs, { merge: true });
    } catch (err) {
        console.error('Error saving settings:', err);
    }
}

// Navigation Tabs Router
function initNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', () => {
            const tabId = link.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.toggle('active', link.getAttribute('data-tab') === tabId);
    });

    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.toggle('active', pane.id === `tab-${tabId}`);
    });

    const pageTitle = document.getElementById('page-title');
    const pageSubtitle = document.getElementById('page-subtitle');
    const greeting = getHeaderGreetingText();
    const firstName = (appState.currentUser?.displayName || 'User').split(' ')[0];

    const titles = {
        'dashboard': { title: `${greeting}, ${firstName}`, sub: "Here's what matters about your money today." },
        'transactions': { title: 'Transactions Ledger', sub: 'Comprehensive records across all accounts' },
        'smart-import': { title: 'Smart Import Hub', sub: 'Import, organize, and categorize statements' },
        'budgets': { title: 'Budgets & Limits', sub: 'Track and enforce category spending caps' },
        'goals': { title: 'Savings Goals', sub: 'Define targets and monitor milestone progress' },
        'insights': { title: 'Smart Insights & Alerts', sub: 'Real-time proactive money alerts based on actual data' },
        'spendwise-ai': { title: 'SpendWise Financial Copilot', sub: 'Understand your money. Plan your next move.' },
        'reports': { title: 'Reports & Audits', sub: 'Executive financial statements and exports' },
        'profile': { title: 'User Profile & Security', sub: 'Manage personal credentials, security, and cloud sync' },
        'settings': { title: 'Settings & Modes', sub: 'Manage personal financial modes and preferences' }
    };

    if (titles[tabId] && pageTitle && pageSubtitle) {
        pageTitle.textContent = titles[tabId].title;
        pageSubtitle.textContent = titles[tabId].sub;
    }

    if (tabId === 'spendwise-ai') {
        renderSpendWiseAi();
    } else if (tabId === 'dashboard') {
        renderSummaryMetrics();
        renderCharts();
    } else if (tabId === 'insights') {
        renderMoneyAlertsView();
    } else if (tabId === 'profile') {
        renderProfileView();
    } else if (tabId === 'reports') {
        renderReportsView();
    }
}
window.switchTab = switchTab;

function getHeaderGreetingText() {
    const hour = new Date().getHours();
    if (hour >= 4 && hour < 12) return 'Good morning';
    if (hour >= 12 && hour < 17) return 'Good afternoon';
    return 'Good evening';
}

/* ==========================================================================
   Authentication & Firestore Listeners
   ========================================================================== */
function setupAuthStateListener() {
    auth.onAuthStateChanged(user => {
        if (user) {
            appState.currentUser = {
                uid: user.uid,
                email: user.email,
                name: user.displayName || user.email?.split('@')[0] || 'User'
            };
            updateUserUI(user);
            closeModal('auth-modal');
            setupFirestoreListeners(user.uid);
            showToast(`Welcome back, ${appState.currentUser.name}!`);
        } else {
            appState.currentUser = null;
            cleanupFirestoreListeners();
            resetState();
            updateUserUI(null);
            openModal('auth-modal');
        }
    });
}

function updateUserUI(user) {
    const nameEl = document.getElementById('user-display-name');
    const emailEl = document.getElementById('user-display-email');
    const syncEl = document.getElementById('sync-status');

    if (user) {
        nameEl.textContent = user.displayName || user.email?.split('@')[0] || 'SpendWise User';
        emailEl.textContent = user.email || 'user@spendwise.app';
        syncEl.innerHTML = `<i class="fa-solid fa-cloud-check"></i> Cloud Synced`;
        syncEl.style.color = 'var(--accent-emerald)';
    } else {
        nameEl.textContent = 'Not Signed In';
        emailEl.textContent = 'Click to sign in';
        syncEl.innerHTML = `<i class="fa-solid fa-cloud-slash"></i> Offline`;
        syncEl.style.color = 'var(--text-muted)';
    }
}

function setupFirestoreListeners(userId) {
    cleanupFirestoreListeners();

    // 1. Expenses: users/{userId}/expenses
    const unsubExpenses = db.collection('users').doc(userId)
        .collection('expenses')
        .orderBy('date', 'desc')
        .onSnapshot(snapshot => {
            appState.expenses = snapshot.docs.map(doc => {
                const data = doc.data();
                return {
                    id: doc.id,
                    type: 'Expense',
                    userId: data.userId || userId,
                    amount: Number(data.amount) || 0,
                    category: data.category || 'Other',
                    description: data.description || '',
                    paymentMethod: data.paymentMethod || 'UPI',
                    source: data.source || 'MANUAL',
                    importId: data.importId || '',
                    isEssential: data.isEssential,
                    date: parseFirestoreDate(data.date)
                };
            });
            renderAll();
        }, error => {
            console.error('Expenses listener error:', error);
        });

    // 2. Incomes: users/{userId}/incomes
    const unsubIncomes = db.collection('users').doc(userId)
        .collection('incomes')
        .orderBy('date', 'desc')
        .onSnapshot(snapshot => {
            appState.incomes = snapshot.docs.map(doc => {
                const data = doc.data();
                return {
                    id: doc.id,
                    type: 'Income',
                    userId: data.userId || userId,
                    amount: Number(data.amount) || 0,
                    source: data.source || 'Salary',
                    category: data.source || 'Salary',
                    description: data.description || '',
                    origin: data.origin || 'MANUAL',
                    importId: data.importId || '',
                    paymentMethod: data.paymentMethod || 'Bank Transfer',
                    date: parseFirestoreDate(data.date)
                };
            });
            renderAll();
        }, error => {
            console.error('Incomes listener error:', error);
        });

    // 3. Budgets: users/{userId}/budgets
    const unsubBudgets = db.collection('users').doc(userId)
        .collection('budgets')
        .onSnapshot(snapshot => {
            appState.budgets = snapshot.docs.map(doc => {
                const data = doc.data();
                return {
                    id: doc.id,
                    userId: data.userId || userId,
                    category: data.category || 'Other',
                    amount: Number(data.amount) || 0,
                    spentAmount: Number(data.spentAmount) || 0
                };
            });
            renderBudgets();
            renderSummaryMetrics();
        }, error => {
            console.error('Budgets listener error:', error);
        });

    // 4. Savings Goals: users/{userId}/savingsGoals
    const unsubGoals = db.collection('users').doc(userId)
        .collection('savingsGoals')
        .onSnapshot(snapshot => {
            appState.goals = snapshot.docs.map(doc => {
                const data = doc.data();
                return {
                    id: doc.id,
                    userId: data.userId || userId,
                    title: data.title || 'Goal',
                    targetAmount: Number(data.targetAmount) || 0,
                    currentAmount: Number(data.currentAmount) || 0,
                    deadline: data.deadline ? parseFirestoreDate(data.deadline) : ''
                };
            });
            renderGoals();
            renderSummaryMetrics();
        }, error => {
            console.error('Goals listener error:', error);
        });

    // 5. User Preferences: users/{userId}/settings/preferences
    const unsubSettings = db.collection('users').doc(userId)
        .collection('settings').doc('preferences')
        .onSnapshot(docSnap => {
            if (docSnap && docSnap.exists) {
                const data = docSnap.data();
                appState.userSettings = {
                    theme: data.theme || 'dark',
                    currency: data.currency || 'INR',
                    accentColor: data.accentColor || 'indigo',
                    financialMode: data.financialMode || 'SAVE',
                    primaryGoal: data.primaryGoal || 'Save Money',
                    dismissedAlertIds: data.dismissedAlertIds || [],
                    notificationsEnabled: data.notificationsEnabled !== false,
                    budgetAlerts: data.budgetAlerts !== false,
                    transactionAlerts: data.transactionAlerts !== false,
                    savingsReminders: data.savingsReminders !== false,
                    financialInsights: data.financialInsights !== false,
                    autoCategorization: data.autoCategorization !== false,
                    duplicateDetection: data.duplicateDetection !== false,
                    importNotifications: data.importNotifications !== false
                };

                applyTheme(appState.userSettings.theme);
                renderSettingsUI();
                renderAll();
            }
        }, error => {
            console.error('Settings listener error:', error);
        });

    window.setupImportHistoryListener(userId);
    appState.unsubscribers = [unsubExpenses, unsubIncomes, unsubBudgets, unsubGoals, unsubSettings];
}

function cleanupFirestoreListeners() {
    appState.unsubscribers.forEach(unsub => unsub());
    appState.unsubscribers = [];
}

function resetState() {
    appState.expenses = [];
    appState.incomes = [];
    appState.budgets = [];
    appState.goals = [];
    appState.importHistory = [];
    renderAll();
}

function parseFirestoreDate(val) {
    if (!val) return new Date().toISOString().split('T')[0];
    if (val.toDate) return val.toDate().toISOString().split('T')[0];
    if (val instanceof Date) return val.toISOString().split('T')[0];
    if (typeof val === 'string') return val.split('T')[0];
    return new Date().toISOString().split('T')[0];
}

/* ==========================================================================
   Core Pure Decision Intelligence Algorithms (Matching Kotlin AnalysisEngine.kt)
   ========================================================================== */

function getAllTransactions() {
    return [...appState.expenses, ...appState.incomes]
        .sort((a, b) => new Date(b.date) - new Date(a.date));
}

function calculateMetrics() {
    const totalIncome = appState.incomes.reduce((sum, t) => sum + Number(t.amount), 0);
    const totalExpense = appState.expenses.reduce((sum, t) => sum + Number(t.amount), 0);
    const savings = totalIncome - totalExpense;
    const savingsRate = totalIncome > 0 ? (savings / totalIncome) * 100 : 0;

    const categoryTotals = {};
    appState.expenses.forEach(t => {
        categoryTotals[t.category] = (categoryTotals[t.category] || 0) + Number(t.amount);
    });

    return { totalIncome, totalExpense, savings, savingsRate, categoryTotals };
}

function calculateFinancialHealthScore() {
    const { totalIncome, totalExpense, savingsRate } = calculateMetrics();

    let savingsScore = 5;
    if (savingsRate >= 30) savingsScore = 35;
    else if (savingsRate >= 20) savingsScore = 30;
    else if (savingsRate >= 10) savingsScore = 22;
    else if (savingsRate > 0) savingsScore = 14;

    let exceededCount = 0;
    let warningCount = 0;
    appState.budgets.forEach(b => {
        if (b.amount > 0) {
            const spent = appState.expenses.filter(e => e.category === b.category).reduce((s, e) => s + Number(e.amount), 0);
            const pct = spent / b.amount;
            if (pct >= 1.0) exceededCount++;
            else if (pct >= 0.8) warningCount++;
        }
    });

    let budgetScore = 20;
    if (appState.budgets.length > 0) {
        if (exceededCount === 0 && warningCount === 0) budgetScore = 25;
        else if (exceededCount === 0) budgetScore = 18;
        else if (exceededCount === 1) budgetScore = 12;
        else budgetScore = 6;
    }

    const ratio = totalIncome > 0 ? totalExpense / totalIncome : 1.0;
    let stabilityScore = 6;
    if (ratio <= 0.50) stabilityScore = 20;
    else if (ratio <= 0.70) stabilityScore = 16;
    else if (ratio <= 0.85) stabilityScore = 12;

    let goalsScore = 15;
    if (appState.goals.length > 0) {
        const avg = appState.goals.reduce((sum, g) => sum + (g.targetAmount > 0 ? Math.min(1, g.currentAmount / g.targetAmount) : 0), 0) / appState.goals.length;
        goalsScore = Math.max(5, Math.min(20, Math.round(avg * 20)));
    }

    const totalScore = Math.max(10, Math.min(100, savingsScore + budgetScore + stabilityScore + goalsScore));
    let label = 'Needs Attention';
    let grade = 'Needs Attention';
    let color = 'var(--accent-rose)';

    if (totalScore >= 80) { label = 'Excellent Standing'; grade = 'Excellent'; color = 'var(--accent-emerald)'; }
    else if (totalScore >= 70) { label = 'Healthy Standing'; grade = 'Healthy'; color = 'var(--accent-emerald)'; }
    else if (totalScore >= 50) { label = 'Moderate Standing'; grade = 'Moderate'; color = 'var(--accent-amber)'; }

    return { totalScore, label, grade, color, savingsScore, budgetScore, stabilityScore, goalsScore };
}

function calculateMoneyHabitScore() {
    const expenses = appState.expenses;
    const incomes = appState.incomes;
    const bullets = [];

    // 1. Recurring Savings Consistency (Max 20)
    let recurringSavingsPts = 12;
    if (incomes.length > 0 && expenses.length > 0) {
        const rate = calculateMetrics().savingsRate;
        if (rate >= 20) {
            recurringSavingsPts = 20;
            bullets.push({ title: "Consistent Savings Ratio", description: `You consistently retain ${rate.toFixed(0)}% of your monthly cashflow.`, isPositive: true });
        } else if (rate > 5) {
            recurringSavingsPts = 14;
            bullets.push({ title: "Moderate Savings Reserve", description: "You maintain positive monthly savings across cycles.", isPositive: true });
        } else {
            recurringSavingsPts = 6;
            bullets.push({ title: "Low Monthly Surplus", description: "Expenses consume nearly all incoming cashflow.", isPositive: false });
        }
    }

    // 2. Budget Discipline (Max 20)
    let budgetDisciplinePts = 15;
    const exceeded = appState.budgets.filter(b => {
        const spent = expenses.filter(e => e.category === b.category).reduce((s, e) => s + Number(e.amount), 0);
        return b.amount > 0 && spent > b.amount;
    }).length;
    if (appState.budgets.length > 0) {
        if (exceeded === 0) {
            budgetDisciplinePts = 20;
            bullets.push({ title: "Zero Budget Overruns", description: "All category caps are strictly respected.", isPositive: true });
        } else {
            budgetDisciplinePts = Math.max(5, 20 - (exceeded * 7));
            bullets.push({ title: `${exceeded} Category Overrun(s)`, description: "Some category limits were breached this cycle.", isPositive: false });
        }
    }

    // 3. Low Micro-Expense Leaks (Max 15)
    let leakPts = 12;
    const leaks = detectSpendingLeaks();
    if (leaks.length === 0) {
        leakPts = 15;
        bullets.push({ title: "Controlled Micro-Spending", description: "No frequent recurring spending leaks detected.", isPositive: true });
    } else {
        leakPts = Math.max(4, 15 - (leaks.length * 3));
        bullets.push({ title: "Repeated Micro-Purchases", description: `${leaks.length} recurring micro-spending patterns identified.`, isPositive: false });
    }

    // 4. Essential vs Discretionary Balance (Max 15)
    let essentialPts = 12;
    const ess = calculateEssentialVsDiscretionary();
    if (ess.essentialPct <= 60 && ess.discretionaryPct <= 30) {
        essentialPts = 15;
        bullets.push({ title: "Optimal 50/30/20 Alignment", description: "Wants and Needs are well proportioned.", isPositive: true });
    } else if (ess.discretionaryPct > 45) {
        essentialPts = 7;
        bullets.push({ title: "High Discretionary Outflow", description: `Wants represent ${ess.discretionaryPct.toFixed(0)}% of total spending.`, isPositive: false });
    }

    // 5. Goal Contribution Momentum (Max 15)
    let goalPts = 10;
    if (appState.goals.length > 0) {
        goalPts = 15;
        bullets.push({ title: "Active Goal Planning", description: "You are actively funding designated savings targets.", isPositive: true });
    }

    // 6. Predictable Cadence (Max 15)
    let cadencePts = 12;
    if (expenses.length >= 5) {
        cadencePts = 15;
        bullets.push({ title: "Regular Record Keeping", description: "Transaction logs are updated in real time.", isPositive: true });
    }

    const total = Math.min(100, recurringSavingsPts + budgetDisciplinePts + leakPts + essentialPts + goalPts + cadencePts);
    let label = 'EXCELLENT MONEY HABITS';
    if (total < 60) label = 'NEEDS DISCIPLINE';
    else if (total < 75) label = 'MODERATE MONEY HABITS';
    else if (total < 85) label = 'GOOD MONEY HABITS';

    return {
        score: total,
        label,
        bulletPoints: bullets
    };
}

function isCategoryEssential(category) {
    const cat = (category || '').toLowerCase().trim();
    if (cat.includes('rent') || cat.includes('housing') || cat.includes('mortgage')) return true;
    if (cat.includes('utilit') || cat.includes('electricity') || cat.includes('water') || cat.includes('gas') || cat.includes('bill')) return true;
    if (cat.includes('grocer') || (cat.includes('food') && !cat.includes('delivery') && !cat.includes('dining') && !cat.includes('restaurant') && !cat.includes('swiggy') && !cat.includes('zomato'))) return true;
    if (cat.includes('educat') || cat.includes('tuition') || cat.includes('school') || cat.includes('course')) return true;
    if (cat.includes('health') || cat.includes('medic') || cat.includes('doctor') || cat.includes('pharmacy')) return true;
    if (cat.includes('insur') || cat.includes('emi') || cat.includes('loan') || cat.includes('investment')) return true;
    return false;
}

function calculateEssentialVsDiscretionary() {
    let essentialTotal = 0;
    let discretionaryTotal = 0;
    const catTotals = calculateMetrics().categoryTotals;

    Object.entries(catTotals).forEach(([cat, amt]) => {
        if (isCategoryEssential(cat)) {
            essentialTotal += amt;
        } else {
            discretionaryTotal += amt;
        }
    });

    const total = essentialTotal + discretionaryTotal;
    const essentialPct = total > 0 ? (essentialTotal / total) * 100 : 50;
    const discretionaryPct = total > 0 ? (discretionaryTotal / total) * 100 : 50;

    let recommendation = "Your spending distribution is well-aligned with sustainable cashflow.";
    if (discretionaryPct > 40) {
        recommendation = `Discretionary purchases account for ${discretionaryPct.toFixed(0)}% of your expenses. Shifting 10% into savings accelerates your targets.`;
    }

    return {
        essentialTotal,
        discretionaryTotal,
        essentialPct,
        discretionaryPct,
        recommendation
    };
}

function detectRecurringMoneyMap() {
    const recurringCategories = ['Subscriptions', 'Rent', 'Utilities'];
    const items = [];
    let monthlyTotal = 0;

    const descGroups = {};
    appState.expenses.forEach(e => {
        const key = (e.description || e.category).toLowerCase().trim();
        if (!descGroups[key]) descGroups[key] = [];
        descGroups[key].push(e);
    });

    Object.entries(descGroups).forEach(([key, txs]) => {
        const first = txs[0];
        const isRecCat = recurringCategories.includes(first.category);
        if (isRecCat || txs.length >= 2) {
            const avgAmt = txs.reduce((s, t) => s + t.amount, 0) / txs.length;
            const monthly = avgAmt;
            monthlyTotal += monthly;
            items.push({
                name: first.description || first.category,
                category: first.category,
                monthlyAmount: monthly,
                annualAmount: monthly * 12,
                frequency: isRecCat ? 'Monthly' : 'Recurring'
            });
        }
    });

    return {
        items,
        monthlyRecurringTotal: monthlyTotal,
        annualProjectedTotal: monthlyTotal * 12
    };
}

function detectSpendingLeaks() {
    const leaks = [];
    const descCounts = {};

    appState.expenses.forEach(e => {
        if (e.amount < 800) {
            const key = (e.description || e.category).trim();
            if (!descCounts[key]) descCounts[key] = { count: 0, sum: 0, cat: e.category };
            descCounts[key].count++;
            descCounts[key].sum += e.amount;
        }
    });

    Object.entries(descCounts).forEach(([name, data]) => {
        if (data.count >= 2) {
            leaks.push({
                name,
                category: data.cat,
                frequencyCount: data.count,
                monthlyTotal: data.sum,
                aiExplanation: `${data.count} repeated purchases totaling ${formatCurrency(data.sum)}/mo (~${formatCurrency(data.sum * 12)}/year).`
            });
        }
    });

    return leaks.sort((a, b) => b.monthlyTotal - a.monthlyTotal).slice(0, 3);
}

function generateGoalRoadmap() {
    const monthlySurplus = Math.max(0, calculateMetrics().savings);
    const activeGoals = appState.goals;

    return activeGoals.map(g => {
        const remaining = Math.max(0, g.targetAmount - g.currentAmount);
        const progressPct = g.targetAmount > 0 ? Math.min(100, Math.round((g.currentAmount / g.targetAmount) * 100)) : 0;
        const monthlyContrib = monthlySurplus > 0 ? Math.round(monthlySurplus / Math.max(1, activeGoals.length)) : 1000;
        const monthsRemaining = monthlyContrib > 0 ? Math.ceil(remaining / monthlyContrib) : 6;

        return {
            title: g.title,
            targetAmount: g.targetAmount,
            currentAmount: g.currentAmount,
            remainingAmount: remaining,
            monthlyContribution: monthlyContrib,
            progressPct,
            projectedCompletionDate: `In ~${monthsRemaining} months`,
            aiSuggestion: `Contributing ${formatCurrency(monthlyContrib)}/mo completes this in ~${monthsRemaining} months.`
        };
    });
}

function calculateMultiGoalPriority() {
    const monthlySurplus = Math.max(0, calculateMetrics().savings);
    const goals = appState.goals;
    if (goals.length === 0) return { allocations: [] };

    const allocations = goals.map((g, idx) => {
        const weight = idx === 0 ? 0.6 : (0.4 / Math.max(1, goals.length - 1));
        const amount = Math.round(monthlySurplus * weight);
        return {
            goalTitle: g.title,
            allocationPercentage: weight * 100,
            recommendedMonthlyAmount: amount,
            reasonWhy: idx === 0 ? "Highest priority active goal." : "Secondary long-term milestone."
        };
    });

    return { allocations };
}

function generateMoneyAlerts() {
    const alerts = [];
    const { categoryTotals } = calculateMetrics();
    const dismissed = appState.userSettings.dismissedAlertIds || [];

    // 1. Budget Risks
    appState.budgets.forEach(b => {
        const spent = categoryTotals[b.category] || 0;
        if (b.amount > 0 && spent >= b.amount) {
            const id = `budget_${b.category}`;
            if (!dismissed.includes(id)) {
                alerts.push({
                    id,
                    type: 'BUDGET_RISK',
                    title: `Budget Exceeded: ${b.category}`,
                    message: `You spent ${formatCurrency(spent)} of your ${formatCurrency(b.amount)} cap.`,
                    severity: 'danger',
                    actionLabel: 'Adjust Budget',
                    actionRoute: 'budgets'
                });
            }
        }
    });

    // 2. Spending Leaks
    const leaks = detectSpendingLeaks();
    if (leaks.length > 0) {
        const id = 'leaks_detected';
        if (!dismissed.includes(id)) {
            alerts.push({
                id,
                type: 'SPENDING_OPPORTUNITY',
                title: 'Spending Leaks Detected',
                message: `Top ${leaks.length} repeated small purchases total ~${formatCurrency(leaks.reduce((s, l) => s + l.monthlyTotal, 0))}/mo.`,
                severity: 'warning',
                actionLabel: 'Review Leaks',
                actionRoute: 'spendwise-ai'
            });
        }
    }

    // 3. Goal Milestones
    appState.goals.forEach(g => {
        const pct = g.targetAmount > 0 ? (g.currentAmount / g.targetAmount) * 100 : 0;
        if (pct >= 50 && pct < 100) {
            const id = `goal_50_${g.title}`;
            if (!dismissed.includes(id)) {
                alerts.push({
                    id,
                    type: 'GOAL_MILESTONE',
                    title: `Goal Halfway Milestone: ${g.title}`,
                    message: `You've achieved ${pct.toFixed(0)}% of your target!`,
                    severity: 'success',
                    actionLabel: 'View Goal',
                    actionRoute: 'goals'
                });
            }
        }
    });

    return alerts;
}

function dismissMoneyAlert(id) {
    const current = appState.userSettings.dismissedAlertIds || [];
    if (!current.includes(id)) {
        current.push(id);
        updateUserPreferences({ dismissedAlertIds: current });
        renderMoneyAlertsView();
        showToast('Alert dismissed');
    }
}
window.dismissMoneyAlert = dismissMoneyAlert;

function filterMoneyAlerts(type) {
    appState.activeAlertFilter = type;
    document.querySelectorAll('.alert-filter-chips .filter-pill').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-alert-filter') === type);
    });
    renderMoneyAlertsView();
}
window.filterMoneyAlerts = filterMoneyAlerts;

function renderMoneyAlertsView() {
    const container = document.getElementById('insights-full-container');
    if (!container) return;

    const allAlerts = generateMoneyAlerts();
    const countBadge = document.getElementById('insights-count');
    if (countBadge) countBadge.textContent = allAlerts.length;

    const filtered = allAlerts.filter(a => appState.activeAlertFilter === 'all' || a.type === appState.activeAlertFilter);

    if (filtered.length === 0) {
        container.innerHTML = `
            <div class="glass-card p-4 text-center">
                <i class="fa-solid fa-circle-check text-emerald" style="font-size: 2.2rem; margin-bottom: 8px;"></i>
                <h4>No Active Money Alerts</h4>
                <p class="text-muted" style="font-size: 0.85rem;">All monitored budgets, recurring subscriptions, and goals are in healthy standing.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = filtered.map(alert => {
        const iconClass = alert.type === 'BUDGET_RISK' ? 'fa-triangle-exclamation' : (alert.type === 'GOAL_MILESTONE' ? 'fa-bullseye' : 'fa-lightbulb');
        return `
            <div class="glass-card p-3 mb-3 d-flex justify-content-between align-items-center">
                <div class="d-flex align-items-center gap-3">
                    <i class="fa-solid ${iconClass} text-${alert.severity}" style="font-size: 1.3rem;"></i>
                    <div>
                        <strong>${escapeHtml(alert.title)}</strong>
                        <p class="text-muted" style="font-size: 0.85rem; margin-top: 2px;">${escapeHtml(alert.message)}</p>
                    </div>
                </div>
                <div class="d-flex gap-2">
                    <button class="btn btn-sm btn-secondary" onclick="dismissMoneyAlert('${alert.id}')">Dismiss</button>
                    <button class="btn btn-sm btn-primary" onclick="switchTab('${alert.actionRoute}')">${alert.actionLabel}</button>
                </div>
            </div>
        `;
    }).join('');
}

/* ==========================================================================
   UI Dashboard Synchronizer
   ========================================================================== */
function renderAll() {
    updateCurrencyLabels();
    renderHeaderGreeting();
    renderSummaryMetrics();
    renderRecentTransactions();
    renderTransactionsTable();
    renderBudgets();
    renderGoals();
    renderMoneyAlertsView();
    renderCharts();
    renderSpendWiseAi();
}

function renderHeaderGreeting() {
    const greeting = getHeaderGreetingText();
    const userName = appState.currentUser?.displayName || appState.currentUser?.email?.split('@')[0] || 'User';
    const firstName = userName.split(' ')[0];

    const titleEl = document.getElementById('page-title');
    const subEl = document.getElementById('page-subtitle');
    const badgeEl = document.getElementById('header-mode-badge');

    if (badgeEl) badgeEl.textContent = `${appState.userSettings.financialMode || 'SAVE'} MODE`;

    const activeTab = document.querySelector('.nav-link.active')?.getAttribute('data-tab') || 'dashboard';
    if (activeTab === 'dashboard' && titleEl && subEl) {
        titleEl.textContent = `${greeting}, ${firstName}`;
        subEl.textContent = "Here's what matters about your money today.";
    }
}

function renderSummaryMetrics() {
    const { totalIncome, totalExpense, savings, savingsRate } = calculateMetrics();
    const health = calculateFinancialHealthScore();
    const habit = calculateMoneyHabitScore();
    const ess = calculateEssentialVsDiscretionary();
    const recMap = detectRecurringMoneyMap();
    const roadmap = generateGoalRoadmap();
    const leaks = detectSpendingLeaks();

    const balEl = document.getElementById('hero-total-balance');
    if (balEl) balEl.textContent = formatCurrency(savings);

    const incEl = document.getElementById('hero-income-val');
    if (incEl) incEl.textContent = `+${formatCurrency(totalIncome)}`;

    const expEl = document.getElementById('hero-expense-val');
    if (expEl) expEl.textContent = `-${formatCurrency(totalExpense)}`;

    const savEl = document.getElementById('hero-savings-val');
    if (savEl) savEl.textContent = formatCurrency(savings);

    const rateEl = document.getElementById('hero-savings-rate-val');
    if (rateEl) rateEl.textContent = `${savingsRate.toFixed(0)}%`;

    const healthEl = document.getElementById('dash-health-score-val');
    if (healthEl) healthEl.textContent = `${health.totalScore}/100`;

    const habitEl = document.getElementById('dash-habit-score-val');
    if (habitEl) habitEl.textContent = `${habit.score}/100`;

    // Essential vs Discretionary
    const essVal = document.getElementById('dash-essential-val');
    if (essVal) essVal.textContent = formatCurrency(ess.essentialTotal);
    const essPct = document.getElementById('dash-essential-pct');
    if (essPct) essPct.textContent = `${ess.essentialPct.toFixed(0)}% of total`;

    const discVal = document.getElementById('dash-discretionary-val');
    if (discVal) discVal.textContent = formatCurrency(ess.discretionaryTotal);
    const discPct = document.getElementById('dash-discretionary-pct');
    if (discPct) discPct.textContent = `${ess.discretionaryPct.toFixed(0)}% of total`;

    // Upcoming list
    const upEl = document.getElementById('dash-upcoming-list');
    if (upEl) {
        if (recMap.items.length === 0) {
            upEl.innerHTML = `<p class="text-muted" style="font-size:0.85rem;">No recurring bills recorded yet.</p>`;
        } else {
            upEl.innerHTML = recMap.items.slice(0, 3).map(item => `
                <div class="d-flex justify-content-between mb-1" style="font-size:0.85rem;">
                    <span><i class="fa-solid fa-repeat text-primary mr-1"></i> ${escapeHtml(item.name)}</span>
                    <strong class="text-danger">-${formatCurrency(item.monthlyAmount)}/mo</strong>
                </div>
            `).join('');
        }
    }

    // Goal roadmap top item
    if (roadmap.length > 0) {
        const topGoal = roadmap[0];
        const gTitle = document.getElementById('dash-goal-title');
        const gPct = document.getElementById('dash-goal-pct');
        const gBar = document.getElementById('dash-goal-bar');
        const gSaved = document.getElementById('dash-goal-saved');
        const gTarget = document.getElementById('dash-goal-target');

        if (gTitle) gTitle.textContent = topGoal.title;
        if (gPct) gPct.textContent = `${topGoal.progressPct}%`;
        if (gBar) gBar.style.width = `${topGoal.progressPct}%`;
        if (gSaved) gSaved.textContent = `${formatCurrency(topGoal.currentAmount)} saved`;
        if (gTarget) gTarget.textContent = `Target: ${topGoal.projectedCompletionDate}`;
    }

    // Leaks mini list
    const leaksEl = document.getElementById('dash-leaks-mini-list');
    if (leaksEl) {
        if (leaks.length === 0) {
            leaksEl.innerHTML = `<p class="text-muted" style="font-size:0.85rem;">No repeated micro-leaks detected.</p>`;
        } else {
            leaksEl.innerHTML = leaks.slice(0, 2).map(l => `
                <div class="d-flex justify-content-between mb-1" style="font-size:0.85rem;">
                    <span>${escapeHtml(l.name)} (${l.frequencyCount}x)</span>
                    <strong class="text-danger">~${formatCurrency(l.monthlyTotal)}/mo</strong>
                </div>
            `).join('');
        }
    }
}

/* ==========================================================================
   SpendWise Financial Copilot Sub-Views & Calculations
   ========================================================================== */
let currentAiMode = 'chat';
let aiConversationsLoaded = false;
const AI_API_BASE = 'http://localhost:5000/api/ai';

function openAiMode(mode) {
    currentAiMode = mode;
    document.querySelectorAll('.ai-mode-pill').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-mode') === mode);
    });

    document.querySelectorAll('.ai-subview').forEach(view => {
        view.classList.remove('active');
    });

    const activeView = document.getElementById(`ai-view-${mode}`);
    if (activeView) activeView.classList.add('active');

    renderActiveAiView(mode);
}
window.openAiMode = openAiMode;

function renderActiveAiView(mode) {
    if (mode === 'simulator') handleSimulatorSliderChange();
    else if (mode === 'affordability') evaluatePurchaseAffordability();
    else if (mode === 'twin') renderDigitalTwinView();
    else if (mode === 'habit') renderHabitScoreView();
    else if (mode === 'essential') renderEssentialView();
    else if (mode === 'recurring') renderRecurringMapView();
    else if (mode === 'leaks') renderLeaksView();
    else if (mode === 'roadmap') renderRoadmapView();
    else if (mode === 'reviews') renderReviewsView();
    else if (mode === 'forecast') renderForecastView();
    else if (mode === 'calendar') renderCalendarView();
}

function renderSpendWiseAi() {
    renderActiveAiView(currentAiMode);
    if (!aiConversationsLoaded && appState.currentUser) {
        loadAiConversations();
    }
}
window.renderSpendWiseAi = renderSpendWiseAi;

// 1. What-If Simulator Handler
function handleSimulatorSliderChange() {
    const foodRed = parseFloat(document.getElementById('slider-sim-food')?.value || 2000);
    const shopRed = parseFloat(document.getElementById('slider-sim-shopping')?.value || 1000);
    const incAdd = parseFloat(document.getElementById('slider-sim-income')?.value || 0);

    const foodValEl = document.getElementById('sim-val-food');
    const shopValEl = document.getElementById('sim-val-shopping');
    const incValEl = document.getElementById('sim-val-income');

    if (foodValEl) foodValEl.textContent = `${formatCurrency(foodRed)}/mo`;
    if (shopValEl) shopValEl.textContent = `${formatCurrency(shopRed)}/mo`;
    if (incValEl) incValEl.textContent = `+${formatCurrency(incAdd)}/mo`;

    const { totalIncome, totalExpense } = calculateMetrics();
    const currentMonthlySavings = Math.max(0, totalIncome - totalExpense);
    const currentRate = totalIncome > 0 ? (currentMonthlySavings / totalIncome) * 100 : 0;

    const projectedExpense = Math.max(0, totalExpense - foodRed - shopRed);
    const projectedIncome = totalIncome + incAdd;
    const projectedSavings = Math.max(0, projectedIncome - projectedExpense);
    const projectedRate = projectedIncome > 0 ? (projectedSavings / projectedIncome) * 100 : 0;

    const improvementPct = currentMonthlySavings > 0
        ? ((projectedSavings - currentMonthlySavings) / currentMonthlySavings) * 100
        : (projectedSavings > 0 ? 100 : 0);

    document.getElementById('sim-current-savings').textContent = formatCurrency(currentMonthlySavings);
    document.getElementById('sim-current-rate').textContent = `${currentRate.toFixed(0)}% savings rate`;
    document.getElementById('sim-projected-savings').textContent = formatCurrency(projectedSavings);
    document.getElementById('sim-projected-improvement').textContent = `+${improvementPct.toFixed(1)}% savings improvement`;

    // Goal shifts
    const shiftsContainer = document.getElementById('sim-goal-shifts-list');
    if (shiftsContainer) {
        if (appState.goals.length === 0) {
            shiftsContainer.innerHTML = `<p class="text-muted" style="font-size:0.85rem;">Set active goals to project timeline acceleration.</p>`;
        } else {
            shiftsContainer.innerHTML = appState.goals.map(g => {
                const remaining = Math.max(0, g.targetAmount - g.currentAmount);
                const curContrib = currentMonthlySavings > 0 ? currentMonthlySavings / appState.goals.length : 1000;
                const projContrib = projectedSavings > 0 ? projectedSavings / appState.goals.length : 1000;
                const curMonths = Math.ceil(remaining / Math.max(1, curContrib));
                const projMonths = Math.ceil(remaining / Math.max(1, projContrib));
                const daysSaved = Math.max(0, (curMonths - projMonths) * 30);

                return `
                    <div class="glass-card p-2 mb-2 d-flex justify-content-between align-items-center">
                        <div>
                            <strong>${escapeHtml(g.title)}</strong>
                            <div class="text-muted" style="font-size:0.8rem;">Timeline: ${curMonths} mo → ${projMonths} mo</div>
                        </div>
                        <span class="badge-pill badge-emerald">${daysSaved} days saved</span>
                    </div>
                `;
            }).join('');
        }
    }
}
window.handleSimulatorSliderChange = handleSimulatorSliderChange;

// 2. Can I Afford This? Evaluator
function evaluatePurchaseAffordability() {
    const amount = parseFloat(document.getElementById('afford-amount-input')?.value || 10000);
    const desc = document.getElementById('afford-desc-input')?.value || 'Item';
    const { totalIncome, totalExpense } = calculateMetrics();
    const savings = totalIncome - totalExpense;
    const box = document.getElementById('affordability-verdict-box');
    if (!box) return;

    box.style.display = 'block';
    let verdictClass = 'verdict-safe';
    let verdictTitle = 'SAFE TO PURCHASE';
    let explanation = `Your monthly surplus of ${formatCurrency(savings)} easily covers this ${formatCurrency(amount)} purchase while maintaining your safety buffer.`;

    if (savings < amount) {
        verdictClass = 'verdict-danger';
        verdictTitle = 'NOT RECOMMENDED RIGHT NOW';
        explanation = `This purchase exceeds your current net savings (${formatCurrency(savings)}) and would cause a cashflow deficit of ${formatCurrency(amount - savings)}.`;
    } else if (savings < amount * 1.5) {
        verdictClass = 'verdict-caution';
        verdictTitle = 'PURCHASE WITH CAUTION';
        explanation = `This purchase consumes over 65% of your monthly surplus (${formatCurrency(savings)}), leaving minimal cushion for unexpected costs.`;
    }

    box.innerHTML = `
        <div class="affordability-verdict-card ${verdictClass}">
            <div class="d-flex justify-content-between align-items-center">
                <span class="card-kicker">AFFORDABILITY VERDICT</span>
                <strong style="font-size: 1.1rem;">${verdictTitle}</strong>
            </div>
            <h4>${escapeHtml(desc)} (${formatCurrency(amount)})</h4>
            <p>${explanation}</p>
            <hr style="opacity:0.2;">
            <div class="d-flex justify-content-between" style="font-size:0.85rem;">
                <span><strong>WHY?</strong> Calculated against monthly net cashflow buffer.</span>
                <span><strong>DATA USED:</strong> Current cycle recorded ledger.</span>
            </div>
        </div>
    `;
}
window.evaluatePurchaseAffordability = evaluatePurchaseAffordability;

// 3. Digital Twin
function renderDigitalTwinView() {
    const metrics = calculateMetrics();
    const container = document.getElementById('twin-metrics-container');
    if (!container) return;

    container.innerHTML = `
        <div class="twin-metric-item"><span>Avg Monthly Income</span><strong>${formatCurrency(metrics.totalIncome)}</strong></div>
        <div class="twin-metric-item"><span>Avg Monthly Expenses</span><strong>${formatCurrency(metrics.totalExpense)}</strong></div>
        <div class="twin-metric-item"><span>Avg Monthly Savings</span><strong>${formatCurrency(metrics.savings)}</strong></div>
        <div class="twin-metric-item"><span>Savings Rate</span><strong>${metrics.savingsRate.toFixed(0)}%</strong></div>
    `;
}

// 4. Habit Score
function renderHabitScoreView() {
    const habit = calculateMoneyHabitScore();
    const scoreBig = document.getElementById('habit-score-big');
    const scoreLabel = document.getElementById('habit-score-label');
    const container = document.getElementById('habit-checklist-container');

    if (scoreBig) scoreBig.textContent = habit.score;
    if (scoreLabel) scoreLabel.textContent = habit.label;

    if (container) {
        container.innerHTML = habit.bulletPoints.map(b => `
            <div class="habit-check-item">
                <i class="fa-solid ${b.isPositive ? 'fa-circle-check text-emerald' : 'fa-triangle-exclamation text-amber'}" style="font-size:1.3rem;"></i>
                <div>
                    <strong>${escapeHtml(b.title)}</strong>
                    <p class="text-muted" style="font-size:0.85rem; margin-top:2px;">${escapeHtml(b.description)}</p>
                </div>
            </div>
        `).join('');
    }
}

// 5. Essential vs Discretionary
function renderEssentialView() {
    const ess = calculateEssentialVsDiscretionary();
    const container = document.getElementById('essential-full-breakdown');
    if (!container) return;

    container.innerHTML = `
        <p class="mb-3">${escapeHtml(ess.recommendation)}</p>
        <div class="essential-summary-boxes mb-3">
            <div class="essential-box">
                <span>ESSENTIAL (Needs)</span>
                <strong class="text-indigo" style="font-size:1.4rem;">${formatCurrency(ess.essentialTotal)}</strong>
                <small>${ess.essentialPct.toFixed(0)}% of total</small>
            </div>
            <div class="essential-box">
                <span>DISCRETIONARY (Wants)</span>
                <strong class="text-emerald" style="font-size:1.4rem;">${formatCurrency(ess.discretionaryTotal)}</strong>
                <small>${ess.discretionaryPct.toFixed(0)}% of total</small>
            </div>
        </div>
    `;
}

// 6. Recurring Map
function renderRecurringMapView() {
    const map = detectRecurringMoneyMap();
    const mTotal = document.getElementById('rec-monthly-total');
    const aTotal = document.getElementById('rec-annual-total');
    const tbody = document.getElementById('recurring-map-tbody');

    if (mTotal) mTotal.textContent = formatCurrency(map.monthlyRecurringTotal);
    if (aTotal) aTotal.textContent = formatCurrency(map.annualProjectedTotal);

    if (tbody) {
        if (map.items.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted">No recurring items identified.</td></tr>`;
        } else {
            tbody.innerHTML = map.items.map(item => `
                <tr>
                    <td><strong>${escapeHtml(item.name)}</strong></td>
                    <td>${escapeHtml(item.category)}</td>
                    <td>${item.frequency}</td>
                    <td class="text-primary font-weight-bold">${formatCurrency(item.monthlyAmount)}</td>
                    <td class="text-muted">${formatCurrency(item.annualAmount)}</td>
                </tr>
            `).join('');
        }
    }
}

// 7. Leaks View
function renderLeaksView() {
    const leaks = detectSpendingLeaks();
    const container = document.getElementById('leaks-full-container');
    if (!container) return;

    if (leaks.length === 0) {
        container.innerHTML = `<p class="text-muted">No micro-spending leaks detected.</p>`;
    } else {
        container.innerHTML = leaks.map(l => `
            <div class="glass-card p-3 mb-2 d-flex justify-content-between align-items-center">
                <div>
                    <strong>${escapeHtml(l.name)} (${l.frequencyCount} occurrences)</strong>
                    <p class="text-muted" style="font-size:0.85rem;">${escapeHtml(l.aiExplanation)}</p>
                </div>
                <strong class="text-danger" style="font-size:1.2rem;">~${formatCurrency(l.monthlyTotal)}/mo</strong>
            </div>
        `).join('');
    }
}

// 8. Roadmap View
function renderRoadmapView() {
    const roadmap = generateGoalRoadmap();
    const priority = calculateMultiGoalPriority();
    const rContainer = document.getElementById('roadmap-full-container');
    const pContainer = document.getElementById('priority-distribution-container');

    if (rContainer) {
        if (roadmap.length === 0) {
            rContainer.innerHTML = `<p class="text-muted">No active savings goals found.</p>`;
        } else {
            rContainer.innerHTML = roadmap.map(g => `
                <div class="glass-card p-3 mb-3">
                    <div class="d-flex justify-content-between">
                        <strong>${escapeHtml(g.title)}</strong>
                        <strong class="text-primary">${g.progressPct}%</strong>
                    </div>
                    <div class="progress-bar-container my-2"><div class="progress-bar-fill" style="width:${g.progressPct}%;"></div></div>
                    <p class="text-muted" style="font-size:0.85rem;">${escapeHtml(g.aiSuggestion)}</p>
                </div>
            `).join('');
        }
    }

    if (pContainer) {
        pContainer.innerHTML = priority.allocations.map(a => `
            <div class="d-flex justify-content-between mb-1" style="font-size:0.85rem;">
                <span>${escapeHtml(a.goalTitle)} (${a.allocationPercentage.toFixed(0)}%)</span>
                <strong class="text-primary">${formatCurrency(a.recommendedMonthlyAmount)}/mo</strong>
            </div>
        `).join('');
    }
}

// 9. Reviews View
function renderReviewsView() {
    const { totalIncome, totalExpense, savings } = calculateMetrics();
    const weeklySummary = document.getElementById('weekly-review-summary');
    const weeklyAction = document.getElementById('weekly-review-action');
    const monthEnd = document.getElementById('monthend-review-content');

    if (weeklySummary) weeklySummary.textContent = `Over the past 7 days, your cashflow remained positive with regular expense logging.`;
    if (weeklyAction) weeklyAction.textContent = `Avoid dining out more than twice next week to boost surplus by ${formatCurrency(1500)}.`;

    if (monthEnd) {
        monthEnd.innerHTML = `
            <p><strong>Net Surplus:</strong> ${formatCurrency(savings)}</p>
            <p class="mt-2"><strong>Recommended Next Month Savings Target:</strong> ${formatCurrency(Math.round(savings * 1.1))}</p>
        `;
    }
}

// 10. Forecast View
function renderForecastView() {
    const { totalIncome, totalExpense } = calculateMetrics();
    const container = document.getElementById('forecast-container');
    if (!container) return;

    container.innerHTML = `
        <div class="twin-metrics-grid mb-3">
            <div class="twin-metric-item"><span>Expected Income</span><strong>${formatCurrency(totalIncome)}</strong></div>
            <div class="twin-metric-item"><span>Expected Expenses</span><strong>${formatCurrency(totalExpense)}</strong></div>
            <div class="twin-metric-item"><span>Expected Net Savings</span><strong class="text-emerald">${formatCurrency(totalIncome - totalExpense)}</strong></div>
        </div>
        <small class="text-muted">* Estimates are calculated from historical averages and are not financial guarantees.</small>
    `;
}

// 11. Calendar View
function renderCalendarView() {
    const calList = document.getElementById('calendar-entries-list');
    const journeyList = document.getElementById('journey-timeline-list');
    const recMap = detectRecurringMoneyMap();

    if (calList) {
        calList.innerHTML = recMap.items.map(item => `
            <div class="cal-entry-item">
                <span>${escapeHtml(item.name)} (1st of month)</span>
                <strong class="text-danger">-${formatCurrency(item.monthlyAmount)}</strong>
            </div>
        `).join('') || `<p class="text-muted">No calendar entries.</p>`;
    }

    if (journeyList) {
        journeyList.innerHTML = `
            <div class="journey-milestone-item">
                <span>Account Created</span>
                <span class="badge-pill badge-primary">Active</span>
            </div>
        `;
    }
}

// Chat Assistant Handlers
async function sendUserAiMessage() {
    const inputEl = document.getElementById('ai-chat-input');
    if (!inputEl) return;
    const query = inputEl.value.trim();
    if (!query) return;

    inputEl.value = '';
    await askAiQuestion(query);
}
window.sendUserAiMessage = sendUserAiMessage;

async function askAiQuestion(question) {
    if (!question) return;
    if (currentAiMode !== 'chat') openAiMode('chat');

    appendAiChatMessage('user', question, new Date());
    const typingEl = document.getElementById('ai-typing-indicator');
    if (typingEl) typingEl.style.display = 'flex';
    scrollAiChatToBottom();

    const context = buildMinimalFinancialContext();
    const aiResponse = await fetchAiChatResponse(question, context);

    if (typingEl) typingEl.style.display = 'none';
    appendAiChatMessage('ai', aiResponse, new Date());
}
window.askAiQuestion = askAiQuestion;

function buildMinimalFinancialContext() {
    const metrics = calculateMetrics();
    const health = calculateFinancialHealthScore();
    return {
        currency: formatCurrency(0).charAt(0),
        metrics,
        financialHealth: health,
        financialMode: appState.userSettings.financialMode || 'SAVE',
        primaryGoal: appState.userSettings.primaryGoal || 'Save Money'
    };
}

async function fetchAiChatResponse(message, context) {
    try {
        let token = 'user_token';
        if (auth.currentUser) token = await auth.currentUser.getIdToken();

        const res = await fetch(`${AI_API_BASE}/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify({ message, context })
        });
        if (res.ok) {
            const data = await res.json();
            return data.response;
        }
    } catch (e) {
        console.warn('Fallback to deterministic client engine');
    }
    return generateClientDeterministicResponse(message, context);
}

function generateClientDeterministicResponse(query, context) {
    const q = (query || '').toLowerCase().trim();
    const currency = context.currency || '₹';
    const metrics = context.metrics || {};
    const savings = (metrics.totalIncome || 0) - (metrics.totalExpense || 0);

    if (q.includes('afford')) {
        const match = q.match(/[\d,]+/);
        const amt = match ? parseFloat(match[0].replace(/,/g, '')) : 10000;
        if (savings >= amt * 1.5) {
            return `### ⚖️ Affordability Verdict: SAFE\n\nYou can make this purchase of ${currency}${amt.toLocaleString()} while maintaining your savings buffer.\n\n**WHY?**\nYour net monthly surplus (${currency}${savings.toLocaleString()}) leaves ${currency}${(savings - amt).toLocaleString()} in reserves.\n\n**DATA USED:**\nCurrent recorded cycle ledger.`;
        } else {
            return `### ⚖️ Affordability Verdict: CAUTION / NOT RECOMMENDED\n\nThis purchase of ${currency}${amt.toLocaleString()} would strain your current monthly surplus of ${currency}${savings.toLocaleString()}.\n\n**WHY?**\nLeaves inadequate cushion for unpredictable expenses.\n\n**DATA USED:**\nCalculated against recorded cashflow.`;
        }
    }

    return `### ✨ SpendWise Financial Copilot\n\n- **Income:** ${currency}${(metrics.totalIncome || 0).toLocaleString()}\n- **Expenses:** ${currency}${(metrics.totalExpense || 0).toLocaleString()}\n- **Net Surplus:** ${currency}${savings.toLocaleString()}\n- **Active Mode:** ${context.financialMode}\n\nAsk me *"Can I afford a ₹10,000 purchase?"* or *"Simulate ₹2,000 food reduction."*`;
}

function appendAiChatMessage(sender, text, timestamp, saveToDb = true) {
    const listEl = document.getElementById('ai-chat-messages');
    if (!listEl) return;

    const isBot = sender === 'ai';
    const row = document.createElement('div');
    row.className = `ai-msg-row ${isBot ? 'ai-msg-bot' : 'ai-msg-user'}`;
    const timeStr = timestamp ? new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Now';

    row.innerHTML = `
        <div class="ai-msg-avatar"><i class="fa-solid ${isBot ? 'fa-wand-magic-sparkles' : 'fa-user'}"></i></div>
        <div class="ai-msg-bubble">
            <div class="ai-msg-header">
                <span class="ai-msg-sender">${isBot ? 'Financial Copilot' : 'You'}</span>
                <span class="ai-msg-time">${timeStr}</span>
            </div>
            <div class="ai-msg-body">${formatMarkdownText(text)}</div>
        </div>
    `;

    listEl.appendChild(row);
    scrollAiChatToBottom();

    if (saveToDb && appState.currentUser) {
        db.collection('users').doc(appState.currentUser.uid)
            .collection('aiConversations').add({
                sender,
                text,
                timestamp: firebase.firestore.FieldValue.serverTimestamp()
            }).catch(() => {});
    }
}

function scrollAiChatToBottom() {
    const listEl = document.getElementById('ai-chat-messages');
    if (listEl) listEl.scrollTop = listEl.scrollHeight;
}

async function loadAiConversations() {
    if (!appState.currentUser) return;
    aiConversationsLoaded = true;
    try {
        const snapshot = await db.collection('users').doc(appState.currentUser.uid)
            .collection('aiConversations').orderBy('timestamp', 'asc').limit(50).get();
        if (!snapshot.empty) {
            const listEl = document.getElementById('ai-chat-messages');
            if (listEl) listEl.innerHTML = '';
            snapshot.docs.forEach(doc => {
                const data = doc.data();
                const d = data.timestamp?.toDate ? data.timestamp.toDate() : new Date();
                appendAiChatMessage(data.sender, data.text, d, false);
            });
        }
    } catch (e) {}
}

async function clearAiConversation() {
    if (!confirm('Clear Copilot conversation history?')) return;
    const listEl = document.getElementById('ai-chat-messages');
    if (listEl) {
        listEl.innerHTML = `
            <div class="ai-msg-row ai-msg-bot">
                <div class="ai-msg-avatar"><i class="fa-solid fa-wand-magic-sparkles"></i></div>
                <div class="ai-msg-bubble">
                    <div class="ai-msg-header"><span class="ai-msg-sender">Financial Copilot</span><span class="ai-msg-time">Just now</span></div>
                    <div class="ai-msg-body">Conversation cleared. How can I assist you with your finances today?</div>
                </div>
            </div>
        `;
    }
    if (appState.currentUser) {
        const snap = await db.collection('users').doc(appState.currentUser.uid).collection('aiConversations').get();
        const batch = db.batch();
        snap.docs.forEach(doc => batch.delete(doc.ref));
        await batch.commit();
        showToast('Chat history cleared.');
    }
}
window.clearAiConversation = clearAiConversation;

function formatMarkdownText(text) {
    if (!text) return '';
    let html = escapeHtml(text);
    html = html.replace(/^### (.*$)/gim, '<h4>$1</h4>');
    html = html.replace(/^## (.*$)/gim, '<h3>$1</h3>');
    html = html.replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>');
    html = html.replace(/\*(.*?)\*/gim, '<em>$1</em>');
    html = html.replace(/^\- (.*$)/gim, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>)/gim, '<ul>$1</ul>');
    html = html.replace(/\n\n/gim, '<br><br>');
    html = html.replace(/\n/gim, '<br>');
    return html;
}

/* ==========================================================================
   Ledger, Budgets, Goals, and Form Modals
   ========================================================================== */
function renderRecentTransactions() {
    const tbody = document.getElementById('dashboard-recent-tbody');
    if (!tbody) return;
    const txs = getAllTransactions().slice(0, 4);

    if (txs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted">No transactions logged yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = txs.map(t => {
        const isInc = t.type === 'Income';
        return `
            <tr>
                <td><span class="badge-pill ${isInc ? 'badge-emerald' : 'badge-amber'}">${t.type}</span></td>
                <td><i class="fa-solid ${getCategoryIcon(t.category)} mr-1 text-primary"></i> ${escapeHtml(t.category)}</td>
                <td>${escapeHtml(t.description || t.category)}</td>
                <td>${t.date}</td>
                <td class="text-right font-weight-bold ${isInc ? 'text-success' : 'text-danger'}">${isInc ? '+' : '-'}${formatCurrency(t.amount)}</td>
            </tr>
        `;
    }).join('');
}

function renderTransactionsTable() {
    const tbody = document.getElementById('transactions-table-body');
    if (!tbody) return;
    const txs = getAllTransactions();

    if (txs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">No transactions logged yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = txs.map(t => {
        const isInc = t.type === 'Income';
        return `
            <tr>
                <td><span class="badge-pill ${isInc ? 'badge-emerald' : 'badge-amber'}">${t.type}</span></td>
                <td><span class="badge-pill badge-sky">${t.source || 'MANUAL'}</span></td>
                <td><i class="fa-solid ${getCategoryIcon(t.category)} mr-1 text-primary"></i> ${escapeHtml(t.category)}</td>
                <td>${escapeHtml(t.description || t.category)}</td>
                <td>${t.paymentMethod || 'UPI'}</td>
                <td>${t.date}</td>
                <td class="text-right font-weight-bold ${isInc ? 'text-success' : 'text-danger'}">${isInc ? '+' : '-'}${formatCurrency(t.amount)}</td>
                <td class="text-center">
                    <button class="btn-icon text-danger" onclick="deleteTransaction('${t.id}', '${t.type}')"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `;
    }).join('');
}

async function deleteTransaction(id, type) {
    if (!confirm(`Delete this ${type.toLowerCase()} record?`)) return;
    if (!appState.currentUser) return;
    const col = type === 'Income' ? 'incomes' : 'expenses';
    await db.collection('users').doc(appState.currentUser.uid).collection(col).doc(id).delete();
    showToast('Record deleted');
}
window.deleteTransaction = deleteTransaction;

function renderBudgets() {
    const container = document.getElementById('budgets-container');
    if (!container) return;
    const { categoryTotals } = calculateMetrics();

    if (appState.budgets.length === 0) {
        container.innerHTML = `<p class="text-muted p-4">No budgets set. Click "Set Budget Cap" to add one.</p>`;
        return;
    }

    container.innerHTML = appState.budgets.map(b => {
        const spent = categoryTotals[b.category] || 0;
        const pct = b.amount > 0 ? Math.min(100, Math.round((spent / b.amount) * 100)) : 0;
        const isExceeded = spent > b.amount;
        return `
            <div class="glass-card p-3">
                <div class="d-flex justify-content-between align-items-center">
                    <strong><i class="fa-solid ${getCategoryIcon(b.category)} text-primary mr-1"></i> ${escapeHtml(b.category)}</strong>
                    <span class="badge-pill ${isExceeded ? 'badge-rose' : 'badge-emerald'}">${pct}%</span>
                </div>
                <div class="progress-bar-container my-2">
                    <div class="progress-bar-fill" style="width:${pct}%; background:${isExceeded ? 'var(--accent-rose)' : 'var(--primary)'};"></div>
                </div>
                <div class="d-flex justify-content-between" style="font-size:0.85rem;">
                    <span>Spent: ${formatCurrency(spent)}</span>
                    <span>Cap: ${formatCurrency(b.amount)}</span>
                </div>
            </div>
        `;
    }).join('');
}

function renderGoals() {
    const container = document.getElementById('goals-container');
    if (!container) return;

    if (appState.goals.length === 0) {
        container.innerHTML = `<p class="text-muted p-4">No active savings goals. Click "New Savings Goal" to create one.</p>`;
        return;
    }

    container.innerHTML = appState.goals.map(g => {
        const pct = g.targetAmount > 0 ? Math.min(100, Math.round((g.currentAmount / g.targetAmount) * 100)) : 0;
        return `
            <div class="glass-card p-3">
                <div class="d-flex justify-content-between align-items-center">
                    <strong><i class="fa-solid fa-bullseye text-primary mr-1"></i> ${escapeHtml(g.title)}</strong>
                    <span class="badge-pill badge-emerald">${pct}%</span>
                </div>
                <div class="progress-bar-container my-2"><div class="progress-bar-fill" style="width:${pct}%;"></div></div>
                <div class="d-flex justify-content-between" style="font-size:0.85rem;">
                    <span>Saved: ${formatCurrency(g.currentAmount)}</span>
                    <span>Target: ${formatCurrency(g.targetAmount)}</span>
                </div>
            </div>
        `;
    }).join('');
}

function renderSettingsUI() {
    const curr = appState.userSettings.currency || 'INR';
    document.querySelectorAll('.btn-currency-chip').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-currency') === curr);
    });
    const mode = appState.userSettings.financialMode || 'SAVE';
    document.querySelectorAll('.btn-mode-chip').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-mode') === mode);
    });
    const pGoal = appState.userSettings.primaryGoal || 'Save Money';
    document.querySelectorAll('.btn-goal-chip').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-pgoal') === pGoal);
    });
}

function renderCharts() {
    renderTrendChart();
    renderCategoryChart();
}

function renderTrendChart() {
    const canvas = document.getElementById('trendChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (appState.chartTrend) appState.chartTrend.destroy();

    const labels = ['Week 1', 'Week 2', 'Week 3', 'Week 4'];
    const exps = [2500, 3200, 2100, 4000];
    const incs = [15000, 0, 0, 0];

    appState.chartTrend = new Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets: [
                { label: 'Income', data: incs, borderColor: '#10b981', tension: 0.3 },
                { label: 'Expenses', data: exps, borderColor: '#ef4444', tension: 0.3 }
            ]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

function renderCategoryChart() {
    const canvas = document.getElementById('categoryChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (appState.chartCategory) appState.chartCategory.destroy();

    const { categoryTotals } = calculateMetrics();
    const labels = Object.keys(categoryTotals);
    const data = Object.values(categoryTotals);

    appState.chartCategory = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels.length ? labels : ['No Expenses'],
            datasets: [{ data: data.length ? data : [1], backgroundColor: ['#6366f1', '#10b981', '#f59e0b', '#ec4899', '#3b82f6', '#8b5cf6'] }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

/* ==========================================================================
   Modals & UI Helpers
   ========================================================================== */
function openModal(id) {
    const m = document.getElementById(id);
    if (m) m.classList.add('active');
}
window.openModal = openModal;

function closeModal(id) {
    const m = document.getElementById(id);
    if (m) m.classList.remove('active');
}
window.closeModal = closeModal;

function openTransactionModal(type = 'Expense') {
    document.getElementById('tx-modal-title').textContent = `Add ${type}`;
    document.getElementById('tx-form').reset();
    document.getElementById('tx-id').value = '';
    document.getElementById('tx-date').value = new Date().toISOString().split('T')[0];
    if (type === 'Income') document.getElementById('type-income').checked = true;
    else document.getElementById('type-expense').checked = true;
    updateCategoryDropdown(type);
    openModal('tx-modal');
}
window.openTransactionModal = openTransactionModal;

function openBudgetModal() {
    document.getElementById('budget-form').reset();
    const sel = document.getElementById('budget-category');
    if (sel) sel.innerHTML = CATEGORIES.Expense.map(c => `<option value="${c}">${c}</option>`).join('');
    openModal('budget-modal');
}
window.openBudgetModal = openBudgetModal;

function openGoalModal() {
    document.getElementById('goal-form').reset();
    openModal('goal-modal');
}
window.openGoalModal = openGoalModal;

function updateCategoryDropdown(type) {
    const sel = document.getElementById('tx-category');
    if (!sel) return;
    const list = CATEGORIES[type] || CATEGORIES.Expense;
    sel.innerHTML = list.map(c => `<option value="${c}">${c}</option>`).join('');
}
window.updateCategoryDropdown = updateCategoryDropdown;

function initForms() {
    // Transaction Form
    document.getElementById('tx-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!appState.currentUser) return;
        const type = document.querySelector('input[name="tx-type"]:checked').value;
        const amount = parseFloat(document.getElementById('tx-amount').value);
        const category = document.getElementById('tx-category').value;
        const description = document.getElementById('tx-desc').value.trim();
        const paymentMethod = document.getElementById('tx-payment').value;
        const date = document.getElementById('tx-date').value;

        const col = type === 'Income' ? 'incomes' : 'expenses';
        if (type === 'Income') {
            await db.collection('users').doc(appState.currentUser.uid).collection('incomes').add({
                userId: appState.currentUser.uid,
                amount,
                source: category,
                category,
                description,
                paymentMethod,
                origin: 'MANUAL',
                reviewStatus: 'confirmed',
                date,
                createdAt: firebase.firestore.FieldValue.serverTimestamp(),
                updatedAt: firebase.firestore.FieldValue.serverTimestamp()
            });
        } else {
            const isEss = isCategoryEssential(category);
            await db.collection('users').doc(appState.currentUser.uid).collection('expenses').add({
                userId: appState.currentUser.uid,
                amount,
                category,
                description,
                paymentMethod,
                source: 'MANUAL',
                reviewStatus: 'confirmed',
                isEssential: isEss,
                date,
                createdAt: firebase.firestore.FieldValue.serverTimestamp(),
                updatedAt: firebase.firestore.FieldValue.serverTimestamp()
            });
        }

        closeModal('tx-modal');
        showToast(`${type} added successfully!`);
    });

    // Budget Form
    document.getElementById('budget-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!appState.currentUser) return;
        const category = document.getElementById('budget-category').value;
        const amount = parseFloat(document.getElementById('budget-amount').value);

        await db.collection('users').doc(appState.currentUser.uid).collection('budgets').add({
            userId: appState.currentUser.uid,
            category,
            amount,
            spentAmount: 0,
            createdAt: firebase.firestore.FieldValue.serverTimestamp()
        });

        closeModal('budget-modal');
        showToast('Budget saved!');
    });

    // Goal Form
    document.getElementById('goal-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!appState.currentUser) return;
        const title = document.getElementById('goal-title').value.trim();
        const targetAmount = parseFloat(document.getElementById('goal-target').value);
        const currentAmount = parseFloat(document.getElementById('goal-current').value || 0);
        const deadline = document.getElementById('goal-date').value;

        await db.collection('users').doc(appState.currentUser.uid).collection('savingsGoals').add({
            userId: appState.currentUser.uid,
            title,
            targetAmount,
            currentAmount,
            deadline,
            createdAt: firebase.firestore.FieldValue.serverTimestamp()
        });

        closeModal('goal-modal');
        showToast('Savings goal saved!');
    });
}

function initVoiceRecognition() {
    const micBtn = document.getElementById('voice-mic-trigger');
    const statusText = document.getElementById('voice-status-text');
    const resultBox = document.getElementById('voice-result-preview');
    const transcriptText = document.getElementById('voice-transcript-text');
    const applyBtn = document.getElementById('btn-voice-apply');

    let parsedExpense = null;
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) return;
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const recognition = new SpeechRecognition();

    micBtn?.addEventListener('click', () => {
        statusText.textContent = 'Listening... Speak clearly!';
        recognition.start();
    });

    recognition.onresult = (e) => {
        const transcript = e.results[0][0].transcript;
        statusText.textContent = 'Voice command recognized!';
        transcriptText.textContent = `"${transcript}"`;
        resultBox.style.display = 'block';

        const amountMatch = transcript.match(/\d+/);
        parsedExpense = {
            amount: amountMatch ? parseFloat(amountMatch[0]) : 500,
            category: 'Food',
            description: transcript
        };
    };

    applyBtn?.addEventListener('click', () => {
        if (parsedExpense) {
            closeModal('voice-modal');
            openTransactionModal('Expense');
            document.getElementById('tx-amount').value = parsedExpense.amount;
            document.getElementById('tx-desc').value = parsedExpense.description;
        }
    });
}

function initReceiptScanner() {
    const dropzone = document.getElementById('scanner-dropzone');
    const fileInput = document.getElementById('receipt-file-input');
    const progressBox = document.getElementById('scan-progress-box');
    const resultBox = document.getElementById('scan-result-box');
    const applyBtn = document.getElementById('btn-apply-ocr');

    let parsedReceipt = { amount: 450, desc: 'Receipt Scan', category: 'Shopping' };

    dropzone?.addEventListener('click', () => fileInput?.click());
    fileInput?.addEventListener('change', (e) => {
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];
            const fileName = file.name;
            dropzone.style.display = 'none';
            progressBox.style.display = 'block';

            setTimeout(() => {
                progressBox.style.display = 'none';
                resultBox.style.display = 'block';

                // Intelligent filename/text parser
                let detectedAmount = 450;
                let detectedDesc = fileName.replace(/\.[^/.]+$/, "").replace(/[_-]/g, " ");
                let detectedCat = 'Shopping';

                const numMatch = fileName.match(/\d+(\.\d+)?/);
                if (numMatch) {
                    detectedAmount = parseFloat(numMatch[0]);
                }

                const lower = fileName.toLowerCase();
                if (lower.includes('food') || lower.includes('zomato') || lower.includes('swiggy') || lower.includes('rest')) {
                    detectedCat = 'Food';
                } else if (lower.includes('uber') || lower.includes('ola') || lower.includes('fuel') || lower.includes('petrol')) {
                    detectedCat = 'Transport';
                } else if (lower.includes('med') || lower.includes('pharm') || lower.includes('hospital')) {
                    detectedCat = 'Healthcare';
                } else if (lower.includes('bill') || lower.includes('util') || lower.includes('wifi')) {
                    detectedCat = 'Utilities';
                }

                parsedReceipt = {
                    amount: detectedAmount,
                    desc: detectedDesc || 'Retail Receipt',
                    category: detectedCat
                };

                const ocrAmtEl = document.getElementById('ocr-amount');
                const ocrDescEl = document.getElementById('ocr-desc');
                if (ocrAmtEl) ocrAmtEl.textContent = formatCurrency(detectedAmount);
                if (ocrDescEl) ocrDescEl.textContent = parsedReceipt.desc;
            }, 800);
        }
    });

    applyBtn?.addEventListener('click', () => {
        closeModal('scan-modal');
        openTransactionModal('Expense');
        const amtInput = document.getElementById('tx-amount');
        const descInput = document.getElementById('tx-desc');
        const catSelect = document.getElementById('tx-category');
        if (amtInput) amtInput.value = parsedReceipt.amount;
        if (descInput) descInput.value = parsedReceipt.desc;
        if (catSelect) catSelect.value = parsedReceipt.category;
    });
}

function initSmartImport() {
    const dropzone = document.getElementById('smart-import-dropzone');
    const fileInput = document.getElementById('smart-import-file-input');
    dropzone?.addEventListener('click', () => fileInput?.click());
}

function initAuthModals() {
    const tabLogin = document.getElementById('tab-btn-login');
    const tabReg = document.getElementById('tab-btn-register');
    const formLogin = document.getElementById('auth-login-form');
    const formReg = document.getElementById('auth-register-form');
    const btnLogout = document.getElementById('btn-logout');

    tabLogin?.addEventListener('click', () => {
        tabLogin.classList.add('active');
        tabReg?.classList.remove('active');
        if (formLogin) formLogin.style.display = 'block';
        if (formReg) formReg.style.display = 'none';
    });

    tabReg?.addEventListener('click', () => {
        tabReg.classList.add('active');
        tabLogin?.classList.remove('active');
        if (formReg) formReg.style.display = 'block';
        if (formLogin) formLogin.style.display = 'none';
    });

    formLogin?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email')?.value.trim();
        const pwd = document.getElementById('login-password')?.value;
        const errMsg = document.getElementById('login-error-msg');

        if (!email || !pwd) return;
        try {
            if (errMsg) errMsg.style.display = 'none';
            await auth.signInWithEmailAndPassword(email, pwd);
            closeModal('auth-modal');
        } catch (err) {
            if (errMsg) {
                errMsg.textContent = err.message || 'Invalid email or password.';
                errMsg.style.display = 'block';
            }
        }
    });

    formReg?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('reg-name')?.value.trim();
        const email = document.getElementById('reg-email')?.value.trim();
        const pwd = document.getElementById('reg-password')?.value;
        const confirmPwd = document.getElementById('reg-confirm-password')?.value;
        const errMsg = document.getElementById('register-error-msg');

        if (!email || !pwd || !name) return;
        if (pwd !== confirmPwd) {
            if (errMsg) {
                errMsg.textContent = 'Passwords do not match.';
                errMsg.style.display = 'block';
            }
            return;
        }

        try {
            if (errMsg) errMsg.style.display = 'none';
            const userCred = await auth.createUserWithEmailAndPassword(email, pwd);
            if (userCred.user) {
                await userCred.user.updateProfile({ displayName: name });
                await db.collection('users').doc(userCred.user.uid).set({
                    displayName: name,
                    email: email,
                    createdAt: firebase.firestore.FieldValue.serverTimestamp()
                }, { merge: true });
            }
            closeModal('auth-modal');
        } catch (err) {
            if (errMsg) {
                errMsg.textContent = err.message || 'Error creating account.';
                errMsg.style.display = 'block';
            }
        }
    });

    btnLogout?.addEventListener('click', async () => {
        if (confirm('Are you sure you want to sign out?')) {
            await auth.signOut();
            showToast('Signed out successfully.');
        }
    });
}

function exportLedgerCsv() {
    const txs = getAllTransactions();
    if (!txs.length) return showToast('No records to export');
    const rows = [['Type', 'Category', 'Description', 'Date', 'Amount']];
    txs.forEach(t => rows.push([t.type, t.category, `"${t.description || ''}"`, t.date, t.amount]));
    const csvContent = "data:text/csv;charset=utf-8," + rows.map(e => e.join(",")).join("\n");
    const link = document.createElement("a");
    link.href = encodeURI(csvContent);
    link.download = `SpendWise_Export_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
}
window.exportLedgerCsv = exportLedgerCsv;

function getCategoryIcon(cat) {
    const map = {
        'Food': 'fa-utensils', 'Transport': 'fa-car', 'Shopping': 'fa-bag-shopping',
        'Rent': 'fa-house', 'Utilities': 'fa-bolt', 'Healthcare': 'fa-heart-pulse',
        'Education': 'fa-graduation-cap', 'Entertainment': 'fa-film', 'Subscriptions': 'fa-repeat',
        'Travel': 'fa-plane', 'Salary': 'fa-money-bill-wave', 'Freelance': 'fa-laptop-code',
        'Investment': 'fa-chart-line', 'Other': 'fa-receipt'
    };
    return map[cat] || 'fa-receipt';
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, m => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[m]);
}

function showToast(msg) {
    const toast = document.getElementById('app-toast');
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add('active');
    setTimeout(() => toast.classList.remove('active'), 3000);
}

/* ==========================================================================
   Profile & Reports Screen Renderers
   ========================================================================== */
function renderProfileView() {
    const nameEl = document.getElementById('profile-display-name');
    const emailEl = document.getElementById('profile-display-email');
    const inputName = document.getElementById('profile-input-name');
    const inputEmail = document.getElementById('profile-input-email');

    const user = appState.currentUser;
    if (user) {
        if (nameEl) nameEl.textContent = user.name || 'SpendWise User';
        if (emailEl) emailEl.textContent = user.email || 'user@spendwise.app';
        if (inputName) inputName.value = user.name || '';
        if (inputEmail) inputEmail.value = user.email || '';
    }
}
window.renderProfileView = renderProfileView;

function renderReportsView() {
    const { totalIncome, totalExpense, savings, savingsRate, categoryTotals } = calculateMetrics();
    const health = calculateFinancialHealthScore();
    const habit = calculateMoneyHabitScore();

    const stmtInc = document.getElementById('report-stat-income');
    const stmtExp = document.getElementById('report-stat-expense');
    const stmtSav = document.getElementById('report-stat-savings');
    const stmtRate = document.getElementById('report-stat-rate');
    const stmtHealth = document.getElementById('report-stat-health');

    if (stmtInc) stmtInc.textContent = formatCurrency(totalIncome);
    if (stmtExp) stmtExp.textContent = formatCurrency(totalExpense);
    if (stmtSav) stmtSav.textContent = formatCurrency(savings);
    if (stmtRate) stmtRate.textContent = `${savingsRate.toFixed(1)}%`;
    if (stmtHealth) stmtHealth.textContent = `${health.totalScore}/100`;

    const catList = document.getElementById('report-category-list');
    if (catList) {
        const entries = Object.entries(categoryTotals).sort((a, b) => b[1] - a[1]);
        if (entries.length === 0) {
            catList.innerHTML = `<p class="text-muted">No expenses recorded for this report cycle.</p>`;
        } else {
            catList.innerHTML = entries.map(([cat, amt]) => {
                const pct = totalExpense > 0 ? ((amt / totalExpense) * 100).toFixed(1) : '0.0';
                return `
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span><i class="fa-solid ${getCategoryIcon(cat)} text-primary mr-2"></i> ${escapeHtml(cat)}</span>
                        <div>
                            <strong>${formatCurrency(amt)}</strong>
                            <small class="text-muted ml-2">(${pct}%)</small>
                        </div>
                    </div>
                `;
            }).join('');
        }
    }

    const insightsList = document.getElementById('report-insights-list');
    if (insightsList) {
        insightsList.innerHTML = `
            <div class="glass-card p-3 mb-2">
                <strong>Financial Stability: ${health.label}</strong>
                <p class="text-muted mt-1" style="font-size: 0.85rem;">Your financial health score is ${health.totalScore}/100 with a behavioral money habit score of ${habit.score}/100.</p>
            </div>
            <div class="glass-card p-3">
                <strong>Recommended Executive Action:</strong>
                <p class="text-muted mt-1" style="font-size: 0.85rem;">Maintain essential spending discipline and allocate ${formatCurrency(Math.round(savings * 0.4))} towards active savings milestones.</p>
            </div>
        `;
    }
}
window.renderReportsView = renderReportsView;

// Bind Profile Form Handlers
document.addEventListener('DOMContentLoaded', () => {
    const profileForm = document.getElementById('profile-update-form');
    profileForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const newName = document.getElementById('profile-input-name')?.value.trim();
        if (!newName || !auth.currentUser) return;

        try {
            await auth.currentUser.updateProfile({ displayName: newName });
            appState.currentUser.name = newName;
            updateUserUI(auth.currentUser);
            renderProfileView();
            showToast('Profile name updated successfully!');
        } catch (err) {
            showToast('Error updating profile: ' + err.message);
        }
    });

    const passwordForm = document.getElementById('profile-password-form');
    passwordForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const p1 = document.getElementById('profile-new-password')?.value;
        const p2 = document.getElementById('profile-confirm-password')?.value;

        if (!p1 || p1.length < 8) {
            return showToast('Password must be at least 8 characters long.');
        }
        if (p1 !== p2) {
            return showToast('Passwords do not match.');
        }
        if (!auth.currentUser) return;

        try {
            await auth.currentUser.updatePassword(p1);
            passwordForm.reset();
            showToast('Password changed securely!');
        } catch (err) {
            showToast('Error changing password: ' + err.message);
        }
    });
});

