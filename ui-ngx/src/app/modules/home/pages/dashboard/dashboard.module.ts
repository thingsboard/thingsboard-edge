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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { DashboardFormComponent } from '@modules/home/pages/dashboard/dashboard-form.component';
import { ManageDashboardCustomersDialogComponent } from '@modules/home/pages/dashboard/manage-dashboard-customers-dialog.component';
import { DashboardRoutingModule } from './dashboard-routing.module';
import { MakeDashboardPublicDialogComponent } from '@modules/home/pages/dashboard/make-dashboard-public-dialog.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { DashboardTabsComponent } from '@home/pages/dashboard/dashboard-tabs.component';
import { DashboardPageComponent } from '@home/pages/dashboard/dashboard-page.component';
import { DashboardToolbarComponent } from './dashboard-toolbar.component';
import { StatesControllerModule } from '@home/pages/dashboard/states/states-controller.module';
import { DashboardLayoutComponent } from './layout/dashboard-layout.component';
import { EditWidgetComponent } from './edit-widget.component';
import { DashboardWidgetSelectComponent } from './dashboard-widget-select.component';
import { AddWidgetDialogComponent } from './add-widget-dialog.component';
import { ManageDashboardLayoutsDialogComponent } from './layout/manage-dashboard-layouts-dialog.component';
import { DashboardSettingsDialogComponent } from './dashboard-settings-dialog.component';
import { ManageDashboardStatesDialogComponent } from './states/manage-dashboard-states-dialog.component';
import { DashboardStateDialogComponent } from './states/dashboard-state-dialog.component';

@NgModule({
  declarations: [
    DashboardFormComponent,
    DashboardTabsComponent,
    ManageDashboardCustomersDialogComponent,
    MakeDashboardPublicDialogComponent,
    DashboardToolbarComponent,
    DashboardPageComponent,
    DashboardLayoutComponent,
    EditWidgetComponent,
    DashboardWidgetSelectComponent,
    AddWidgetDialogComponent,
    ManageDashboardLayoutsDialogComponent,
    DashboardSettingsDialogComponent,
    ManageDashboardStatesDialogComponent,
    DashboardStateDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    StatesControllerModule,
    DashboardRoutingModule
  ]
})
export class DashboardModule { }
