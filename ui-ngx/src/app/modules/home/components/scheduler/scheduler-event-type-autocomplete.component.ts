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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, mergeMap, share, startWith, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { SchedulerEventConfigType } from '@home/components/scheduler/scheduler-event-config.models';

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
  }]
})
export class SchedulerEventTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  schedulerEventTypeFormGroup: FormGroup;

  modelValue: string | null;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @Input()
  schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType};

  @ViewChild('schedulerEventTypeInput', {static: true}) schedulerEventTypeInput: ElementRef<HTMLInputElement>;

  filteredSchedulerEventTypes: Observable<Array<SchedulerEventTypeInfo>>;
  schedulerEventTypes: Array<SchedulerEventTypeInfo>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              private fb: FormBuilder) {
    this.schedulerEventTypeFormGroup = this.fb.group({
      schedulerEventType: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
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
        tap(value => {
          this.updateView(value);
        }),
        startWith<string | SchedulerEventTypeInfo>(''),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(schedulerEventType => this.fetchSchedulerEventTypes(schedulerEventType) ),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
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

  onFocus() {
    if (this.dirty) {
      this.schedulerEventTypeFormGroup.get('schedulerEventType').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: SchedulerEventTypeInfo | string | null) {
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

  fetchSchedulerEventTypes(searchText?: string): Observable<Array<SchedulerEventTypeInfo>> {
    this.searchText = searchText;
    let result = this.schedulerEventTypes;
    if (searchText && searchText.length) {
      result = this.schedulerEventTypes.filter((eventTypeInfo) =>
        eventTypeInfo.name.toLowerCase().includes(searchText.toLowerCase()));
      if (!result.length) {
        result = [{ name: searchText, value: searchText }];
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
