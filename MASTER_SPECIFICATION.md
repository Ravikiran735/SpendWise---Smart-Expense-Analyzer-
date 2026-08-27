# SpendWise — Unified Master Specification (Single Source of Truth)

This document is the authoritative cross-platform specification for SpendWise across **Android**, **Web**, and the **AI Backend**. Both platforms MUST adhere to these exact definitions, schemas, calculations, categorizations, and design tokens.

---

## 1. Design System Tokens & Colors

| Token Name | Hex Code | Purpose |
| :--- | :--- | :--- |
| `Primary` | `#6366F1` | Primary brand color, hero accents, active states |
| `Success` | `#10B981` | Positive cashflow, healthy budgets, safe verdicts |
| `Warning` | `#F59E0B` | Budget warnings (80–99%), caution verdicts, anomalies |
| `Danger` | `#F43F5E` | Budget exceeded (≥100%), negative savings, unsafe verdicts |
| `Info` | `#06B6D4` | Information badges, secondary callouts, sync statuses |
| `Dark Background` | `#0A0E17` | Canvas background for Dark theme |
| `Light Background` | `#F8FAFC` | Canvas background for Light theme |
| `Surface Dark` | `#131B2E` | Card surface color in Dark mode |
| `Surface Light` | `#FFFFFF` | Card surface color in Light mode |
| `Border Dark` | `rgba(255, 255, 255, 0.08)` | Standard card border in Dark mode |
| `Border Light` | `rgba(0, 0, 0, 0.08)` | Standard card border in Light mode |

### Radii Hierarchy
- **Extra Small**: `4px` (Tags, chips)
- **Small**: `8px` (Form controls, sub-items)
- **Medium**: `12px` (Standard cards, alert items)
- **Large**: `16px` (Hero cards, modal containers)
- **Full / Pill**: `9999px` (Badges, action buttons, slider thumbs)

---

## 2. Navigation Architecture & Destination Order

Both Android and Web must display and support the 10 core destinations in this exact order:

1. **Dashboard** (`dashboard`) — Command Center, Dual Scores, Essential vs Discretionary, One Action, Leaks, Roadmap.
2. **Transactions** (`transactions`) — Ledger of all income & expenses with search, filter, and pagination.
3. **Smart Import** (`smart_import`) — Bank statement import (CSV, Excel/XLSX), Receipt OCR, Voice Entry, and Import History.
4. **Budgets** (`budgets`) — Category monthly limits, spent vs remaining, progress bars, and status pills.
5. **Savings Goals** (`savings_goals`) — Milestone progress, target vs saved, projected dates, and contribution calculator.
6. **Smart Insights / Money Alerts** (`insights`) — Proactive categorization: Budget Risk, Opportunity, Milestone, Anomaly, Recurring.
7. **Financial Copilot** (`spendwise_ai`) — 12-subview intelligence hub (Chat, Simulator, Affordability, Digital Twin, Habit Score, Essential/Discretionary, Recurring Map, Leaks, Roadmap/Priority, Reviews, Forecast, Calendar/Journey).
8. **Reports** (`reports`) — Executive statements, category breakdown, budget performance audit, CSV & PDF exports.
9. **Profile** (`profile`) — User account, display name, email, security credentials, and sync status.
10. **Settings** (`settings`) — Financial Mode (`BUILD`, `BALANCE`, `SAVE`, `CONTROL`), Primary Goal, Currency (`INR`, `USD`, `EUR`, `GBP`), Theme, and Notification toggles.

---

## 3. Categories & Payment Methods

### Expense Categories (12 Canonical)
1. `Food` (Essential)
2. `Transport` (Discretionary / Essential by override)
3. `Shopping` (Discretionary)
4. `Rent` (Essential)
5. `Utilities` (Essential)
6. `Education` (Essential)
7. `Healthcare` (Essential)
8. `Entertainment` (Discretionary)
9. `Subscriptions` (Discretionary)
10. `Travel` (Discretionary)
11. `Investment` (Savings / Essential)
12. `Other` (Discretionary)

### Income Categories (6 Canonical)
1. `Salary`
2. `Freelance`
3. `Business`
4. `Investment`
5. `Gift`
6. `Other`

### Payment Methods (6 Canonical)
1. `Cash`
2. `UPI`
3. `Credit Card`
4. `Debit Card`
5. `Bank Transfer`
6. `Other`

### Import Sources (5 Canonical)
1. `MANUAL`
2. `CSV`
3. `EXCEL`
4. `RECEIPT`
5. `VOICE`

---

