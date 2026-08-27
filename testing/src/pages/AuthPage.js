/**
 * Authentication Page Object Model
 * Handles Login, Register, Logout, Form Validations, Error Messages, and Session Persistence
 */
const BasePage = require('./BasePage');
const logger = require('../utils/logger');

class AuthPage extends BasePage {
  // Locators supporting ValueKey, Semantics, Text & Accessibility ID
  get emailInput() { return '~email_input, //*[@content-desc="email_field" or @resource-id="login-email"]'; }
  get passwordInput() { return '~password_input, //*[@content-desc="password_field" or @resource-id="login-password"]'; }
  get nameInput() { return '~name_input, //*[@content-desc="name_field" or @resource-id="reg-name"]'; }
  get loginButton() { return '~login_button, //*[@text="Sign In" or @content-desc="login_button"]'; }
  get registerButton() { return '~register_button, //*[@text="Create Account" or @content-desc="register_button"]'; }
  get logoutButton() { return '~logout_button, //*[@text="Logout" or @content-desc="logout_button"]'; }
  get errorMessage() { return '~error_message, //*[@resource-id="login-error-msg" or contains(@content-desc, "error")]'; }
  get userProfileHeader() { return '~user_profile_header, //*[@content-desc="dashboard_header" or @text="Dashboard"]'; }

  async login(email, password) {
    logger.info(`AuthPage: Logging in with email "${email}"`);
    if (email) {
      await this.sendKeys(this.emailInput, email);
    }
    if (password) {
      await this.sendKeys(this.passwordInput, password);
    }
    await this.click(this.loginButton);
  }

  async register(name, email, password, confirmPassword) {
    logger.info(`AuthPage: Registering user "${name}" <${email}>`);
    if (name) await this.sendKeys(this.nameInput, name);
    if (email) await this.sendKeys(this.emailInput, email);
    if (password) await this.sendKeys(this.passwordInput, password);
    if (confirmPassword) await this.sendKeys('~confirm_password_input', confirmPassword);
    await this.click(this.registerButton);
  }

  async logout() {
    logger.info('AuthPage: Triggering Logout flow');
    await this.click(this.logoutButton);
  }

  async getErrorMessageText() {
    return await this.getText(this.errorMessage);
  }

  async isDashboardVisible() {
    return await this.isDisplayed(this.userProfileHeader);
  }
}

module.exports = AuthPage;
