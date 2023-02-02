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
import {
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NgForm,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { DashboardState } from '@app/shared/models/dashboard.models';
import { DashboardStateInfo } from '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component.models';
import { TranslateService } from '@ngx-translate/core';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';

export interface DashboardStateDialogData {
  states: {[id: string]: DashboardState };
  state: DashboardStateInfo;
  isAdd: boolean;
}

@Component({
  selector: 'tb-dashboard-state-dialog',
  templateUrl: './dashboard-state-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: DashboardStateDialogComponent}],
  styleUrls: []
})
export class DashboardStateDialogComponent extends
  DialogComponent<DashboardStateDialogComponent, DashboardStateInfo>
  implements OnInit, ErrorStateMatcher {

  stateFormGroup: FormGroup;

  states: {[id: string]: DashboardState };
  state: DashboardStateInfo;
  prevStateId: string;

  stateIdTouched: boolean;

  isAdd: boolean;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DashboardStateDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<DashboardStateDialogComponent, DashboardStateInfo>,
              private fb: FormBuilder,
              private translate: TranslateService,
              private dashboardUtils: DashboardUtilsService) {
    super(store, router, dialogRef);

    this.states = this.data.states;
    this.isAdd = this.data.isAdd;
    if (this.isAdd) {
      this.state = {id: '', ...this.dashboardUtils.createDefaultState('', false)};
      this.prevStateId = '';
    } else {
      this.state = this.data.state;
      this.prevStateId = this.state.id;
    }

    this.stateFormGroup = this.fb.group({
      name: [this.state.name, [Validators.required]],
      id: [this.state.id, [Validators.required, this.validateDuplicateStateId()]],
      root: [this.state.root, []],
    });

    this.stateFormGroup.get('name').valueChanges.subscribe((name: string) => {
      this.checkStateName(name);
    });

    this.stateFormGroup.get('id').valueChanges.subscribe((id: string) => {
      this.stateIdTouched = true;
    });
  }

  private checkStateName(name: string) {
    if (name && !this.stateIdTouched && this.isAdd) {
      this.stateFormGroup.get('id').setValue(
        name.toLowerCase().replace(/\W/g, '_'),
        { emitEvent: false }
      );
    }
  }

  private validateDuplicateStateId(): ValidatorFn {
    return (c: FormControl) => {
      const newStateId: string = c.value;
      if (newStateId) {
        const existing = this.states[newStateId];
        if (existing && newStateId !== this.prevStateId) {
          return {
            stateExists: true
          };
        }
      }
      return null;
    };
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
    this.state = {...this.state, ...this.stateFormGroup.value};
    this.state.id = this.state.id.trim();
    this.dialogRef.close(this.state);
  }
}
