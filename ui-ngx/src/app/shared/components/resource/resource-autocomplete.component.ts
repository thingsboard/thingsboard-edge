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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { isDefinedAndNotNull, isEmptyStr, isEqual, isObject } from '@core/utils';
import { ResourceInfo, ResourceType } from '@shared/models/resource.models';
import { TbResourceId } from '@shared/models/id/tb-resource-id';
import { ResourceService } from '@core/http/resource.service';
import { PageLink } from '@shared/models/page/page-link';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';

@Component({
  selector: 'tb-resource-autocomplete',
  templateUrl: './resource-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ResourceAutocompleteComponent),
    multi: true
  }]
})
export class ResourceAutocompleteComponent implements ControlValueAccessor, OnInit {

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  placeholder: string;

  @Input()
  @coerceBoolean()
  hideRequiredMarker = false;

  @Input()
  @coerceBoolean()
  allowAutocomplete = false;

  resourceFormGroup = this.fb.group({
    resource: [null]
  });

  filteredResources$: Observable<Array<ResourceInfo>>;

  searchText = '';

  @ViewChild('resourceInput', {static: true}) resourceInput: ElementRef;

  private modelValue: string | TbResourceId;
  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private resourceService: ResourceService) {
  }

  ngOnInit(): void {
    if(this.required) {
      this.resourceFormGroup.get('resource').setValidators(Validators.required);
      this.resourceFormGroup.get('resource').updateValueAndValidity({emitEvent: false});
    }
    this.filteredResources$ = this.resourceFormGroup.get('resource').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (isObject(value)) {
            modelValue = value.id;
          } else if (isEmptyStr(value)) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.title) : ''),
        switchMap(name => this.fetchResources(name) ),
        share()
      );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.resourceFormGroup.disable({emitEvent: false});
    } else {
      this.resourceFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | TbResourceId) {
    if (isDefinedAndNotNull(value)) {
      this.searchText = '';
      if (isObject(value) && typeof value !== 'string' && (value as TbResourceId).id) {
        this.resourceService.getResourceInfo(value.id, {ignoreLoading: true, ignoreErrors: true}).subscribe({
          next: resource => {
            this.modelValue = resource.id;
            this.resourceFormGroup.get('resource').patchValue(resource, {emitEvent: false});
          },
          error: () => {
            this.modelValue = '';
            this.resourceFormGroup.get('resource').patchValue('');
          }
        });
      } else {
        this.modelValue = value;
        this.resourceFormGroup.get('resource').patchValue(value, {emitEvent: false});
      }
      this.dirty = true;
    }
  }

  displayResourceFn(resource?: ResourceInfo | string): string {
    return isObject(resource) ? (resource as ResourceInfo).title : resource as string;
  }

  clear() {
    this.resourceFormGroup.get('resource').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.resourceInput.nativeElement.blur();
      this.resourceInput.nativeElement.focus();
    }, 0);
  }

  onFocus() {
    if (this.dirty) {
      this.resourceFormGroup.get('resource').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  private updateView(value: string | TbResourceId ) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  private fetchResources(searchText?: string): Observable<Array<ResourceInfo>> {
    this.searchText = searchText;
    return this.resourceService.getResources(new PageLink(50, 0, searchText), ResourceType.JS_MODULE, {ignoreLoading: true}).pipe(
      catchError(() => of(null)),
      map(data => data.data)
    );
  }

}
