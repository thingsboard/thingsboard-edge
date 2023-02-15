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
import { WidgetService } from '@core/http/widget.service';

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

  functionScopeVariables: string[];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EntityGroupColumnDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<EntityGroupColumnDialogComponent, EntityGroupColumn>,
              private widgetService: WidgetService,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.functionScopeVariables = this.widgetService.getWidgetScopeVariables();
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
