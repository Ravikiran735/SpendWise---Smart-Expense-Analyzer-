/**
 * Environment & Application Configuration
 */
require('dotenv').config();
const path = require('path');

module.exports = {
  // Mobile / APK Settings
  mobile: {
    platformName: 'Android',
    automationName: process.env.APPIUM_AUTOMATION_NAME || 'UiAutomator2', // fallback or 'Flutter'/'ReactNative'
    deviceName: process.env.DEVICE_NAME || 'emulator-5554',
    platformVersion: process.env.PLATFORM_VERSION || '13.0',
    app: process.env.APK_PATH || path.resolve(__dirname, '../../../app/build/outputs/apk/debug/app-debug.apk'),
    appPackage: process.env.APP_PACKAGE || 'com.spendwise.app',
    appActivity: process.env.APP_ACTIVITY || 'com.spendwise.app.MainActivity',
    noReset: false,
    fullReset: false,
    autoGrantPermissions: true,
    newCommandTimeout: 300,
    appiumHost: process.env.APPIUM_HOST || '127.0.0.1',
    appiumPort: parseInt(process.env.APPIUM_PORT, 10) || 4723
  },

  // Web / Selenium Settings
  web: {
    baseUrl: process.env.WEB_BASE_URL || 'http://localhost:5000',
    browser: process.env.BROWSER || 'chrome',
    headless: process.env.HEADLESS !== 'false',
    implicitWaitMs: 10000,
    pageLoadTimeoutMs: 30000
  },

  // Reporting Paths
  reports: {
    outputDir: path.resolve(__dirname, '../../reports'),
    failuresDir: path.resolve(__dirname, '../../reports/failures'),
    screenshotsDir: path.resolve(__dirname, '../../reports/failures/screenshots'),
    excelReportName: 'React native_E2E_Report.xlsx',
    webExcelReportName: 'Selenium_Web_Report.xlsx',
    securityExcelReportName: 'Security_Vulnerability_Report.xlsx',
    loadReportName: 'LoadTest_Report.html'
  }
};
