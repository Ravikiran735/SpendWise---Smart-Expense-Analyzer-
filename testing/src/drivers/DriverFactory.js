/**
 * Enterprise Driver Factory
 * Creates and orchestrates Appium Mobile Drivers and Selenium Web Drivers
 */
const { remote } = require('webdriverio');
const { Builder } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const config = require('../config/env.config');
const logger = require('../utils/logger');
const ReactNativeFinder = require('./ReactNativeDriver');

class DriverFactory {
  /**
   * Initialize Appium Mobile Driver (with UiAutomator2 / React Native / Flutter support)
   */
  static async createMobileDriver(customCapabilities = {}) {
    logger.info('Initializing Appium Mobile Driver session...');
    const caps = {
      platformName: config.mobile.platformName,
      'appium:automationName': config.mobile.automationName,
      'appium:deviceName': config.mobile.deviceName,
      'appium:platformVersion': config.mobile.platformVersion,
      'appium:app': config.mobile.app,
      'appium:appPackage': config.mobile.appPackage,
      'appium:appActivity': config.mobile.appActivity,
      'appium:autoGrantPermissions': config.mobile.autoGrantPermissions,
      'appium:noReset': config.mobile.noReset,
      'appium:fullReset': config.mobile.fullReset,
      'appium:newCommandTimeout': config.mobile.newCommandTimeout,
      ...customCapabilities
    };

    const driver = await remote({
      protocol: 'http',
      hostname: config.mobile.appiumHost,
      port: config.mobile.appiumPort,
      path: '/',
      capabilities: caps,
      logLevel: 'error'
    });

    driver.reactNative = new ReactNativeFinder(driver);
    logger.info(`Appium Driver initialized for package: ${config.mobile.appPackage}`);
    return driver;
  }

  /**
   * Initialize Selenium Web Driver for React JS / Web Apps
   */
  static async createWebDriver() {
    logger.info('Initializing Selenium Web Driver session...');
    const options = new chrome.Options();
    if (config.web.headless) {
      options.addArguments('--headless=new');
    }
    options.addArguments(
      '--no-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
      '--window-size=1920,1080',
      '--disable-extensions'
    );

    const driver = await new Builder()
      .forBrowser(config.web.browser)
      .setChromeOptions(options)
      .build();

    await driver.manage().setTimeouts({
      implicit: config.web.implicitWaitMs,
      pageLoad: config.web.pageLoadTimeoutMs
    });

    logger.info('Selenium Chrome Web Driver initialized.');
    return driver;
  }
}

module.exports = DriverFactory;
