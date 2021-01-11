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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  EntityGroupColumn,
  EntityGroupColumnType,
  entityGroupColumnTypeTranslationMap,
  EntityGroupEntityField,
  EntityGroupSortOrder,
  entityGroupSortOrderTranslationMap
} from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';

export interface EntityGroupColumnDialogData {
  isReadOnly: boolean;
  column: EntityGroupColumn;
  entityType: EntityType;
  columnTypes: EntityGroupColumnType[];
  entityFields: {[fieldName: string]: EntityGroupEntityField};
}

@Component({
  selector: 'tb-entity-group-column-dialog',
  templateUrl: './entity-group-column-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: EntityGroupColumnDialogComponent}],
  styleUrls: []
})
export class EntityGroupColumnDialogComponent extends
  DialogComponent<EntityGroupColumnDialogComponent, EntityGroupColumn> implements OnInit, ErrorStateMatcher {

  columnFormGroup: FormGroup;

  columnType = EntityGroupColumnType;

  isReadOnly = this.data.isReadOnly;
  column = this.data.column;
  entityType = this.data.entityType;
  columnTypes = this.data.columnTypes;
  entityFields = this.data.entityFields;
  entityFieldKeys = Object.keys(this.entityFields);
  sortOrders = Object.keys(EntityGroupSortOrder);

  entityGroupColumnTypeTranslations = entityGroupColumnTypeTranslationMap;
  entityGroupSortOrderTranslations = entityGroupSortOrderTranslationMap;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EntityGroupColumnDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<EntityGroupColumnDialogComponent, EntityGroupColumn>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.columnFormGroup = this.fb.group({
      type: [null, Validators.required],
      key: [null, Validators.required],
      title: [null],
      sortOrder: [null, Validators.required],
      mobileHide: [null],
      useCellStyleFunction: [null],
      cellStyleFunction: [null],
      useCellContentFunction: [null],
      cellContentFunction: [null]
    });
    this.columnFormGroup.reset(this.column, {emitEvent: false});
    if (this.isReadOnly) {
      this.columnFormGroup.disable({emitEvent: false});
    } else {
      this.columnFormGroup.get('useCellStyleFunction').valueChanges.subscribe(() => {
        this.updateDisabledState();
      });
      this.columnFormGroup.get('useCellContentFunction').valueChanges.subscribe(() => {
        this.updateDisabledState();
      });
      this.updateDisabledState();
    }
  }

  private updateDisabledState() {
    const useCellStyleFunction: boolean = this.columnFormGroup.get('useCellStyleFunction').value;
    const useCellContentFunction: boolean = this.columnFormGroup.get('useCellContentFunction').value;
    if (useCellStyleFunction) {
      this.columnFormGroup.get('cellStyleFunction').enable({emitEvent: false});
    } else {
      this.columnFormGroup.get('cellStyleFunction').disable({emitEvent: false});
    }
    if (useCellContentFunction) {
      this.columnFormGroup.get('cellContentFunction').enable({emitEvent: false});
    } else {
      this.columnFormGroup.get('cellContentFunction').disable({emitEvent: false});
    }
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
    if (this.columnFormGroup.valid) {
      this.column = {...this.column, ...this.columnFormGroup.value};
      this.dialogRef.close(this.column);
    }
  }
}
