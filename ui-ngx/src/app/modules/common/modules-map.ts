///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import * as AngularAnimations from '@angular/animations';
import * as AngularCore from '@angular/core';
import * as AngularCommon from '@angular/common';
import * as AngularForms from '@angular/forms';
import * as AngularFlexLayout from '@angular/flex-layout';
import * as AngularPlatformBrowser from '@angular/platform-browser';
import * as AngularRouter from '@angular/router';
import * as AngularCdkCoercion from '@angular/cdk/coercion';
import * as AngularCdkCollections from '@angular/cdk/collections';
import * as AngularCdkKeycodes from '@angular/cdk/keycodes';
import * as AngularCdkLayout from '@angular/cdk/layout';
import * as AngularCdkOverlay from '@angular/cdk/overlay';
import * as AngularCdkPortal from '@angular/cdk/portal';
import * as AngularMaterialAutocomplete from '@angular/material/autocomplete';
import * as AngularMaterialBadge from '@angular/material/badge';
import * as AngularMaterialBottomSheet from '@angular/material/bottom-sheet';
import * as AngularMaterialButton from '@angular/material/button';
import * as AngularMaterialButtonToggle from '@angular/material/button-toggle';
import * as AngularMaterialCard from '@angular/material/card';
import * as AngularMaterialCheckbox from '@angular/material/checkbox';
import * as AngularMaterialChips from '@angular/material/chips';
import * as AngularMaterialCore from '@angular/material/core';
import * as AngularMaterialDatepicker from '@angular/material/datepicker';
import * as AngularMaterialDialog from '@angular/material/dialog';
import * as AngularMaterialDivider from '@angular/material/divider';
import * as AngularMaterialExpansion from '@angular/material/expansion';
import * as AngularMaterialFormField from '@angular/material/form-field';
import * as AngularMaterialGridList from '@angular/material/grid-list';
import * as AngularMaterialIcon from '@angular/material/icon';
import * as AngularMaterialInput from '@angular/material/input';
import * as AngularMaterialList from '@angular/material/list';
import * as AngularMaterialMenu from '@angular/material/menu';
import * as AngularMaterialPaginator from '@angular/material/paginator';
import * as AngularMaterialProgressBar from '@angular/material/progress-bar';
import * as AngularMaterialProgressSpinner from '@angular/material/progress-spinner';
import * as AngularMaterialRadio from '@angular/material/radio';
import * as AngularMaterialSelect from '@angular/material/select';
import * as AngularMaterialSidenav from '@angular/material/sidenav';
import * as AngularMaterialSlideToggle from '@angular/material/slide-toggle';
import * as AngularMaterialSlider from '@angular/material/slider';
import * as AngularMaterialSnackBar from '@angular/material/snack-bar';
import * as AngularMaterialSort from '@angular/material/sort';
import * as AngularMaterialStepper from '@angular/material/stepper';
import * as AngularMaterialTable from '@angular/material/table';
import * as AngularMaterialTabs from '@angular/material/tabs';
import * as AngularMaterialToolbar from '@angular/material/toolbar';
import * as AngularMaterialTooltip from '@angular/material/tooltip';
import * as AngularMaterialTree from '@angular/material/tree';
import * as NgrxStore from '@ngrx/store';
import * as RxJs from 'rxjs';
import * as RxJsOperators from 'rxjs/operators';
import * as TranslateCore from '@ngx-translate/core';
import * as TbCore from '@core/public-api';
import * as TbShared from '@shared/public-api';
import * as TbHomeComponents from '@home/components/public-api';
import * as _moment from 'moment';
import * as DragDropModule from "@angular/cdk/drag-drop";
import * as HttpClientModule from "@angular/common/http";

declare const SystemJS;

