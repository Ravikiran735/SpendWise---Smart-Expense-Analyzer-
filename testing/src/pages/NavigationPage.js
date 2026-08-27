/**
 * Navigation Page Object Model
 * Handles screen transitions, bottom navigation, drawer, deep links, back button & restart
 */
const BasePage = require('./BasePage');
const logger = require('../utils/logger');

class NavigationPage extends BasePage {
  get tabDashboard() { return '~tab_dashboard, //*[@text="Dashboard" or @content-desc="tab_dashboard"]'; }
  get tabTransactions() { return '~tab_transactions, //*[@text="Transactions" or @content-desc="tab_transactions"]'; }
  get tabBudgets() { return '~tab_budgets, //*[@text="Budgets" or @content-desc="tab_budgets"]'; }
  get tabCopilot() { return '~tab_copilot, //*[@text="Copilot" or @content-desc="tab_copilot"]'; }
  get tabSettings() { return '~tab_settings, //*[@text="Settings" or @content-desc="tab_settings"]'; }
  get drawerToggle() { return '~drawer_toggle, //*[@content-desc="Open navigation drawer"]'; }

  async navigateTo(tabName) {
    logger.info(`NavigationPage: Navigating to tab "${tabName}"`);
    switch (tabName.toLowerCase()) {
      case 'dashboard': await this.click(this.tabDashboard); break;
      case 'transactions': await this.click(this.tabTransactions); break;
      case 'budgets': await this.click(this.tabBudgets); break;
      case 'copilot': await this.click(this.tabCopilot); break;
      case 'settings': await this.click(this.tabSettings); break;
      default: logger.warn(`Unknown navigation destination: ${tabName}`);
    }
  }

  async openDrawer() {
    logger.info('NavigationPage: Opening navigation drawer');
    if (await this.isDisplayed(this.drawerToggle)) {
      await this.click(this.drawerToggle);
    }
  }

  async pressBackButton() {
    logger.info('NavigationPage: Pressing Android Hardware Back Button');
    await this.driver.back();
  }

  async restartApp(appPackage = 'com.spendwise.app', appActivity = 'com.spendwise.app.MainActivity') {
    logger.info(`NavigationPage: Restarting application "${appPackage}"`);
    try {
      await this.driver.terminateApp(appPackage);
      await this.driver.activateApp(appPackage);
    } catch (e) {
      await this.driver.startActivity(appPackage, appActivity);
    }
  }
}

module.exports = NavigationPage;
