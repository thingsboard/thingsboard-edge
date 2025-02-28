///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, effect, ElementRef, forwardRef, input, OnChanges, SimpleChanges, ViewChild, } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { map, startWith, switchMap } from 'rxjs/operators';
import { combineLatest, of, Subject } from 'rxjs';
import { EntityService } from '@core/http/entity.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EntitiesKeysByQuery } from '@shared/models/entity.models';
import { EntityFilter } from '@shared/models/query/query.models';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-entity-key-autocomplete',
  templateUrl: './entity-key-autocomplete.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityKeyAutocompleteComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EntityKeyAutocompleteComponent),
      multi: true
    }
  ],
})
export class EntityKeyAutocompleteComponent implements ControlValueAccessor, Validator, OnChanges {

  @ViewChild('keyInput', {static: true}) keyInput: ElementRef;

  entityFilter = input.required<EntityFilter>();
  dataKeyType = input.required<DataKeyType>();
  keyScopeType = input<AttributeScope>();

  keyControl = this.fb.control('', [Validators.required]);
  searchText = '';
  keyInputSubject = new Subject<void>();

  private propagateChange: (value: string) => void;
  private cachedResult: EntitiesKeysByQuery;

  keys$ = this.keyInputSubject.asObservable()
    .pipe(
      switchMap(() => {
        return this.cachedResult ? of(this.cachedResult) : this.entityService.findEntityKeysByQuery({
          pageLink: { page: 0, pageSize: 100 },
          entityFilter: this.entityFilter(),
        }, this.dataKeyType() === DataKeyType.attribute, this.dataKeyType() === DataKeyType.timeseries, this.keyScopeType());
      }),
      map(result => {
        this.cachedResult = result;
        switch (this.dataKeyType()) {
          case DataKeyType.attribute:
            return result.attribute;
          case DataKeyType.timeseries:
            return result.timeseries;
          default:
            return [];
        }
      }),
    );

  filteredKeys$ = combineLatest([this.keys$, this.keyControl.valueChanges.pipe(startWith(''))])
    .pipe(
      map(([keys, searchText = '']) => {
        this.searchText = searchText;
        return searchText ? keys.filter(item => item.toLowerCase().includes(searchText.toLowerCase())) : keys;
      })
    );

  constructor(
    private fb: FormBuilder,
    private entityService: EntityService,
  ) {
    this.keyControl.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(value => this.propagateChange(value));
    effect(() => {
      if (this.keyScopeType() || this.entityFilter() && this.dataKeyType()) {
        this.cachedResult = null;
        this.searchText = '';
      }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    const filterChanged = changes.entityFilter?.previousValue &&
      !isEqual(changes.entityFilter.currentValue, changes.entityFilter.previousValue);
    const keyScopeChanged = changes.keyScopeType?.previousValue &&
      changes.keyScopeType.currentValue !== changes.keyScopeType.previousValue;
    const keyTypeChanged = changes.dataKeyType?.previousValue &&
      changes.dataKeyType.currentValue !== changes.dataKeyType.previousValue;

    if (filterChanged || keyScopeChanged || keyTypeChanged) {
      this.keyControl.setValue('', {emitEvent: false});
    }
  }

  clear(): void {
    this.keyControl.patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.keyInput.nativeElement.blur();
      this.keyInput.nativeElement.focus();
    }, 0);
  }

  registerOnChange(onChange: (value: string) => void): void {
    this.propagateChange = onChange;
  }

  registerOnTouched(_): void {}

  validate(): ValidationErrors | null {
    return this.keyControl.valid ? null : { keyControl: false };
  }

  writeValue(value: string): void {
    this.keyControl.patchValue(value, {emitEvent: false});
  }
}
