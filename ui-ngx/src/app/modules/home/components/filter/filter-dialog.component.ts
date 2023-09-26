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
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  FormGroupDirective,
  NgForm,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { Filter, Filters } from '@shared/models/query/query.models';

export interface FilterDialogData {
  isAdd: boolean;
  filters: Filters | Array<Filter>;
  filter?: Filter;
}

@Component({
  selector: 'tb-filter-dialog',
  templateUrl: './filter-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: FilterDialogComponent}],
  styleUrls: ['./filter-dialog.component.scss']
})
export class FilterDialogComponent extends DialogComponent<FilterDialogComponent, Filter>
  implements OnInit, ErrorStateMatcher {

  isAdd: boolean;
  filters: Array<Filter>;

  filter: Filter;

  filterFormGroup: UntypedFormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: FilterDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<FilterDialogComponent, Filter>,
              private fb: UntypedFormBuilder,
              private utils: UtilsService,
              public translate: TranslateService) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    if (Array.isArray(data.filters)) {
      this.filters = data.filters;
    } else {
      this.filters = [];
      for (const filterId of Object.keys(data.filters)) {
        this.filters.push(data.filters[filterId]);
      }
    }
    if (this.isAdd && !this.data.filter) {
      this.filter = {
        id: null,
        filter: '',
        keyFilters: [],
        editable: true
      };
    } else {
      this.filter = data.filter;
    }

    this.filterFormGroup = this.fb.group({
      filter: [this.filter.filter, [this.validateDuplicateFilterName(), Validators.required]],
      editable: [this.filter.editable],
      keyFilters: [this.filter.keyFilters, Validators.required]
    });
  }

  validateDuplicateFilterName(): ValidatorFn {
    return (c: UntypedFormControl) => {
      const newFilter = c.value.trim();
      const found = this.filters.find((filter) => filter.filter === newFilter);
      if (found) {
        if (this.isAdd || this.filter.id !== found.id) {
          return {
            duplicateFilterName: {
              valid: false
            }
          };
        }
      }
      return null;
    };
  }

  ngOnInit(): void {
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
    this.filter.filter = this.filterFormGroup.get('filter').value.trim();
    this.filter.editable = this.filterFormGroup.get('editable').value;
    this.filter.keyFilters = this.filterFormGroup.get('keyFilters').value;
    if (this.isAdd) {
      this.filter.id = this.utils.guid();
    }
    this.dialogRef.close(this.filter);
  }
}
