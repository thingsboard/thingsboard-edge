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

import { Component, Inject, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { AttributesNameValueMap } from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';

export interface Lwm2mAttributesDialogData {
  readonly: boolean;
  attributes: AttributesNameValueMap;
  modelName: string;
  isResource: boolean;
}

@Component({
  selector: 'tb-lwm2m-attributes-dialog',
  templateUrl: './lwm2m-attributes-dialog.component.html',
  styleUrls: [],
  providers: [{provide: ErrorStateMatcher, useExisting: Lwm2mAttributesDialogComponent}],
})
export class Lwm2mAttributesDialogComponent
  extends DialogComponent<Lwm2mAttributesDialogComponent, AttributesNameValueMap> implements ErrorStateMatcher {

  readonly: boolean;
  name: string;
  isResource: boolean;

  private submitted = false;

  attributeFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: Lwm2mAttributesDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<Lwm2mAttributesDialogComponent, AttributesNameValueMap>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.readonly = data.readonly;
    this.name = data.modelName;
    this.isResource = data.isResource;

    this.attributeFormGroup = this.fb.group({
      attributes: [data.attributes]
    });
    if (this.readonly) {
      this.attributeFormGroup.disable({emitEvent: false});
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  save(): void {
    this.submitted = true;
    this.dialogRef.close(this.attributeFormGroup.get('attributes').value);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
