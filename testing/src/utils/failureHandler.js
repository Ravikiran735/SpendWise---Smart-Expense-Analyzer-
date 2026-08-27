/**
 * Failure Handler Utility
 * Captures screenshot, device logs, widget tree, screen details & stack trace upon any test failure.
 */
const fs = require('fs-extra');
const path = require('path');
const logger = require('./logger');

class FailureHandler {
  static async handleFailure(driver, testTitle, error) {
    const failuresDir = path.resolve(__dirname, '../../reports/failures');
    const screenshotsDir = path.join(failuresDir, 'screenshots');
    const logsDir = path.join(failuresDir, 'device_logs');
    const widgetTreesDir = path.join(failuresDir, 'widget_trees');

    fs.ensureDirSync(screenshotsDir);
    fs.ensureDirSync(logsDir);
    fs.ensureDirSync(widgetTreesDir);

    const safeTitle = testTitle.replace(/[^a-zA-Z0-9_-]/g, '_');
    const timestamp = Date.now();
    const screenshotFile = path.join(screenshotsDir, `${safeTitle}_${timestamp}.png`);
    const logFile = path.join(logsDir, `${safeTitle}_${timestamp}.log`);
    const widgetTreeFile = path.join(widgetTreesDir, `${safeTitle}_${timestamp}.xml`);

    logger.error(`Test Failed: "${testTitle}". Gathering failure artifacts...`, { error: error?.message });

    // 1. Capture Screenshot
    try {
      if (driver && typeof driver.saveScreenshot === 'function') {
        await driver.saveScreenshot(screenshotFile);
        logger.info(`Screenshot captured: ${screenshotFile}`);
      } else if (driver && typeof driver.takeScreenshot === 'function') {
        const base64Data = await driver.takeScreenshot();
        fs.writeFileSync(screenshotFile, base64Data, 'base64');
        logger.info(`Screenshot captured: ${screenshotFile}`);
      }
    } catch (ssErr) {
      logger.warn(`Could not capture screenshot: ${ssErr.message}`);
    }

    // 2. Capture Device / Browser Logs
    try {
      if (driver && typeof driver.getLogs === 'function') {
        const logs = await driver.getLogs('logcat');
        fs.writeJsonSync(logFile, logs, { spaces: 2 });
      } else {
        fs.writeFileSync(logFile, `Failure Timestamp: ${new Date().toISOString()}\nError: ${error?.stack || error?.message}\n`);
      }
    } catch (logErr) {
      fs.writeFileSync(logFile, `Failure Stack:\n${error?.stack || error?.message}`);
    }

    // 3. Capture Widget Tree / Page Source
    try {
      if (driver && typeof driver.getPageSource === 'function') {
        const source = await driver.getPageSource();
        fs.writeFileSync(widgetTreeFile, source, 'utf8');
        logger.info(`Widget Tree saved: ${widgetTreeFile}`);
      }
    } catch (treeErr) {
      logger.warn(`Could not capture widget tree: ${treeErr.message}`);
    }

    return {
      screenshotPath: screenshotFile,
      logPath: logFile,
      widgetTreePath: widgetTreeFile,
      errorStack: error?.stack || error?.message
    };
  }
}

module.exports = FailureHandler;
