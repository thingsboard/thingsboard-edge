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

import { ActivatedRouteSnapshot, Params } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { HasUUID } from '@shared/models/id/has-uuid';

export interface BreadCrumb extends HasUUID{
  label: string;
  labelFunction?: () => string;
  ignoreTranslate: boolean;
  icon: string;
  iconUrl: string;
  isMdiIcon: boolean;
  link: any[];
  queryParams: Params;
}

export type BreadCrumbLabelFunction<C> = (
  route: ActivatedRouteSnapshot,
  translate: TranslateService,
  component: C,
  data?: any,
  utils?: UtilsService) => string;

export interface BreadCrumbConfig<C> {
  labelFunction: BreadCrumbLabelFunction<C>;
  label: string;
  icon: string;
  skip: boolean;
  custom: boolean;
  customChild: boolean;
}
