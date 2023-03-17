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

import {
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { filter, map, mergeMap, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { BaseData } from '@shared/models/base-data';
import { EntityId, entityIdEquals } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipGrid } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupService } from '@core/http/entity-group.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';

@Component({
  selector: 'tb-entity-group-list',
  templateUrl: './entity-group-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityGroupListComponent),
      multi: true
    }
  ]
})
export class EntityGroupListComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnChanges {

  entityGroupListFormGroup: UntypedFormGroup;

  modelValue: Array<string> | null;

  @Input()
  groupType: EntityType;

  private ownerIdValue: EntityId;
  get ownerId(): EntityId {
    return this.ownerIdValue;
  }

  @Input()
  set ownerId(value: EntityId) {
    if (!entityIdEquals(this.ownerIdValue, value)) {
      this.reset();
    }
    this.ownerIdValue = value;
  }

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  disabled: boolean;

  @ViewChild('entityGroupInput') entityGroupInput: ElementRef<HTMLInputElement>;
  @ViewChild('entityGroupAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipGrid;

  entityGroups: Array<BaseData<EntityId>> = [];
  filteredEntityGroups: Observable<Array<BaseData<EntityId>>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityService: EntityService,
              private entityGroupService: EntityGroupService,
              private fb: UntypedFormBuilder) {
    this.entityGroupListFormGroup = this.fb.group({
      entityGroups: [this.entityGroups, this.required ? [Validators.required] : []],
      entityGroup: [null]
    });
  }

  updateValidators() {
    this.entityGroupListFormGroup.get('entityGroups').setValidators(this.required ? [Validators.required] : []);
    this.entityGroupListFormGroup.get('entityGroups').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredEntityGroups = this.entityGroupListFormGroup.get('entityGroup').valueChanges
      .pipe(
        // startWith<string | BaseData<EntityId>>(''),
        tap((value) => {
          if (value && typeof value !== 'string') {
            this.add(value);
          } else if (value === null) {
            this.clear(this.entityGroupInput.nativeElement.value);
          }
        }),
        filter((value) => typeof value === 'string'),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchEntityGroups(name) ),
        share()
      );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'groupType') {
          this.reset();
        }
      }
    }
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.entityGroupListFormGroup.disable({emitEvent: false});
    } else {
      this.entityGroupListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.entityService.getEntities(EntityType.ENTITY_GROUP, value).subscribe(
        (entityGroups) => {
          this.entityGroups = entityGroups;
          this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
        }
      );
    } else {
      this.entityGroups = [];
      this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  reset() {
    this.entityGroups = [];
    this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
    this.modelValue = null;
    if (this.entityGroupInput) {
      this.entityGroupInput.nativeElement.value = '';
    }
    this.entityGroupListFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
    this.propagateChange(this.modelValue);
    this.dirty = true;
  }

  add(entityGroup: BaseData<EntityId>): void {
    if (!this.modelValue || this.modelValue.indexOf(entityGroup.id.id) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(entityGroup.id.id);
      this.entityGroups.push(entityGroup);
      this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  remove(entityGroup: BaseData<EntityId>) {
    const index = this.entityGroups.indexOf(entityGroup);
    if (index >= 0) {
      this.entityGroups.splice(index, 1);
      this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.propagateChange(this.modelValue);
      this.clear();
    }
  }

  displayEntityGroupFn(entityGroup?: BaseData<EntityId>): string | undefined {
    return entityGroup ? entityGroup.name : undefined;
  }

  fetchEntityGroups(searchText?: string): Observable<Array<BaseData<EntityId>>> {
    this.searchText = searchText;
    if (this.ownerId) {
      const pageLink = new PageLink(50, 0, searchText, {
        property: 'name',
        direction: Direction.ASC
      });
      return this.entityGroupService.getEntityGroupsByOwnerIdAndPageLink(
        this.ownerId.entityType as EntityType, this.ownerId.id, this.groupType, pageLink, {ignoreLoading: true}).pipe(
        map((data) => data ? data.data : [])
      );
    } else {
      return this.entityService.getEntitiesByNameFilter(EntityType.ENTITY_GROUP, searchText,
        50, this.groupType, {ignoreLoading: true}).pipe(
        map((data) => data ? data : []));
    }
  }

  onFocus() {
    if (this.dirty) {
      this.entityGroupListFormGroup.get('entityGroup').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  clear(value: string = '') {
    this.entityGroupInput.nativeElement.value = value;
    this.entityGroupListFormGroup.get('entityGroup').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entityGroupInput.nativeElement.blur();
      this.entityGroupInput.nativeElement.focus();
    }, 0);
  }

}