export const modulesMap: {[key: string]: any} = {
  '@angular/animations': SystemJS.newModule(AngularAnimations),
  '@angular/core': SystemJS.newModule(AngularCore),
  '@angular/common': SystemJS.newModule(AngularCommon),
  '@angular/common/http': SystemJS.newModule(HttpClientModule),
  '@angular/forms': SystemJS.newModule(AngularForms),
  '@angular/flex-layout': SystemJS.newModule(AngularFlexLayout),
  '@angular/platform-browser': SystemJS.newModule(AngularPlatformBrowser),
  '@angular/router': SystemJS.newModule(AngularRouter),
  '@angular/cdk/coercion': SystemJS.newModule(AngularCdkCoercion),
  '@angular/cdk/collections': SystemJS.newModule(AngularCdkCollections),
  '@angular/cdk/keycodes': SystemJS.newModule(AngularCdkKeycodes),
  '@angular/cdk/layout': SystemJS.newModule(AngularCdkLayout),
  '@angular/cdk/overlay': SystemJS.newModule(AngularCdkOverlay),
  '@angular/cdk/portal': SystemJS.newModule(AngularCdkPortal),
  '@angular/cdk/drag-drop': SystemJS.newModule(DragDropModule),
  '@angular/material/autocomplete': SystemJS.newModule(AngularMaterialAutocomplete),
  '@angular/material/badge': SystemJS.newModule(AngularMaterialBadge),
  '@angular/material/bottom-sheet': SystemJS.newModule(AngularMaterialBottomSheet),
  '@angular/material/button': SystemJS.newModule(AngularMaterialButton),
  '@angular/material/button-toggle': SystemJS.newModule(AngularMaterialButtonToggle),
  '@angular/material/card': SystemJS.newModule(AngularMaterialCard),
  '@angular/material/checkbox': SystemJS.newModule(AngularMaterialCheckbox),
  '@angular/material/chips': SystemJS.newModule(AngularMaterialChips),
  '@angular/material/core': SystemJS.newModule(AngularMaterialCore),
  '@angular/material/datepicker': SystemJS.newModule(AngularMaterialDatepicker),
  '@angular/material/dialog': SystemJS.newModule(AngularMaterialDialog),
  '@angular/material/divider': SystemJS.newModule(AngularMaterialDivider),
  '@angular/material/expansion': SystemJS.newModule(AngularMaterialExpansion),
  '@angular/material/form-field': SystemJS.newModule(AngularMaterialFormField),
  '@angular/material/grid-list': SystemJS.newModule(AngularMaterialGridList),
  '@angular/material/icon': SystemJS.newModule(AngularMaterialIcon),
  '@angular/material/input': SystemJS.newModule(AngularMaterialInput),
  '@angular/material/list': SystemJS.newModule(AngularMaterialList),
  '@angular/material/menu': SystemJS.newModule(AngularMaterialMenu),
  '@angular/material/paginator': SystemJS.newModule(AngularMaterialPaginator),
  '@angular/material/progress-bar': SystemJS.newModule(AngularMaterialProgressBar),
  '@angular/material/progress-spinner': SystemJS.newModule(AngularMaterialProgressSpinner),
  '@angular/material/radio': SystemJS.newModule(AngularMaterialRadio),
  '@angular/material/select': SystemJS.newModule(AngularMaterialSelect),
  '@angular/material/sidenav': SystemJS.newModule(AngularMaterialSidenav),
  '@angular/material/slide-toggle': SystemJS.newModule(AngularMaterialSlideToggle),
  '@angular/material/slider': SystemJS.newModule(AngularMaterialSlider),
  '@angular/material/snack-bar': SystemJS.newModule(AngularMaterialSnackBar),
  '@angular/material/sort': SystemJS.newModule(AngularMaterialSort),
  '@angular/material/stepper': SystemJS.newModule(AngularMaterialStepper),
  '@angular/material/table': SystemJS.newModule(AngularMaterialTable),
  '@angular/material/tabs': SystemJS.newModule(AngularMaterialTabs),
  '@angular/material/toolbar': SystemJS.newModule(AngularMaterialToolbar),
  '@angular/material/tooltip': SystemJS.newModule(AngularMaterialTooltip),
  '@angular/material/tree': SystemJS.newModule(AngularMaterialTree),
  '@ngrx/store': SystemJS.newModule(NgrxStore),
  rxjs: SystemJS.newModule(RxJs),
  'rxjs/operators': SystemJS.newModule(RxJsOperators),
  '@ngx-translate/core': SystemJS.newModule(TranslateCore),
  '@core/public-api': SystemJS.newModule(TbCore),
  '@shared/public-api': SystemJS.newModule(TbShared),
  '@home/components/public-api': SystemJS.newModule(TbHomeComponents),
  moment: SystemJS.newModule(_moment)
};
