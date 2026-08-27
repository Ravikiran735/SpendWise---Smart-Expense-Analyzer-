# SpendWise — Enterprise QA & Automated Testing Framework

This directory contains the production-grade, multi-tier test automation framework for **SpendWise** across Mobile (Appium / React Native / Flutter / Android), Web (Selenium / React JS), Load & Performance, and Vulnerability & Security audits.

---

## 🎯 Architecture Overview

```
                      ┌──────────────────────────────────────────────┐
                      │ SpendWise Master Testing & CI/CD Hub        │
                      └───────┬──────────────┬──────────────┬────────┘
                              │              │              │
        ┌─────────────────────┼──────────────┼──────────────┼─────────────────────┐
        ▼                     ▼              ▼              ▼                     ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│  1. Appium E2E   │ │ 2. Selenium Web  │ │ 3. Load Testing  │ │ 4. Vulnerability │ │ 5. Smart AI      │
│  (300 Reports)   │ │  (300 Reports)   │ │  (300 Scenarios) │ │  (300 Checks)    │ │ Widget Discovery │
└────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
         │                    │                    │                    │                    │
         └────────────────────┴──────────────┬─────┴────────────────────┴────────────────────┘
                                             ▼
                      ┌──────────────────────────────────────────────┐
                      │            Artifacts & Reporting             │
                      │ • React native_E2E_Report.xlsx (4 Sheets)    │
                      │ • Selenium_Web_Report.xlsx                   │
                      │ • Security_Vulnerability_Report.xlsx         │
                      │ • LoadTest_Report.html & .json               │
                      │ • Mochawesome HTML Reports                   │
                      │ • reports/failures/ (Screenshots, Logs, XML) │
                      │ • reports/index.html (Master KPI Dashboard)  │
                      └──────────────────────────────────────────────┘
```

---

## 📊 Test Suites Summary (1,200 Total Tests & Artifacts)

| Suite | Technology | Target Coverage | Key Output Artifact |
| :--- | :--- | :--- | :--- |
| **1. Mobile Appium** | Appium 2.x, Mocha, Chai, UiAutomator2, React Native Driver | Auth, Form Validation, Gestures, UI Components, Lifecycle | `React native_E2E_Report.xlsx`, `failures/screenshots/` |
| **2. Web Selenium** | Selenium WebDriver, Chrome Headless, Mocha | React / Vanilla JS SPA, Modals, Forms, PDF Export | `Selenium_Web_Report.xlsx`, `mochawesome/` |
| **3. Load Testing** | Multi-Virtual User Engine | 300 Requests, Throughput, p50/p95/p99 latency | `LoadTest_Report.html`, `LoadTest_Report.json` |
| **4. Vulnerability Scan** | OWASP Top 10 SAST/DAST Rule Engine | Secret Leaks, Injection, Firestore Security Rules | `Security_Vulnerability_Report.xlsx` |

---

## 🚀 Getting Started Locally

### 1. Installation
Navigate to the `testing/` directory and install dependencies:
```bash
cd testing
npm install
```

### 2. Running Individual Suites

#### **Run Mobile Appium Tests (with 300 AI Smart Discovery Matrix)**
```bash
npm run test:ai-smart
```

#### **Run Web Selenium Tests (300 Scenarios)**
```bash
npm run test:web
```

#### **Run Load Testing Suite (300 Scenarios)**
```bash
npm run test:load
```

#### **Run Vulnerability & Security Scanner (300 Rules)**
```bash
npm run test:security
```

#### **Run All Suites & Generate Master Dashboard**
```bash
npm run test:all
npm run generate:reports
```

---

## 📁 Artifacts & Reports Directory Structure

```
testing/reports/
├── React native_E2E_Report.xlsx    # 4-Sheet Excel (Summary, Test Cases, Failed Tests, Logs)
├── Selenium_Web_Report.xlsx        # 300 Web DOM verification results
├── Security_Vulnerability_Report.xlsx # 300 OWASP Security Audit results
├── LoadTest_Report.html            # Visual latency and throughput distribution
├── LoadTest_Report.json            # Machine-readable performance metrics
├── index.html                      # Consolidated KPI & Quality Gate dashboard
├── mochawesome/                    # Interactive HTML test execution reports
└── failures/                       # Captured on any assertion failure
    ├── screenshots/                # Device & browser failure screenshots (.png)
    ├── device_logs/                # Logcat and console error logs (.log)
    └── widget_trees/               # React Native / Flutter XML widget hierarchy
```

---

## ⚙️ GitHub Actions CI/CD Integration

Two production workflows are available under `.github/workflows/`:
1. [`.github/workflows/React native-appium.yml`](file:///d:/PDD/.github/workflows/React%20native-appium.yml) — Dedicated Android Emulator + Appium test runner.
2. [`.github/workflows/ci-full-test-suite.yml`](file:///d:/PDD/.github/workflows/ci-full-test-suite.yml) — Consolidated 5-job pipeline generating all 1,200 reports and uploading artifacts to GitHub Actions for each commit/PR.
