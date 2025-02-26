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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { isDefinedAndNotNull } from '@core/utils';

export interface ExportResourceDialogData {
  title: string;
  prompt: string;
  include?: boolean;
  ignoreLoading?: boolean;
}

export interface ExportResourceDialogDialogResult {
  include: boolean;
}

@Component({
  selector: 'tb-export-resource-dialog',
  templateUrl: './export-resource-dialog.component.html',
  styleUrls: []
})
export class ExportResourceDialogComponent extends DialogComponent<ExportResourceDialogComponent, ExportResourceDialogDialogResult> {

  ignoreLoading = false;

  title: string;
  prompt: string

  includeResourcesFormControl = new FormControl(true);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: ExportResourceDialogData,
              public dialogRef: MatDialogRef<ExportResourceDialogComponent, ExportResourceDialogDialogResult>) {
    super(store, router, dialogRef);
    this.ignoreLoading = this.data.ignoreLoading;
    this.title = this.data.title;
    this.prompt = this.data.prompt;
    if (isDefinedAndNotNull(this.data.include)) {
      this.includeResourcesFormControl.patchValue(this.data.include, {emitEvent: false});
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  export(): void {
    this.dialogRef.close({
      include: this.includeResourcesFormControl.value
    });
  }
}
