/**
 * Base Page Object Model (POM)
 * Provides robust element interactions, explicit waits, and logging
 */
const logger = require('../utils/logger');
const GestureUtils = require('../utils/gestureUtils');

class BasePage {
  constructor(driver) {
    this.driver = driver;
    this.finder = driver?.reactNative;
  }

  async waitForElement(locator, timeoutMs = 10000) {
    logger.info(`Waiting for element: "${locator}" (timeout: ${timeoutMs}ms)`);
    const el = typeof locator === 'string' ? await this.driver.$(locator) : locator;
    await el.waitForDisplayed({ timeout: timeoutMs });
    return el;
  }

  async click(locator) {
    logger.info(`Clicking element: "${locator}"`);
    const el = await this.waitForElement(locator);
    await el.click();
  }

  async sendKeys(locator, text, clearFirst = true) {
    logger.info(`Entering text into "${locator}": "${text}"`);
    const el = await this.waitForElement(locator);
    if (clearFirst) {
      await el.clearValue();
    }
    await el.setValue(text);
  }

  async getText(locator) {
    const el = await this.waitForElement(locator);
    const text = await el.getText();
    logger.info(`Read text from "${locator}": "${text}"`);
    return text;
  }

  async isDisplayed(locator, timeoutMs = 4000) {
    try {
      const el = typeof locator === 'string' ? await this.driver.$(locator) : locator;
      return await el.isDisplayed();
    } catch (e) {
      return false;
    }
  }

  async scrollDown() {
    await GestureUtils.scroll(this.driver, 'down');
  }

  async scrollUp() {
    await GestureUtils.scroll(this.driver, 'up');
  }
}

module.exports = BasePage;
