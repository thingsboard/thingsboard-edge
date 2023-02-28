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

import { Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Observable } from 'rxjs';
import { distinctUntilChanged, filter, mergeMap, share, tap } from 'rxjs/operators';
import { ObjectLwM2M, PAGE_SIZE_LIMIT } from './lwm2m-profile-config.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { Direction } from '@shared/models/page/sort-order';
import { isDefined, isDefinedAndNotNull, isString } from '@core/utils';
import { PageLink } from '@shared/models/page/page-link';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';

@Component({
  selector: 'tb-profile-lwm2m-object-list',
  templateUrl: './lwm2m-object-list.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObjectListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mObjectListComponent),
      multi: true
    }
  ]
})
export class Lwm2mObjectListComponent implements ControlValueAccessor, OnInit, Validator {

  private requiredValue: boolean;
  private dirty = false;

  lwm2mListFormGroup: FormGroup;
  objectsList: Array<ObjectLwM2M> = [];
  filteredObjectsList: Observable<Array<ObjectLwM2M>>;
  disabled = false;
  searchText = '';

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
    this.updateValidators();
  }

  @Output()
  addList = new EventEmitter<any>();

  @Output()
  removeList = new EventEmitter<any>();

  @ViewChild('objectInput') objectInput: ElementRef<HTMLInputElement>;

  private propagateChange = (v: any) => {
  }

  constructor(public truncate: TruncatePipe,
              private deviceProfileService: DeviceProfileService,
              private fb: FormBuilder) {
    this.lwm2mListFormGroup = this.fb.group({
      objectsList: [this.objectsList],
      objectLwm2m: ['']
    });
    this.lwm2mListFormGroup.valueChanges.subscribe((value) => {
      let formValue = null;
      if (this.lwm2mListFormGroup.valid) {
        formValue = value.objectsList;
      }
      this.propagateChange(formValue);
    });
  }

  private updateValidators = (): void => {
    this.lwm2mListFormGroup.get('objectsList').setValidators(this.required ? [Validators.required] : []);
    this.lwm2mListFormGroup.get('objectsList').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredObjectsList = this.lwm2mListFormGroup.get('objectLwm2m').valueChanges
      .pipe(
        tap((value) => {
          if (value && !isString(value)) {
            this.add(value);
          } else if (value === null) {
            this.clear(this.objectInput.nativeElement.value);
          }
        }),
        filter(searchText => isString(searchText)),
        distinctUntilChanged(),
        mergeMap(searchText => this.fetchListObjects(searchText)),
        share()
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.lwm2mListFormGroup.disable({emitEvent: false});
      if (isDefined(this.objectInput)) {
        this.clear('', false);
      }
    } else {
      this.lwm2mListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ObjectLwM2M[]): void {
    this.searchText = '';
    if (isDefinedAndNotNull(value)) {
      if (Array.isArray(value)) {
        this.objectsList = value;
      } else {
        this.objectsList = [];
      }
      this.lwm2mListFormGroup.patchValue({objectsList: this.objectsList}, {emitEvent: false});
      this.dirty = false;
    }
  }

  validate(): ValidationErrors | null {
    return this.lwm2mListFormGroup.valid ? null : {
      lwm2mListObj: false
    };
  }

  private add(object: ObjectLwM2M): void {
    if (isDefinedAndNotNull(this.objectsList) && this.objectsList.findIndex(item => item.keyId === object.keyId) === -1) {
      this.objectsList.push(object);
      this.lwm2mListFormGroup.get('objectsList').setValue(this.objectsList);
      this.addList.next(this.objectsList);
    }
    this.clear();
  }

  remove = (object: ObjectLwM2M): void => {
    const index = this.objectsList.indexOf(object);
    if (index >= 0) {
      this.objectsList.splice(index, 1);
      this.lwm2mListFormGroup.get('objectsList').setValue(this.objectsList);
      this.removeList.next(object);
      this.clear();
    }
  }

  displayObjectLwm2mFn = (object?: ObjectLwM2M): string => {
    return object ? object.name : '';
  }

  private fetchListObjects = (searchText: string): Observable<Array<ObjectLwM2M>> =>  {
    this.searchText = searchText;
    const pageLink = new PageLink(PAGE_SIZE_LIMIT, 0, this.searchText, {
      property: 'id',
      direction: Direction.ASC
    });
    return this.deviceProfileService.getLwm2mObjectsPage(pageLink);
  }

  onFocus = (): void => {
    if (!this.dirty) {
      this.lwm2mListFormGroup.get('objectLwm2m').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = true;
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  private clear(value = '', emitEvent = true) {
    this.objectInput.nativeElement.value = value;
    this.lwm2mListFormGroup.get('objectLwm2m').patchValue(value, {emitEvent});
    if (emitEvent) {
      setTimeout(() => {
        this.objectInput.nativeElement.blur();
        this.objectInput.nativeElement.focus();
      }, 0);
    }
  }
}
