/**
 * Winston Logging Utility with Console & File Transports
 */
const winston = require('winston');
const path = require('path');
const fs = require('fs-extra');

const logDir = path.resolve(__dirname, '../../reports/logs');
fs.ensureDirSync(logDir);

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss.SSS' }),
    winston.format.errors({ stack: true }),
    winston.format.splat(),
    winston.format.json()
  ),
  defaultMeta: { service: 'spendwise-test-suite' },
  transports: [
    new winston.transports.File({
      filename: path.join(logDir, 'error.log'),
      level: 'error'
    }),
    new winston.transports.File({
      filename: path.join(logDir, 'combined.log')
    }),
    new winston.transports.Console({
      format: winston.format.combine(
        winston.format.colorize(),
        winston.format.printf(({ timestamp, level, message, meta }) => {
          return `[${timestamp}] [${level}]: ${message} ${meta ? JSON.stringify(meta) : ''}`;
        })
      )
    })
  ]
});

module.exports = logger;
