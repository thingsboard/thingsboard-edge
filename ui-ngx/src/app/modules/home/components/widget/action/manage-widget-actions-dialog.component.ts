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

import { widgetActionTypes, widgetType } from '@shared/models/widget.models';
import {
  WidgetActionCallbacks,
  WidgetActionsData
} from '@home/components/widget/action/manage-widget-actions.component.models';
import { Component, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';

export interface ManageWidgetActionsDialogData {
  widgetTitle: string;
  actionsData: WidgetActionsData;
  callbacks: WidgetActionCallbacks;
  widgetType: widgetType;
}

@Component({
  selector: 'tb-manage-widget-actions-dialog',
  templateUrl: './manage-widget-actions-dialog.component.html',
  providers: [],
  styleUrls: []
})
export class ManageWidgetActionsDialogComponent extends DialogComponent<ManageWidgetActionsDialogComponent,
  WidgetActionsData> implements OnInit {

  widgetActionTypesList = widgetActionTypes;

  actionSources = this.data.actionsData.actionSources;
  actionsSettings: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageWidgetActionsDialogData,
              private fb: UntypedFormBuilder,
              public dialogRef: MatDialogRef<ManageWidgetActionsDialogComponent, WidgetActionsData>) {
    super(store, router, dialogRef);
  }

  ngOnInit() {
    this.actionsSettings = this.fb.group({
      actions: [this.data.actionsData.actionsMap, []]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.actionsSettings.get('actions').value);
  }

}
