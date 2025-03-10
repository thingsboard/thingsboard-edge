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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
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
import { Observable, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, share, startWith, switchMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { SchedulerEventConfigType } from '@home/components/scheduler/scheduler-event-config.models';
import { coerceBoolean } from '@shared/decorators/coercion';

interface SchedulerEventTypeInfo {
  name: string;
  value: string;
}

@Component({
  selector: 'tb-scheduler-event-type-autocomplete',
  templateUrl: './scheduler-event-type-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SchedulerEventTypeAutocompleteComponent),
    multi: true
  }, {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => SchedulerEventTypeAutocompleteComponent),
    multi: true
  }]
})
export class SchedulerEventTypeAutocompleteComponent implements ControlValueAccessor, OnInit, Validator{

  schedulerEventTypeFormGroup: FormGroup;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType};

  @Input()
  label = this.translate.instant('scheduler.event-type');

  @Input()
  placeholder = this.translate.instant('scheduler.select-event-type');

  @ViewChild('schedulerEventTypeInput', {static: true}) schedulerEventTypeInput: ElementRef<HTMLInputElement>;

  filteredSchedulerEventTypes: Observable<Array<SchedulerEventTypeInfo>>;

  searchText = '';

  private dirty = false;
  private schedulerEventTypes: Array<SchedulerEventTypeInfo>;
  private modelValue: string | null;

  private propagateChange: (value: any) => void = () => {};

  constructor(private translate: TranslateService,
              private fb: FormBuilder) {
    this.schedulerEventTypeFormGroup = this.fb.group({
      schedulerEventType: [null, Validators.maxLength(255)]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.schedulerEventTypes = [];
    Object.keys(this.schedulerEventConfigTypes).forEach(key => {
      this.schedulerEventTypes.push(
        {
          name: this.schedulerEventConfigTypes[key].name,
          value: key
        }
      );
    });

    this.filteredSchedulerEventTypes = this.schedulerEventTypeFormGroup.get('schedulerEventType').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          this.updateView(value);
        }),
        startWith<string | SchedulerEventTypeInfo>(''),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(schedulerEventType => this.fetchSchedulerEventTypes(schedulerEventType)),
        share()
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.schedulerEventTypeFormGroup.disable({emitEvent: false});
    } else {
      this.schedulerEventTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value;
    let schedulerEventTypeInfo = this.schedulerEventTypes.find(eventType => eventType.value === value);
    if (schedulerEventTypeInfo) {
      this.modelValue = schedulerEventTypeInfo.value;
    } else if (value) {
      schedulerEventTypeInfo = {
        value,
        name: value
      };
    }
    this.schedulerEventTypeFormGroup.get('schedulerEventType').patchValue(schedulerEventTypeInfo, {emitEvent: false});
    this.dirty = true;
  }

  validate(): ValidationErrors | null {
    return this.schedulerEventTypeFormGroup.valid ? null : {
      schedulerEventTypeFormGroup: false
    };
  }

  onFocus() {
    if (this.dirty) {
      this.schedulerEventTypeFormGroup.get('schedulerEventType').updateValueAndValidity({
        onlySelf: true,
        emitEvent: true
      });
      this.dirty = false;
    }
  }

  private updateView(value: SchedulerEventTypeInfo | string | null) {
    let res: string;
    if (value && typeof value !== 'string') {
      res = value.value;
    } else {
      res = null;
    }
    if (this.modelValue !== res) {
      this.modelValue = res;
      this.propagateChange(this.modelValue);
    }
  }

  displaySchedulerEventTypeFn(eventType?: SchedulerEventTypeInfo): string | undefined {
    if (eventType) {
      return eventType.name;
    }
    return undefined;
  }

  private fetchSchedulerEventTypes(searchText?: string): Observable<Array<SchedulerEventTypeInfo>> {
    this.searchText = searchText;
    let result = this.schedulerEventTypes;
    if (searchText && searchText.length) {
      result = this.schedulerEventTypes.filter((eventTypeInfo) =>
        eventTypeInfo.name.toLowerCase().includes(searchText.toLowerCase()));
      if (!result.length && searchText.length < 256) {
        result = [{name: searchText, value: searchText}];
      }
    }
    return of(result);
  }

  clear(value: string = '') {
    this.schedulerEventTypeInput.nativeElement.value = value;
    this.schedulerEventTypeFormGroup.get('schedulerEventType').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.schedulerEventTypeInput.nativeElement.blur();
      this.schedulerEventTypeInput.nativeElement.focus();
    }, 0);
  }

}
