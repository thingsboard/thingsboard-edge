///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///


import config from 'config';
import path from 'path';
import DailyRotateFile from 'winston-daily-rotate-file';
import { createLogger, format, transports }  from 'winston';
import * as Transport from 'winston-transport';
const { combine, timestamp, label, printf, splat } = format;

const loggerTransports: Array<Transport> = [];

if (process.env.NODE_ENV !== 'production' || process.env.DOCKER_MODE === 'true') {
    loggerTransports.push(new transports.Console({
        handleExceptions: true
    }));
} else {
    const filename = path.join(config.get('logger.path'), config.get('logger.filename'));
    const transport = new (DailyRotateFile)({
        filename: filename,
        datePattern: 'YYYY-MM-DD-HH',
        zippedArchive: true,
        maxSize: '20m',
        maxFiles: '14d',
        handleExceptions: true
    });
    loggerTransports.push(transport);
}

const tbFormat = printf(info => {
    let logMessage = `${info.timestamp} [${info.label}] ${info.level.toUpperCase()}: ${info.message}`;
    if (info.stack) {
        logMessage += ':\n' + info.stack;
    }
    return logMessage;
});

export function _logger(moduleLabel: string) {
    return createLogger({
        level: config.get('logger.level'),
        format:combine(
            splat(),
            label({ label: moduleLabel }),
            timestamp({format: 'YYYY-MM-DD HH:mm:ss,SSS'}),
            tbFormat
        ),
        transports: loggerTransports
    });
}
