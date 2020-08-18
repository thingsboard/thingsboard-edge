///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import * as AngularCore from '@angular/core';
import * as AngularCommon from '@angular/common';
import * as AngularForms from '@angular/forms';
import * as AngularRouter from '@angular/router';
import * as AngularCdkKeycodes from '@angular/cdk/keycodes';
import * as AngularCdkCoercion from '@angular/cdk/coercion';
import * as AngularMaterialChips from '@angular/material/chips';
import * as AngularMaterialAutocomplete from '@angular/material/autocomplete';
import * as AngularMaterialDialog from '@angular/material/dialog';
import * as NgrxStore from '@ngrx/store';
import * as RxJs from 'rxjs';
import * as RxJsOperators from 'rxjs/operators';
import * as TranslateCore from '@ngx-translate/core';
import * as TbCore from '@core/public-api';
import * as TbShared from '@shared/public-api';
import * as TbHomeComponents from '@home/components/public-api';
import * as _moment from 'moment';

declare const SystemJS;

export const modulesMap: {[key: string]: any} = {
  '@angular/core': SystemJS.newModule(AngularCore),
  '@angular/common': SystemJS.newModule(AngularCommon),
  '@angular/forms': SystemJS.newModule(AngularForms),
  '@angular/router': SystemJS.newModule(AngularRouter),
  '@angular/cdk/keycodes': SystemJS.newModule(AngularCdkKeycodes),
  '@angular/cdk/coercion': SystemJS.newModule(AngularCdkCoercion),
  '@angular/material/chips': SystemJS.newModule(AngularMaterialChips),
  '@angular/material/autocomplete': SystemJS.newModule(AngularMaterialAutocomplete),
  '@angular/material/dialog': SystemJS.newModule(AngularMaterialDialog),
  '@ngrx/store': SystemJS.newModule(NgrxStore),
  rxjs: SystemJS.newModule(RxJs),
  'rxjs/operators': SystemJS.newModule(RxJsOperators),
  '@ngx-translate/core': SystemJS.newModule(TranslateCore),
  '@core/public-api': SystemJS.newModule(TbCore),
  '@shared/public-api': SystemJS.newModule(TbShared),
  '@home/components/public-api': SystemJS.newModule(TbHomeComponents),
  moment: SystemJS.newModule(_moment)
};
