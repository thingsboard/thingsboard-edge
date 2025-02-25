///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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


import { isUndefined } from '@core/utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';

export interface ExceptionData {
  message?: string;
  name?: string;
  lineNumber?: number;
  columnNumber?: number;
}


export const parseException = (exception: any, lineOffset?: number): ExceptionData => {
  const data: ExceptionData = {};
  if (exception) {
    if (typeof exception === 'string') {
      data.message = exception;
    } else if (exception instanceof String) {
      data.message = exception.toString();
    } else {
      if (exception.name) {
        data.name = exception.name;
      } else {
        data.name = 'UnknownError';
      }
      if (exception.message) {
        data.message = exception.message;
      }
      if (exception.lineNumber) {
        data.lineNumber = exception.lineNumber;
        if (exception.columnNumber) {
          data.columnNumber = exception.columnNumber;
        }
      } else if (exception.stack) {
        const lineInfoRegexp = /(.*<anonymous>):(\d*)(:)?(\d*)?/g;
        const lineInfoGroups = lineInfoRegexp.exec(exception.stack);
        if (lineInfoGroups != null && lineInfoGroups.length >= 3) {
          if (isUndefined(lineOffset)) {
            lineOffset = -2;
          }
          data.lineNumber = Number(lineInfoGroups[2]) + lineOffset;
          if (lineInfoGroups.length >= 5) {
            data.columnNumber = Number(lineInfoGroups[4]);
          }
        }
      }
    }
  }
  return data;
}

export const parseError = (err: any): string =>
  parseException(err).message || 'Unknown Error';
