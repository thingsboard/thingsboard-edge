///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { DashboardFormComponent } from '@modules/home/pages/dashboard/dashboard-form.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
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
import { PublicDashboardLinkDialogComponent } from '@home/pages/dashboard/public-dashboard-link.dialog.component';
import { DASHBOARD_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { DashboardGroupConfigFactory } from '@home/pages/dashboard/dashboard-group-config.factory';

@NgModule({
  declarations: [
    DashboardFormComponent,
    DashboardToolbarComponent,
    DashboardPageComponent,
    DashboardLayoutComponent,
    EditWidgetComponent,
    DashboardWidgetSelectComponent,
    AddWidgetDialogComponent,
    ManageDashboardLayoutsDialogComponent,
    DashboardSettingsDialogComponent,
    ManageDashboardStatesDialogComponent,
    DashboardStateDialogComponent,
    PublicDashboardLinkDialogComponent
  ],
  exports: [
    DashboardPageComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    StatesControllerModule
  ],
  providers: [
    {
      provide: DASHBOARD_GROUP_CONFIG_FACTORY,
      useClass: DashboardGroupConfigFactory
    }
  ]
})
export class DashboardModule { }
