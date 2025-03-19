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

import { Component, Inject, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroupDirective, NgForm, UntypedFormControl } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetsBundleComponent } from '@home/pages/widget/widgets-bundle.component';
import { WidgetService } from '@core/http/widget.service';

export interface WidgetsBundleDialogData {
  widgetsBundle: WidgetsBundle;
}

@Component({
  selector: 'tb-widgets-bundle-dialog',
  templateUrl: './widgets-bundle-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: WidgetsBundleDialogComponent}],
  styleUrls: ['widgets-bundle-dialog.component.scss']
})
export class WidgetsBundleDialogComponent extends
  DialogComponent<WidgetsBundleDialogComponent, WidgetsBundle> implements ErrorStateMatcher {

  widgetsBundle: WidgetsBundle;

  submitted = false;

  @ViewChild('widgetsBundleComponent', {static: true}) widgetsBundleComponent: WidgetsBundleComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: WidgetsBundleDialogData,
              public dialogRef: MatDialogRef<WidgetsBundleDialogComponent, WidgetsBundle>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private widgetsService: WidgetService) {
    super(store, router, dialogRef);
    this.widgetsBundle = this.data.widgetsBundle;
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.widgetsBundleComponent.entityForm.valid) {
      this.widgetsBundle = {...this.widgetsBundle, ...this.widgetsBundleComponent.entityFormValue()};
      this.widgetsService.saveWidgetsBundle(this.widgetsBundle).subscribe((widgetBundle) => {
        this.dialogRef.close(widgetBundle);
      });
    }
  }

}
