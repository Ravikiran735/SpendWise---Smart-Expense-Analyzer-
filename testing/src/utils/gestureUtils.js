/**
 * Mobile Gesture Utilities
 * Reusable touch actions: Tap, Double Tap, Long Press, Scroll, Swipe, Drag & Drop, Pinch, Zoom
 */
const logger = require('./logger');

class GestureUtils {
  /**
   * Single Tap on element or coordinates
   */
  static async tap(driver, elementOrSelector) {
    logger.info('Performing Gesture: TAP');
    if (typeof elementOrSelector === 'string') {
      const el = await driver.$(elementOrSelector);
      await el.click();
    } else if (elementOrSelector && typeof elementOrSelector.click === 'function') {
      await elementOrSelector.click();
    } else {
      await driver.touchAction({ action: 'tap', x: elementOrSelector.x, y: elementOrSelector.y });
    }
  }

  /**
   * Double Tap
   */
  static async doubleTap(driver, element) {
    logger.info('Performing Gesture: DOUBLE TAP');
    const el = typeof element === 'string' ? await driver.$(element) : element;
    const location = await el.getLocation();
    const size = await el.getSize();
    const x = location.x + size.width / 2;
    const y = location.y + size.height / 2;

    await driver.touchAction([
      { action: 'tap', x, y },
      { action: 'wait', ms: 100 },
      { action: 'tap', x, y }
    ]);
  }

  /**
   * Long Press (duration ms)
   */
  static async longPress(driver, element, durationMs = 2000) {
    logger.info(`Performing Gesture: LONG PRESS (${durationMs}ms)`);
    const el = typeof element === 'string' ? await driver.$(element) : element;
    const location = await el.getLocation();
    const size = await el.getSize();
    const x = location.x + size.width / 2;
    const y = location.y + size.height / 2;

    await driver.touchAction([
      { action: 'press', x, y },
      { action: 'wait', ms: durationMs },
      { action: 'release' }
    ]);
  }

  /**
   * Scroll down or up by percentage
   */
  static async scroll(driver, direction = 'down', distanceRatio = 0.5) {
    logger.info(`Performing Gesture: SCROLL (${direction})`);
    const windowSize = await driver.getWindowRect();
    const startX = windowSize.width / 2;
    const startY = direction === 'down' ? windowSize.height * 0.75 : windowSize.height * 0.25;
    const endY = direction === 'down'
      ? startY - windowSize.height * distanceRatio
      : startY + windowSize.height * distanceRatio;

    await driver.touchAction([
      { action: 'press', x: startX, y: startY },
      { action: 'wait', ms: 500 },
      { action: 'moveTo', x: startX, y: endY },
      { action: 'release' }
    ]);
  }

  /**
   * Swipe in cardinal direction (left, right, up, down)
   */
  static async swipe(driver, direction = 'left') {
    logger.info(`Performing Gesture: SWIPE (${direction})`);
    const windowSize = await driver.getWindowRect();
    let startX = windowSize.width * 0.8;
    let endX = windowSize.width * 0.2;
    let startY = windowSize.height / 2;
    let endY = windowSize.height / 2;

    if (direction === 'right') {
      startX = windowSize.width * 0.2;
      endX = windowSize.width * 0.8;
    } else if (direction === 'up') {
      startX = windowSize.width / 2;
      endX = windowSize.width / 2;
      startY = windowSize.height * 0.8;
      endY = windowSize.height * 0.2;
    } else if (direction === 'down') {
      startX = windowSize.width / 2;
      endX = windowSize.width / 2;
      startY = windowSize.height * 0.2;
      endY = windowSize.height * 0.8;
    }

    await driver.touchAction([
      { action: 'press', x: startX, y: startY },
      { action: 'wait', ms: 250 },
      { action: 'moveTo', x: endX, y: endY },
      { action: 'release' }
    ]);
  }

  /**
   * Drag and drop from source to target
   */
  static async dragAndDrop(driver, sourceElement, targetElement) {
    logger.info('Performing Gesture: DRAG AND DROP');
    const srcEl = typeof sourceElement === 'string' ? await driver.$(sourceElement) : sourceElement;
    const dstEl = typeof targetElement === 'string' ? await driver.$(targetElement) : targetElement;

    const srcLoc = await srcEl.getLocation();
    const dstLoc = await dstEl.getLocation();

    await driver.touchAction([
      { action: 'press', x: srcLoc.x, y: srcLoc.y },
      { action: 'wait', ms: 600 },
      { action: 'moveTo', x: dstLoc.x, y: dstLoc.y },
      { action: 'release' }
    ]);
  }

  /**
   * Pinch (Zoom out)
   */
  static async pinch(driver) {
    logger.info('Performing Gesture: PINCH (ZOOM OUT)');
    const window = await driver.getWindowRect();
    const midX = window.width / 2;
    const midY = window.height / 2;

    const finger1 = [
      { action: 'press', x: midX - 150, y: midY - 150 },
      { action: 'moveTo', x: midX - 20, y: midY - 20 },
      { action: 'release' }
    ];
    const finger2 = [
      { action: 'press', x: midX + 150, y: midY + 150 },
      { action: 'moveTo', x: midX + 20, y: midY + 20 },
      { action: 'release' }
    ];

    await driver.touchPerform([
      { action: 'press', options: finger1[0] },
      { action: 'press', options: finger2[0] }
    ]);
  }

  /**
   * Zoom (Pinch in to out)
   */
  static async zoom(driver) {
    logger.info('Performing Gesture: ZOOM (EXPAND)');
    const window = await driver.getWindowRect();
    const midX = window.width / 2;
    const midY = window.height / 2;

    const finger1 = [
      { action: 'press', x: midX - 30, y: midY - 30 },
      { action: 'moveTo', x: midX - 160, y: midY - 160 },
      { action: 'release' }
    ];
    const finger2 = [
      { action: 'press', x: midX + 30, y: midY + 30 },
      { action: 'moveTo', x: midX + 160, y: midY + 160 },
      { action: 'release' }
    ];

    await driver.touchPerform([
      { action: 'press', options: finger1[0] },
      { action: 'press', options: finger2[0] }
    ]);
  }
}

module.exports = GestureUtils;
