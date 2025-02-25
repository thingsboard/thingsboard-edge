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

import { Component } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { CustomMobilePage } from '@shared/models/mobile-app.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { FormBuilder } from '@angular/forms';
import { deepTrim } from '@core/utils';

@Component({
  selector: 'tb-add-mobile-page-dialog',
  templateUrl: './add-mobile-page-dialog.component.html',
  styleUrls: ['./add-mobile-page-dialog.component.scss']
})
export class AddMobilePageDialogComponent extends DialogComponent<AddMobilePageDialogComponent, CustomMobilePage> {

  customMobilePage = this.fb.control<CustomMobilePage>(null);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<AddMobilePageDialogComponent, CustomMobilePage>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save() {
    if (this.customMobilePage.valid) {
      const pageItem = deepTrim(this.customMobilePage.value);
      this.dialogRef.close(pageItem);
    }
  }
}
