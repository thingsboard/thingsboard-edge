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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import {
  ComplexFilterPredicateInfo,
  ComplexOperation,
  complexOperationTranslationMap,
  FilterPredicateType
} from '@shared/models/query/query.models';
import { ComplexFilterPredicateDialogData } from '@home/components/filter/filter-component.models';

@Component({
  selector: 'tb-complex-filter-predicate-dialog',
  templateUrl: './complex-filter-predicate-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ComplexFilterPredicateDialogComponent}],
  styleUrls: []
})
export class ComplexFilterPredicateDialogComponent extends
  DialogComponent<ComplexFilterPredicateDialogComponent, ComplexFilterPredicateInfo>
  implements OnInit, ErrorStateMatcher {

  complexFilterFormGroup: FormGroup;

  complexOperations = Object.keys(ComplexOperation);
  complexOperationEnum = ComplexOperation;
  complexOperationTranslations = complexOperationTranslationMap;

  isAdd: boolean;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ComplexFilterPredicateDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ComplexFilterPredicateDialogComponent, ComplexFilterPredicateInfo>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.isAdd = this.data.isAdd;

    this.complexFilterFormGroup = this.fb.group(
      {
        operation: [this.data.complexPredicate.operation, [Validators.required]],
        predicates: [this.data.complexPredicate.predicates, [Validators.required]]
      }
    );
    if (this.data.readonly) {
      this.complexFilterFormGroup.disable({emitEvent: false});
    }
  }

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.complexFilterFormGroup.valid) {
      const predicate: ComplexFilterPredicateInfo = this.complexFilterFormGroup.getRawValue();
      predicate.type = FilterPredicateType.COMPLEX;
      this.dialogRef.close(predicate);
    }
  }
}