## 4. Firestore Database Structure

```
users/{uid}
├── expenses/{expenseId}
│   ├── userId: String
│   ├── amount: Double
│   ├── category: String
│   ├── description: String
│   ├── paymentMethod: String
│   ├── source: String ("MANUAL" | "CSV" | "EXCEL" | "RECEIPT" | "VOICE")
│   ├── importId: String?
│   ├── isEssential: Boolean?
│   ├── categoryConfidence: Double?
│   ├── reviewStatus: String? ("AUTO_APPROVED" | "NEEDS_REVIEW")
│   ├── date: Timestamp | String (YYYY-MM-DD)
│   ├── createdAt: Timestamp
│   └── updatedAt: Timestamp?
│
├── incomes/{incomeId}
│   ├── userId: String
│   ├── amount: Double
│   ├── source: String
│   ├── category: String
│   ├── description: String
│   ├── origin: String ("MANUAL" | "CSV" | "EXCEL" | "VOICE")
│   ├── paymentMethod: String
│   ├── importId: String?
│   ├── date: Timestamp | String (YYYY-MM-DD)
│   ├── createdAt: Timestamp
│   └── updatedAt: Timestamp?
│
├── budgets/{budgetId}
│   ├── userId: String
│   ├── category: String
│   ├── amount: Double
│   ├── spentAmount: Double
│   └── createdAt: Timestamp
│
├── savingsGoals/{goalId}
│   ├── userId: String
│   ├── title: String
│   ├── targetAmount: Double
│   ├── currentAmount: Double
│   ├── deadline: Timestamp | String?
│   └── createdAt: Timestamp
│
├── settings/preferences
│   ├── theme: String ("dark" | "light")
│   ├── currency: String ("INR" | "USD" | "EUR" | "GBP")
│   ├── accentColor: String ("indigo" | "emerald" | "amber" | "rose")
│   ├── language: String ("en")
│   ├── financialMode: String ("BUILD" | "BALANCE" | "SAVE" | "CONTROL")
│   ├── primaryGoal: String ("Save Money" | "Control Spending" | "Build Emergency Fund" | "Buy Something" | "Travel" | "Education")
│   ├── dismissedAlertIds: Array<String>
│   ├── notificationsEnabled: Boolean
│   ├── budgetAlerts: Boolean
│   ├── transactionAlerts: Boolean
│   ├── savingsReminders: Boolean
│   ├── financialInsights: Boolean
│   ├── autoCategorization: Boolean
│   ├── duplicateDetection: Boolean
│   └── importNotifications: Boolean
│
├── importHistory/{importId}
│   ├── fileName: String
│   ├── fileType: String ("CSV" | "XLSX" | "RECEIPT")
│   ├── totalRecords: Int
│   ├── importedCount: Int
│   ├── duplicateCount: Int
│   ├── timestamp: Timestamp
│   └── status: String ("SUCCESS" | "PARTIAL" | "FAILED")
│
└── aiConversations/{msgId}
    ├── sender: String ("user" | "ai")
    ├── text: String
    └── timestamp: Timestamp
```

---

## 5. Authoritative Financial Calculations

### A. Cashflow Metrics
- **Total Income**: `Sum(income.amount)`
- **Total Expenses**: `Sum(expense.amount)`
- **Net Balance / Net Savings**: `Total Income - Total Expenses`
- **Savings Rate**:
  $$\text{Savings Rate} = \begin{cases} 0.0 & \text{if Total Income } \le 0 \\ \max\left(0.0, \frac{\text{Net Savings}}{\text{Total Income}} \times 100\right) & \text{if Total Income } > 0 \end{cases}$$

### B. Budget Status
- **Budget Percentage**: `(spentAmount / budget.amount) * 100` (if `budget.amount > 0` else `0`)
- **Status Classification**:
  - `< 80%`: `Healthy` (Color: `Success #10B981`)
  - `80% – 99%`: `Warning` (Color: `Warning #F59E0B`)
  - `≥ 100%`: `Exceeded` (Color: `Danger #F43F5E`)

### C. Financial Health Score (0–100 Points)
1. **Savings Rate Component (Max 35 pts)**:
   - `Savings Rate ≥ 30%`: 35 pts
   - `Savings Rate ≥ 20%`: 30 pts
   - `Savings Rate ≥ 10%`: 22 pts
   - `Savings Rate > 0%`: 14 pts
   - `Savings Rate ≤ 0%`: 5 pts
