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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { MobileBundleRoutingModule } from '@home/pages/mobile/bundes/bundles-routing.module';
import { MobileBundleTableHeaderComponent } from '@home/pages/mobile/bundes/mobile-bundle-table-header.component';
import { MobileBundleDialogComponent } from '@home/pages/mobile/bundes/mobile-bundle-dialog.component';
import { MobileLayoutComponent } from '@home/pages/mobile/bundes/layout/mobile-layout.component';
import { MobilePageItemRowComponent } from '@home/pages/mobile/bundes/layout/mobile-page-item-row.component';
import { AddMobilePageDialogComponent } from '@home/pages/mobile/bundes/layout/add-mobile-page-dialog.component';
import { CustomMobilePageComponent } from '@home/pages/mobile/bundes/layout/custom-mobile-page.component';
import { CustomMobilePagePanelComponent } from '@home/pages/mobile/bundes/layout/custom-mobile-page-panel.component';
import { DefaultMobilePagePanelComponent } from '@home/pages/mobile/bundes/layout/default-mobile-page-panel.component';
import {
  MobileAppConfigurationDialogComponent
} from '@home/pages/mobile/bundes/mobile-app-configuration-dialog.component';

import {
  MobileSelfRegistrationComponent
} from '@home/pages/mobile/bundes/sefl-registration/mobile-self-registration.component';
import {
  MobileRegistrationFieldsPanelComponent
} from '@home/pages/mobile/bundes/sefl-registration/mobile-registration-fields-panel.component';
import { WidgetSettingsCommonModule } from '@home/components/widget/lib/settings/common/widget-settings-common.module';
import {
  MobileRegistrationFieldsRowComponent
} from '@home/pages/mobile/bundes/sefl-registration/mobile-registration-fields-row.component';
import { CommonMobileModule } from '@home/pages/mobile/common/common-mobile.module';

@NgModule({
  declarations: [
    MobileBundleTableHeaderComponent,
    MobileBundleDialogComponent,
    MobileLayoutComponent,
    MobilePageItemRowComponent,
    AddMobilePageDialogComponent,
    CustomMobilePageComponent,
    CustomMobilePagePanelComponent,
    DefaultMobilePagePanelComponent,
    MobileAppConfigurationDialogComponent,
    MobileSelfRegistrationComponent,
    MobileRegistrationFieldsPanelComponent,
    MobileRegistrationFieldsRowComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    MobileBundleRoutingModule,
    WidgetSettingsCommonModule,
    CommonMobileModule,
  ]
})
export class MobileBundlesModule { }
