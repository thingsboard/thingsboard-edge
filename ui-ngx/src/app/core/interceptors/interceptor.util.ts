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

import { HttpRequest } from '@angular/common/http';
import { InterceptorConfig } from '@core/interceptors/interceptor-config';
import { InterceptorHttpParams } from '@core/interceptors/interceptor-http-params';

const internalUrlPrefixes = [
  '/api/auth/token',
  '/api/rpc'
];

export const getInterceptorConfig = (req: HttpRequest<unknown>): InterceptorConfig => {
  let config: InterceptorConfig;
  if (req.params && req.params instanceof InterceptorHttpParams) {
    config = (req.params as InterceptorHttpParams).interceptorConfig;
  } else {
    config = new InterceptorConfig();
  }
  if (isInternalUrlPrefix(req.url)) {
    config.ignoreLoading = true;
  }
  return config;
};

const isInternalUrlPrefix = (url: string): boolean => {
  for (const prefix of internalUrlPrefixes) {
    if (url.startsWith(prefix)) {
      return true;
    }
  }
  return false;
};
