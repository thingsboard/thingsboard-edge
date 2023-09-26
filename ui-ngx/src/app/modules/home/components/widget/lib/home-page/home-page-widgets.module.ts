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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { ClusterInfoTableComponent } from '@home/components/widget/lib/home-page/cluster-info-table.component';
import { ConfiguredFeaturesComponent } from '@home/components/widget/lib/home-page/configured-features.component';
import { VersionInfoComponent } from '@home/components/widget/lib/home-page/version-info.component';
import { DocLinksWidgetComponent } from '@home/components/widget/lib/home-page/doc-links-widget.component';
import { DocLinkComponent } from '@home/components/widget/lib/home-page/doc-link.component';
import { AddDocLinkDialogComponent } from '@home/components/widget/lib/home-page/add-doc-link-dialog.component';
import { EditLinksDialogComponent } from '@home/components/widget/lib/home-page/edit-links-dialog.component';
import { GettingStartedWidgetComponent } from '@home/components/widget/lib/home-page/getting-started-widget.component';
import {
  GettingStartedCompletedDialogComponent
} from '@home/components/widget/lib/home-page/getting-started-completed-dialog.component';
import { LicenseUsageInfoComponent } from '@home/components/widget/lib/home-page/license-usage-info.component';
import { UsageInfoWidgetComponent } from '@home/components/widget/lib/home-page/usage-info-widget.component';
import { QuickLinksWidgetComponent } from '@home/components/widget/lib/home-page/quick-links-widget.component';
import { QuickLinkComponent } from '@home/components/widget/lib/home-page/quick-link.component';
import { AddQuickLinkDialogComponent } from '@home/components/widget/lib/home-page/add-quick-link-dialog.component';
import {
  RecentDashboardsWidgetComponent
} from '@home/components/widget/lib/home-page/recent-dashboards-widget.component';
import {
  SolutionTemplatesWidgetComponent
} from '@home/components/widget/lib/home-page/solution-templates-widget.component';
import {
  SolutionTemplateVideoComponent
} from '@home/components/widget/lib/home-page/solution-template-video.component';

@NgModule({
  declarations:
    [
      ClusterInfoTableComponent,
      ConfiguredFeaturesComponent,
      VersionInfoComponent,
      DocLinksWidgetComponent,
      DocLinkComponent,
      AddDocLinkDialogComponent,
      EditLinksDialogComponent,
      GettingStartedWidgetComponent,
      GettingStartedCompletedDialogComponent,
      LicenseUsageInfoComponent,
      UsageInfoWidgetComponent,
      QuickLinksWidgetComponent,
      QuickLinkComponent,
      AddQuickLinkDialogComponent,
      RecentDashboardsWidgetComponent,
      SolutionTemplatesWidgetComponent,
      SolutionTemplateVideoComponent
    ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    ClusterInfoTableComponent,
    ConfiguredFeaturesComponent,
    VersionInfoComponent,
    DocLinksWidgetComponent,
    DocLinkComponent,
    AddDocLinkDialogComponent,
    EditLinksDialogComponent,
    GettingStartedWidgetComponent,
    GettingStartedCompletedDialogComponent,
    LicenseUsageInfoComponent,
    UsageInfoWidgetComponent,
    QuickLinksWidgetComponent,
    QuickLinkComponent,
    AddQuickLinkDialogComponent,
    RecentDashboardsWidgetComponent,
    SolutionTemplatesWidgetComponent,
    SolutionTemplateVideoComponent
  ]
})
export class HomePageWidgetsModule { }
