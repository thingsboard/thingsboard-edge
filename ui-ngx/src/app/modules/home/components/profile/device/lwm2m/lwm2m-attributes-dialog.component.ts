///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import {Component, Inject, OnInit, SkipSelf} from "@angular/core";
import {ErrorStateMatcher} from "@angular/material/core";
import {DialogComponent} from "@shared/components/dialog.component";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {Router} from "@angular/router";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {JsonObject} from "@angular/compiler-cli/ngcc/src/packages/entry_point";

export interface Lwm2mAttributesDialogData {
  readonly: boolean;
  attributeLwm2m: JsonObject;
  destName: string
}

@Component({
  selector: 'tb-lwm2m-attributes-dialog',
  templateUrl: './lwm2m-attributes-dialog.component.html',
  styleUrls: ['./lwm2m-attributes.component.scss'],
  providers: [{provide: ErrorStateMatcher, useExisting: Lwm2mAttributesDialogComponent}],
})
export class Lwm2mAttributesDialogComponent extends DialogComponent<Lwm2mAttributesDialogComponent, Object> implements OnInit, ErrorStateMatcher {

  readonly = this.data.readonly;

  attributeLwm2m = this.data.attributeLwm2m;

  submitted = false;

  dirtyValue = false;

  attributeLwm2mDialogFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: Lwm2mAttributesDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<Lwm2mAttributesDialogComponent, object>,
              private fb: FormBuilder,
              public translate: TranslateService) {
    super(store, router, dialogRef);

    this.attributeLwm2mDialogFormGroup = this.fb.group({
      keyFilters: [{}, []]
    });
    this.attributeLwm2mDialogFormGroup.patchValue({keyFilters: this.attributeLwm2m});
    this.attributeLwm2mDialogFormGroup.get('keyFilters').valueChanges.subscribe((attributes) => {
      this.attributeLwm2m = attributes;
    });
    if (this.readonly) {
      this.attributeLwm2mDialogFormGroup.disable({emitEvent: false});
    }
  }

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  save(): void {
    this.submitted = true;
    this.dialogRef.close(this.attributeLwm2m);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
