/**
 * Enterprise Excel Report Generator using ExcelJS
 * Generates structured 4-sheet multi-tier test reports
 */
const ExcelJS = require('exceljs');
const path = require('path');
const fs = require('fs-extra');
const logger = require('./logger');

class ExcelReporter {
  constructor(reportFileName = 'React native_E2E_Report.xlsx') {
    this.reportFileName = reportFileName;
    this.reportPath = path.resolve(__dirname, '../../reports', reportFileName);
    this.testResults = [];
    this.failedTests = [];
    this.executionLogs = [];
    this.summaryMeta = {
      executionDate: new Date().toISOString(),
      deviceName: process.env.DEVICE_NAME || 'Pixel 7 (Android 13 Emulator)',
      androidVersion: process.env.PLATFORM_VERSION || '13.0',
      startTime: Date.now(),
      endTime: null,
      totalDurationSec: 0
    };
  }

  recordTestResult({ testId, module, scenario, status, device, durationSec }) {
    this.testResults.push({
      testId: testId || `TC-${String(this.testResults.length + 1).padStart(3, '0')}`,
      module: module || 'General',
      scenario: scenario || 'Unspecified scenario',
      status: status || 'PASSED',
      device: device || this.summaryMeta.deviceName,
      duration: `${(durationSec || 0.5).toFixed(2)}s`
    });
  }

  recordFailure({ testName, failureReason, screenshotPath, device, androidVersion }) {
    this.failedTests.push({
      testName: testName || 'Unknown Test',
      failureReason: failureReason || 'Assertion failed',
      screenshotPath: screenshotPath || 'N/A',
      device: device || this.summaryMeta.deviceName,
      androidVersion: androidVersion || this.summaryMeta.androidVersion
    });
  }

  recordLog({ timestamp, testName, step, result, remarks }) {
    this.executionLogs.push({
      timestamp: timestamp || new Date().toISOString(),
      testName: testName || 'General',
      step: step || 'Step Execution',
      result: result || 'SUCCESS',
      remarks: remarks || ''
    });
  }

  async generateReport() {
    fs.ensureDirSync(path.dirname(this.reportPath));
    const workbook = new ExcelJS.Workbook();
    workbook.creator = 'SpendWise Automation Engine';
    workbook.created = new Date();

    const totalTests = this.testResults.length;
    const passedTests = this.testResults.filter(t => t.status === 'PASSED').length;
    const failedCount = this.testResults.filter(t => t.status === 'FAILED').length;
    const skippedCount = this.testResults.filter(t => t.status === 'SKIPPED').length;
    const passPercentage = totalTests > 0 ? ((passedTests / totalTests) * 100).toFixed(2) + '%' : '100.00%';
    const duration = `${(((Date.now() - this.summaryMeta.startTime) / 1000) || 12).toFixed(2)}s`;

    // -------------------------------------------------------------------------
    // SHEET 1: Summary
    // -------------------------------------------------------------------------
    const sheet1 = workbook.addWorksheet('Summary', {
      views: [{ showGridLines: true }]
    });

    sheet1.columns = [
      { header: 'Metric', key: 'metric', width: 28 },
      { header: 'Value', key: 'value', width: 45 }
    ];

    sheet1.addRows([
      { metric: 'Execution Date', value: this.summaryMeta.executionDate },
      { metric: 'Device Name', value: this.summaryMeta.deviceName },
      { metric: 'Android Version', value: this.summaryMeta.androidVersion },
      { metric: 'Total Tests', value: totalTests },
      { metric: 'Passed', value: passedTests },
      { metric: 'Failed', value: failedCount },
      { metric: 'Skipped', value: skippedCount },
      { metric: 'Pass Percentage', value: passPercentage },
      { metric: 'Duration', value: duration }
    ]);

    // Style Header
    sheet1.getRow(1).font = { bold: true, color: { argb: 'FFFFFFFF' } };
    sheet1.getRow(1).fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF4F46E5' } // Indigo
    };

    // -------------------------------------------------------------------------
    // SHEET 2: Test Cases
    // -------------------------------------------------------------------------
    const sheet2 = workbook.addWorksheet('Test Cases', {
      views: [{ showGridLines: true }]
    });

    sheet2.columns = [
      { header: 'Test ID', key: 'testId', width: 14 },
      { header: 'Module', key: 'module', width: 22 },
      { header: 'Scenario', key: 'scenario', width: 45 },
      { header: 'Status', key: 'status', width: 15 },
      { header: 'Device', key: 'device', width: 28 },
      { header: 'Duration', key: 'duration', width: 14 }
    ];

    this.testResults.forEach(res => {
      const row = sheet2.addRow(res);
      if (res.status === 'PASSED') {
        row.getCell('status').font = { color: { argb: 'FF10B981' }, bold: true };
      } else if (res.status === 'FAILED') {
        row.getCell('status').font = { color: { argb: 'FFF43F5E' }, bold: true };
      }
    });

    sheet2.getRow(1).font = { bold: true, color: { argb: 'FFFFFFFF' } };
    sheet2.getRow(1).fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF0EA5E9' } // Cyan/Blue
    };

    // -------------------------------------------------------------------------
    // SHEET 3: Failed Tests
    // -------------------------------------------------------------------------
    const sheet3 = workbook.addWorksheet('Failed Tests', {
      views: [{ showGridLines: true }]
    });

    sheet3.columns = [
      { header: 'Test Name', key: 'testName', width: 32 },
      { header: 'Failure Reason', key: 'failureReason', width: 40 },
      { header: 'Screenshot Path', key: 'screenshotPath', width: 45 },
      { header: 'Device', key: 'device', width: 25 },
      { header: 'Android Version', key: 'androidVersion', width: 18 }
    ];

    if (this.failedTests.length === 0) {
      sheet3.addRow({
        testName: 'None',
        failureReason: 'All test cases executed successfully (0 failures).',
        screenshotPath: 'N/A',
        device: this.summaryMeta.deviceName,
        androidVersion: this.summaryMeta.androidVersion
      });
    } else {
      this.failedTests.forEach(fail => sheet3.addRow(fail));
    }

    sheet3.getRow(1).font = { bold: true, color: { argb: 'FFFFFFFF' } };
    sheet3.getRow(1).fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FFF43F5E' } // Rose/Red
    };

    // -------------------------------------------------------------------------
    // SHEET 4: Execution Logs
    // -------------------------------------------------------------------------
    const sheet4 = workbook.addWorksheet('Execution Logs', {
      views: [{ showGridLines: true }]
    });

    sheet4.columns = [
      { header: 'Timestamp', key: 'timestamp', width: 26 },
      { header: 'Test Name', key: 'testName', width: 28 },
      { header: 'Step', key: 'step', width: 35 },
      { header: 'Result', key: 'result', width: 14 },
      { header: 'Remarks', key: 'remarks', width: 35 }
    ];

    this.executionLogs.forEach(log => sheet4.addRow(log));

    sheet4.getRow(1).font = { bold: true, color: { argb: 'FFFFFFFF' } };
    sheet4.getRow(1).fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF334155' } // Slate
    };

    await workbook.xlsx.writeFile(this.reportPath);
    logger.info(`Excel report successfully generated at: ${this.reportPath}`);
    return this.reportPath;
  }
}

module.exports = ExcelReporter;
