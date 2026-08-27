/**
 * UI Components Page Object Model
 * Covers ElevatedButton, TextButton, IconButton, TextField, Dropdowns, Checkbox, Radio, Switch,
 * Dialog, BottomSheet, Snackbar, ListView, GridView, Card, TabBar, and Navigation Drawer
 */
const BasePage = require('./BasePage');
const logger = require('../utils/logger');

class UIComponentsPage extends BasePage {
  // Widget Locators
  get elevatedButton() { return '~elevated_button, //*[@content-desc="primary_action_btn"]'; }
  get textButton() { return '~text_button, //*[@content-desc="text_action_btn"]'; }
  get iconButton() { return '~icon_button, //*[@content-desc="icon_action_btn"]'; }
  get textField() { return '~input_text_field, //*[@content-desc="sample_text_field"]'; }
  get dropdownButton() { return '~dropdown_selector, //*[@content-desc="category_dropdown"]'; }
  get checkbox() { return '~checkbox_widget, //*[@content-desc="terms_checkbox"]'; }
  get radioButton() { return '~radio_option, //*[@content-desc="payment_radio_upi"]'; }
  get switchWidget() { return '~switch_toggle, //*[@content-desc="dark_mode_switch"]'; }
  get dialogContainer() { return '~modal_dialog, //*[@content-desc="confirmation_dialog"]'; }
  get bottomSheet() { return '~bottom_sheet_modal, //*[@content-desc="action_bottom_sheet"]'; }
  get snackbarToast() { return '~snackbar_container, //*[@content-desc="toast_notification"]'; }
  get listView() { return '~scrollable_list_view, //*[@content-desc="transactions_list"]'; }
  get gridView() { return '~grid_view_container, //*[@content-desc="analytics_grid"]'; }
  get card() { return '~metric_card, //*[@content-desc="net_balance_card"]'; }
  get tabBar() { return '~tab_bar_navigation, //*[@content-desc="main_tab_bar"]'; }
  get navDrawer() { return '~navigation_drawer, //*[@content-desc="side_nav_drawer"]'; }

  async interactWithButtons() {
    logger.info('UIComponentsPage: Interacting with Button components');
    if (await this.isDisplayed(this.elevatedButton)) await this.click(this.elevatedButton);
    if (await this.isDisplayed(this.textButton)) await this.click(this.textButton);
    if (await this.isDisplayed(this.iconButton)) await this.click(this.iconButton);
  }

  async interactWithFormControls(textVal = 'Test Input') {
    logger.info('UIComponentsPage: Interacting with Input and Selection controls');
    if (await this.isDisplayed(this.textField)) await this.sendKeys(this.textField, textVal);
    if (await this.isDisplayed(this.checkbox)) await this.click(this.checkbox);
    if (await this.isDisplayed(this.radioButton)) await this.click(this.radioButton);
    if (await this.isDisplayed(this.switchWidget)) await this.click(this.switchWidget);
  }

  async verifyDialogAndBottomSheet() {
    logger.info('UIComponentsPage: Verifying Dialogs and BottomSheets');
    return {
      hasDialog: await this.isDisplayed(this.dialogContainer),
      hasBottomSheet: await this.isDisplayed(this.bottomSheet),
      hasSnackbar: await this.isDisplayed(this.snackbarToast)
    };
  }
}

module.exports = UIComponentsPage;
