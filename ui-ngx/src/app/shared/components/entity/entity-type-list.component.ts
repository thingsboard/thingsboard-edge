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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { filter, map, mergeMap, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { AliasEntityType, EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipGrid } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { FloatLabelType, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';

interface EntityTypeInfo {
  name: string;
  value: EntityType;
}

@Component({
  selector: 'tb-entity-type-list',
  templateUrl: './entity-type-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityTypeListComponent),
      multi: true
    }
  ]
})
export class EntityTypeListComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  entityTypeListFormGroup: UntypedFormGroup;

  modelValue: Array<EntityType> | null;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }

  @Input() label: string;

  @Input() floatLabel: FloatLabelType = 'auto';

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
  allowedEntityTypes: Array<EntityType | AliasEntityType>;

  @Input()
  @coerceBoolean()
  ignoreAuthorityFilter: boolean;

  @ViewChild('entityTypeInput') entityTypeInput: ElementRef<HTMLInputElement>;
  @ViewChild('entityTypeAutocomplete') entityTypeAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipGrid;

  allEntityTypeList: Array<EntityTypeInfo> = [];
  entityTypeList: Array<EntityTypeInfo> = [];
  filteredEntityTypeList: Observable<Array<EntityTypeInfo>>;

  placeholder: string;
  secondaryPlaceholder: string;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityService: EntityService,
              private fb: UntypedFormBuilder) {
    this.entityTypeListFormGroup = this.fb.group({
      entityTypeList: [this.entityTypeList, this.required ? [Validators.required] : []],
      entityType: [null]
    });
  }

  updateValidators() {
    this.entityTypeListFormGroup.get('entityTypeList').setValidators(this.required ? [Validators.required] : []);
    this.entityTypeListFormGroup.get('entityTypeList').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {

    this.placeholder = this.required ? this.translate.instant('entity.enter-entity-type')
      : this.translate.instant('entity.any-entity');
    this.secondaryPlaceholder = '+' + this.translate.instant('entity.entity-type');

    let entityTypes: Array<EntityType | AliasEntityType>;
    if (this.ignoreAuthorityFilter && this.allowedEntityTypes
      && this.allowedEntityTypes.length) {
      entityTypes = [];
      this.allowedEntityTypes.forEach((entityTypeValue) => {
        entityTypes.push(entityTypeValue);
      });
    } else {
      entityTypes = this.entityService.prepareAllowedEntityTypesList(this.allowedEntityTypes) as Array<EntityType>;
    }

    entityTypes.forEach((entityType) => {
      this.allEntityTypeList.push({
        name: this.translate.instant(entityTypeTranslations.get(entityType).type),
        value: entityType as EntityType
      });
    });

    this.filteredEntityTypeList = this.entityTypeListFormGroup.get('entityType').valueChanges
      .pipe(
        // startWith<string | BaseData<EntityId>>(''),
        tap((value) => {
          if (value && typeof value !== 'string') {
            this.add(value);
          } else if (value === null) {
            this.clear(this.entityTypeInput.nativeElement.value);
          }
        }),
        filter((value) => typeof value === 'string'),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchEntityTypes(name) ),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.entityTypeListFormGroup.disable({emitEvent: false});
    } else {
      this.entityTypeListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<EntityType> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.entityTypeList = [];
      value.forEach((entityType) => {
        this.entityTypeList.push({
          name: entityTypeTranslations.has(entityType) ? this.translate.instant(entityTypeTranslations.get(entityType).type) : 'Unknown',
          value: entityType
        });
      });
      this.entityTypeListFormGroup.get('entityTypeList').setValue(this.entityTypeList);
    } else {
      this.entityTypeList = [];
      this.entityTypeListFormGroup.get('entityTypeList').setValue(this.entityTypeList);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  add(entityType: EntityTypeInfo): void {
    if (!this.modelValue || this.modelValue.indexOf(entityType.value) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(entityType.value);
      this.entityTypeList.push(entityType);
      this.entityTypeListFormGroup.get('entityTypeList').setValue(this.entityTypeList);
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  remove(entityType: EntityTypeInfo) {
    const index = this.entityTypeList.indexOf(entityType);
    if (index >= 0) {
      this.entityTypeList.splice(index, 1);
      this.entityTypeListFormGroup.get('entityTypeList').setValue(this.entityTypeList);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.propagateChange(this.modelValue);
      this.clear();
    }
  }

  displayEntityTypeFn(entityType?: EntityTypeInfo): string | undefined {
    return entityType ? entityType.name : undefined;
  }

  fetchEntityTypes(searchText?: string): Observable<Array<EntityTypeInfo>> {
    this.searchText = searchText;
    let result = this.allEntityTypeList;
    if (searchText && searchText.length) {
      result = this.allEntityTypeList.filter((entityTypeInfo) => entityTypeInfo.name.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
  }

  onFocus() {
    if (this.dirty) {
      this.entityTypeListFormGroup.get('entityType').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  clear(value: string = '') {
    this.entityTypeInput.nativeElement.value = value;
    this.entityTypeListFormGroup.get('entityType').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entityTypeInput.nativeElement.blur();
      this.entityTypeInput.nativeElement.focus();
    }, 0);
  }

}
