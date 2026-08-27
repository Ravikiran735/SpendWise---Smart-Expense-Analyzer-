# SpendWise: Smart Expense Analyzer

## Overview
SpendWise is a full-featured Android application for personal expense tracking and spending analysis. It helps users track, analyze, and improve their spending behavior.

## Tech Stack
- **Kotlin**: Language
- **Jetpack Compose**: UI Framework
- **Firebase**: Backend (Auth, Firestore, Storage)
- **MVVM + Clean Architecture**: Structure
- **Vico**: Charts

## Project Structure
- `data/`: Repositories and Firestore implementations
- `domain/`: Business logic, models, and repository interfaces
- `presentation/`: Compose UI, ViewModels, and Navigation
- `utils/`: Analysis engine, PDF generation, OCR, and voice entry

## Setup Instructions
1. **Firebase Project**:
   - Create a project at [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with package name `com.spendwise.app`.
   - Download `google-services.json` and place it in the `app/` directory.
2. **Enable Services**:
   - Authentication (Email/Password)
   - Cloud Firestore (Test mode or apply rules from `firestore.rules`)
3. **Build**:
   - Open the project in Android Studio.
   - Sync Gradle.
   - Run on an emulator or device.

## Features
- **Auth**: Secure login/register with Firebase.
- **Dashboard**: Real-time financial summary and charts.
- **Expenses/Income**: Add, edit, and categorize transactions.
- **Budgets**: Set category-wise limits.
- **Smart Insights**: Rule-based analysis of spending habits.
- **OCR**: Scan receipts using ML Kit.
- **Voice Entry**: Log expenses via speech.
- **Reports**: Export financial reports to PDF.

## Firestore Rules
Apply the rules found in `firestore.rules` to ensure user data privacy.

## Running Tests
Unit tests for the `AnalysisEngine` can be found in `src/test/java`.
