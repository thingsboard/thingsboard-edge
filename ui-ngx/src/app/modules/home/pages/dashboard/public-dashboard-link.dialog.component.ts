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