2. **Budget Discipline Component (Max 25 pts)**:
   - If no budgets configured: 20 pts baseline.
   - If budgets exist:
     - 0 exceeded & 0 warnings: 25 pts
     - 0 exceeded & warnings exist: 18 pts
     - 1 exceeded: 12 pts
     - ≥ 2 exceeded: 6 pts
3. **Cashflow Stability Component (Max 20 pts)**:
   - `Expense / Income ≤ 0.50`: 20 pts
   - `Expense / Income ≤ 0.70`: 16 pts
   - `Expense / Income ≤ 0.85`: 12 pts
   - `Expense / Income > 0.85`: 6 pts
4. **Goal Progress Component (Max 20 pts)**:
   - If no goals: 15 pts baseline.
   - If goals exist: `Math.round(Average(Goal Progress Ratio) * 20)` clamped to `[5, 20]`.
- **Total Score**: `Clamp(Savings + Budget + Stability + Goals, 10, 100)`
- **Standing Labels**:
  - `≥ 80`: `Excellent Standing` (`Success #10B981`)
  - `70 – 79`: `Healthy Standing` (`Success #10B981`)
  - `50 – 69`: `Moderate Standing` (`Warning #F59E0B`)
  - `< 50`: `Needs Attention` (`Danger #F43F5E`)

### D. Money Habit Score (0–100 Behavioral Points)
1. **Savings Consistency (Max 20 pts)**:
   - `Savings Rate ≥ 20%`: 20 pts
   - `Savings Rate > 5%`: 14 pts
   - `Savings Rate ≤ 5%`: 6 pts
2. **Budget Adherence (Max 20 pts)**:
   - 0 exceeded: 20 pts
   - Each exceeded budget deducts 7 pts (minimum 5 pts).
3. **Micro-Spending Leak Control (Max 15 pts)**:
   - 0 leaks: 15 pts
   - Each detected leak deducts 3 pts (minimum 4 pts).
4. **50/30/20 Alignment (Max 15 pts)**:
   - `Essential ≤ 60%` and `Discretionary ≤ 30%`: 15 pts
   - `Discretionary > 45%`: 7 pts
   - Otherwise: 12 pts
5. **Active Goal Planning (Max 15 pts)**:
   - Goals active: 15 pts; No goals: 10 pts.
6. **Logging Cadence (Max 15 pts)**:
   - `Transactions ≥ 5`: 15 pts; Otherwise: 12 pts.
- **Standing Labels**:
  - `≥ 85`: `EXCELLENT MONEY HABITS`
  - `75 – 84`: `GOOD MONEY HABITS`
  - `60 – 74`: `MODERATE MONEY HABITS`
  - `< 60`: `NEEDS DISCIPLINE`

### E. Essential vs Discretionary Classification
- **Essential Categories**: `Rent`, `Utilities`, `Food`, `Healthcare`, `Education`
- **Discretionary Categories**: `Shopping`, `Entertainment`, `Subscriptions`, `Travel`, `Other`
- Overrides: If `expense.isEssential != null`, respect the explicit boolean.

### F. Spending Leak Detection Algorithm
- Group expenses where `amount < 800`.
- Key: Trimmed, lowercase description or category.
- If count in key $\ge 2$, classify as a Spending Leak.
- Rank by monthly total descending, top 3 displayed.

### G. Purchase Impact Analyzer
- Given amount $A$ and monthly net surplus $S$:
  - If $S \ge A \times 1.5$: `SAFE TO PURCHASE`
  - Else if $S \ge A$: `PURCHASE WITH CAUTION`
  - Else ($S < A$): `NOT RECOMMENDED RIGHT NOW`

### H. Duplicate Detection Algorithm
Two transactions $T_1$ and $T_2$ are duplicates if:
1. `normalizeDate(T1.date) == normalizeDate(T2.date)` (same YYYY-MM-DD)
2. `abs(T1.amount - T2.amount) < 0.01`
3. `T1.type.toLowerCase() == T2.type.toLowerCase()`
4. `T1.description.trim().toLowerCase() == T2.description.trim().toLowerCase()`
5. `T1.paymentMethod.trim().toLowerCase() == T2.paymentMethod.trim().toLowerCase()`

---

## 6. Currency Formatting Rules
- **INR (`₹`)**: `en-IN`, Symbol: `₹`, Format: `₹10,500.00`
- **USD (`$`)**: `en-US`, Symbol: `$`, Format: `$10,500.00`
- **EUR (`€`)**: `de-DE`, Symbol: `€`, Format: `10.500,00 €` / `€10,500.00`
- **GBP (`£`)**: `en-GB`, Symbol: `£`, Format: `£10,500.00`
