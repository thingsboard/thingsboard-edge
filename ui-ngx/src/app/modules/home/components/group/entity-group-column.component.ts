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

import { AfterViewInit, Component, EventEmitter, forwardRef, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import { PageComponent } from '@shared/components/page.component';
import {
  EntityGroupColumn,
  EntityGroupColumnType,
  entityGroupColumnTypeTranslationMap,
  EntityGroupEntityField,
  entityGroupEntityFields,
  EntityGroupSortOrder,
  entityGroupSortOrderTranslationMap
} from '@shared/models/entity-group.models';
import { MatDialog } from '@angular/material/dialog';
import {
  EntityGroupColumnDialogComponent,
  EntityGroupColumnDialogData
} from '@home/components/group/entity-group-column-dialog.component';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-entity-group-column',
  templateUrl: './entity-group-column.component.html',
  styleUrls: ['./entity-group-column.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityGroupColumnComponent),
    multi: true
  }]
})
export class EntityGroupColumnComponent extends PageComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  modelValue: EntityGroupColumn | null;

  columnFormGroup: FormGroup;

  @Input()
  entityType: EntityType;

  @Input()
  disabled: boolean;

  @Output()
  defaultSortOrderChanged = new EventEmitter<EntityGroupSortOrder>();

  @Output()
  removeColumn = new EventEmitter();

  @Output()
  updateColumn = new EventEmitter<EntityGroupColumn>();

  columnType = EntityGroupColumnType;

  columnTypes: EntityGroupColumnType[] = [];
  entityFields: {[fieldName: string]: EntityGroupEntityField} = {};
  entityFieldKeys: string[] = [];
  sortOrders = Object.keys(EntityGroupSortOrder);

  entityGroupColumnTypeTranslations = entityGroupColumnTypeTranslationMap;
  entityGroupSortOrderTranslations = entityGroupSortOrderTranslationMap;

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.columnFormGroup = this.fb.group({
      type: [null, Validators.required],
      key: [null, Validators.required],
      title: [null],
      sortOrder: [null, Validators.required],
      mobileHide: [null]
    });
    this.columnFormGroup.get('sortOrder').valueChanges.subscribe((sortOrder: EntityGroupSortOrder) => {
      this.defaultSortOrderChanged.emit(sortOrder);
    });
    this.columnFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });

    switch (this.entityType) {
      case EntityType.USER:
      case EntityType.CUSTOMER:
      case EntityType.ASSET:
      case EntityType.DASHBOARD:
        this.columnTypes.push(EntityGroupColumnType.SERVER_ATTRIBUTE);
        this.columnTypes.push(EntityGroupColumnType.TIMESERIES);
        this.columnTypes.push(EntityGroupColumnType.ENTITY_FIELD);
        break;
      case EntityType.DEVICE:
      case EntityType.ENTITY_VIEW:
        for (const columnType of Object.keys(EntityGroupColumnType)) {
          this.columnTypes.push(EntityGroupColumnType[columnType]);
        }
        break;
    }

    this.entityFields.created_time = entityGroupEntityFields.created_time;

    let entityFieldKeys: string[];

    switch (this.entityType) {
      case EntityType.USER:
        entityFieldKeys = ['email', 'authority', 'first_name', 'last_name'];
        break;
      case EntityType.CUSTOMER:
        entityFieldKeys = ['title', 'email', 'country', 'state', 'city', 'address', 'address2', 'zip', 'phone'];
        break;
      case EntityType.ASSET:
        entityFieldKeys = ['name', 'type', 'label', 'assigned_customer'];
        break;
      case EntityType.DEVICE:
        entityFieldKeys = ['name', 'device_profile', 'label', 'assigned_customer'];
        break;
      case EntityType.ENTITY_VIEW:
        entityFieldKeys = ['name', 'type', 'assigned_customer'];
        break;
      case EntityType.DASHBOARD:
        entityFieldKeys = ['title'];
        break;
    }
    for (const fieldKey of entityFieldKeys) {
      this.entityFields[fieldKey] = entityGroupEntityFields[fieldKey];
    }
    this.entityFieldKeys = Object.keys(this.entityFields);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.columnFormGroup.disable({emitEvent: false})
    } else {
      this.columnFormGroup.enable({emitEvent: false})
    }
  }

  writeValue(value: EntityGroupColumn | null): void {
    this.modelValue = value;
    this.columnFormGroup.reset(this.modelValue || undefined,{emitEvent: false});
  }

  private propagateChange = (v: any) => { };

  private updateModel() {
    if (this.columnFormGroup.valid) {
      const value = this.columnFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

  openColumn($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EntityGroupColumnDialogComponent, EntityGroupColumnDialogData,
      EntityGroupColumn>(EntityGroupColumnDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isReadOnly: this.disabled,
        column: deepClone(this.modelValue),
        entityType: this.entityType,
        columnTypes: this.columnTypes,
        entityFields: this.entityFields
      }
    }).afterClosed().subscribe((column) => {
      if (column && column !== null) {
        this.modelValue = column;
        this.columnFormGroup.reset(column, {emitEvent: false});
        this.updateModel();
        this.updateColumn.emit(this.modelValue);
      }
    });
  }
}
