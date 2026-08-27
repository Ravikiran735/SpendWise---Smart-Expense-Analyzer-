/**
 * React Native & Flutter Widget Finder Driver Extension
 * Supports ValueKey, Semantics Label, Accessibility ID, and Widget Text
 */
const logger = require('../utils/logger');

class ReactNativeFinder {
  constructor(driver) {
    this.driver = driver;
  }

  /**
   * Find by ValueKey or testID (React Native: testID, Flutter: ValueKey)
   */
  async byValueKey(key) {
    logger.info(`Locating widget by ValueKey/testID: "${key}"`);
    try {
      // 1. Try React Native testID / Accessibility ID
      return await this.driver.$(`~${key}`);
    } catch (e) {
      // 2. Fallback to resource-id or tag
      return await this.driver.$(`//*[@resource-id="${key}" or @content-desc="${key}"]`);
    }
  }

  /**
   * Find by Widget Text
   */
  async byText(text, exact = true) {
    logger.info(`Locating widget by Text: "${text}" (exact: ${exact})`);
    if (exact) {
      return await this.driver.$(`//*[@text="${text}" or @content-desc="${text}"]`);
    }
    return await this.driver.$(`//*[contains(@text, "${text}") or contains(@content-desc, "${text}")]`);
  }

  /**
   * Find by Semantics Label or Content Description
   */
  async bySemanticsLabel(label) {
    logger.info(`Locating widget by Semantics Label: "${label}"`);
    return await this.driver.$(`//*[@content-desc="${label}"]`);
  }

  /**
   * Find by Accessibility ID
   */
  async byAccessibilityId(id) {
    logger.info(`Locating widget by Accessibility ID: "${id}"`);
    return await this.driver.$(`~${id}`);
  }

  /**
   * Universal smart element resolver
   */
  async findWidget({ key, text, label, accessibilityId, xpath }) {
    if (key) return await this.byValueKey(key);
    if (label) return await this.bySemanticsLabel(label);
    if (accessibilityId) return await this.byAccessibilityId(accessibilityId);
    if (text) return await this.byText(text);
    if (xpath) return await this.driver.$(xpath);
    throw new Error('No valid locator provided for findWidget()');
  }
}

module.exports = ReactNativeFinder;
