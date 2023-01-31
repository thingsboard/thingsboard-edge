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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder } from '@angular/forms';
import { DashboardService } from '@core/http/dashboard.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EntityGroupInfo, ShortEntityView } from '@shared/models/entity-group.models';
import { DashboardInfo } from '@shared/models/dashboard.models';

export interface PublicDashboardLinkDialogData {
  dashboard: ShortEntityView | DashboardInfo;
  entityGroup: EntityGroupInfo;
}

@Component({
  selector: 'tb-public-dashboard-link-dialog',
  templateUrl: './public-dashboard-link.dialog.component.html',
  styleUrls: []
})
export class PublicDashboardLinkDialogComponent extends DialogComponent<PublicDashboardLinkDialogComponent> implements OnInit {

  dashboard: ShortEntityView | DashboardInfo;
  entityGroup: EntityGroupInfo;

  publicLink: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: PublicDashboardLinkDialogData,
              public translate: TranslateService,
              private dashboardService: DashboardService,
              public dialogRef: MatDialogRef<PublicDashboardLinkDialogComponent>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);

    this.dashboard = data.dashboard;
    this.entityGroup = data.entityGroup;
    this.publicLink = dashboardService.getPublicDashboardLink(this.dashboard, this.entityGroup);
  }

  ngOnInit(): void {
  }

  close(): void {
    this.dialogRef.close();
  }


  onPublicLinkCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('dashboard.public-link-copied-message'),
        type: 'success',
        target: 'publicDashboardLinkDialogContent',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

}
