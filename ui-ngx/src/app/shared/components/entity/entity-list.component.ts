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
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipGrid } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { SubscriptSizing } from '@angular/material/form-field';

@Component({
  selector: 'tb-entity-list',
  templateUrl: './entity-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityListComponent),
      multi: true
    }
  ]
})
export class EntityListComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnChanges {

  entityListFormGroup: UntypedFormGroup;

  modelValue: Array<string> | null;

  @Input()
  entityType: EntityType;

  @Input()
  entitySubType = '';

  @Input()
  entityListText = 'entity.entity-list';

  @Input()
  noEntitiesText = 'entity.no-entities-matching';

  @Input()
  entitiesRequiredText = 'entity.entity-list-empty';

  // @Input()
  // subType: string;

  @Input()
  labelText: string;

  @Input()
  placeholderText = this.translate.instant('entity.entity-list');

  @Input()
  requiredText = this.translate.instant('entity.entity-list-empty');

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

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  hint: string;

  @ViewChild('entityInput') entityInput: ElementRef<HTMLInputElement>;
  @ViewChild('entityAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipGrid;

  entities: Array<BaseData<EntityId>> = [];
  filteredEntities: Observable<Array<BaseData<EntityId>>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityService: EntityService,
              private fb: UntypedFormBuilder) {
    this.entityListFormGroup = this.fb.group({
      entities: [this.entities, this.required ? [Validators.required] : []],
      entity: [null]
    });
  }

  updateValidators() {
    this.entityListFormGroup.get('entities').setValidators(this.required ? [Validators.required] : []);
    this.entityListFormGroup.get('entities').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredEntities = this.entityListFormGroup.get('entity').valueChanges
    .pipe(
      // startWith<string | BaseData<EntityId>>(''),
      tap((value) => {
        if (value && typeof value !== 'string') {
          this.add(value);
        } else if (value === null) {
          this.clear(this.entityInput.nativeElement.value);
        }
      }),
      filter((value) => typeof value === 'string'),
      map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
      mergeMap(name => this.fetchEntities(name) ),
      share()
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'entityType') {
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
      this.entityListFormGroup.disable({emitEvent: false});
    } else {
      this.entityListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.entityService.getEntities(this.entityType, value).subscribe(
        (entities) => {
          this.entities = entities;
          this.entityListFormGroup.get('entities').setValue(this.entities);
        }
      );
    } else {
      this.entities = [];
      this.entityListFormGroup.get('entities').setValue(this.entities);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  reset() {
    this.entities = [];
    this.entityListFormGroup.get('entities').setValue(this.entities);
    this.modelValue = null;
    if (this.entityInput) {
      this.entityInput.nativeElement.value = '';
    }
    this.entityListFormGroup.get('entity').patchValue('', {emitEvent: false});
    this.propagateChange(this.modelValue);
    this.dirty = true;
  }

  add(entity: BaseData<EntityId>): void {
    if (!this.modelValue || this.modelValue.indexOf(entity.id.id) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(entity.id.id);
      this.entities.push(entity);
      this.entityListFormGroup.get('entities').setValue(this.entities);
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  remove(entity: BaseData<EntityId>) {
    let index = this.entities.indexOf(entity);
    if (index >= 0) {
      this.entities.splice(index, 1);
      this.entityListFormGroup.get('entities').setValue(this.entities);
      index = this.modelValue.indexOf(entity.id.id);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.propagateChange(this.modelValue);
      this.clear();
    }
  }

  displayEntityFn(entity?: BaseData<EntityId>): string | undefined {
    return entity ? entity.name : undefined;
  }

  fetchEntities(searchText?: string): Observable<Array<BaseData<EntityId>>> {
    this.searchText = searchText;
    return this.entityService.getEntitiesByNameFilter(this.entityType, searchText,
      50, this.entitySubType, {ignoreLoading: true}).pipe(
      map((data) => data ? data : []));
  }

  onFocus() {
    if (this.dirty) {
      this.entityListFormGroup.get('entity').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  clear(value: string = '') {
    this.entityInput.nativeElement.value = value;
    this.entityListFormGroup.get('entity').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entityInput.nativeElement.blur();
      this.entityInput.nativeElement.focus();
    }, 0);
  }

  get placeholder(): string {
    return this.placeholderText ? this.placeholderText : (this.entityListText ? this.translate.instant(this.entityListText): undefined);
  }

  get requiredLabel(): string {
    return this.requiredText ? this.requiredText :
      (this.entitiesRequiredText ? this.translate.instant(this.entitiesRequiredText): undefined);
  }

}
