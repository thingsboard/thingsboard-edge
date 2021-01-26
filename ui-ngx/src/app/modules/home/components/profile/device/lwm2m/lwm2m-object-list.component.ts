///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
  Component,
  forwardRef,
  Input,
  OnInit,
  ViewChild,
  ElementRef,
  Output,
  EventEmitter
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR, Validators
} from '@angular/forms';
import {coerceBooleanProperty} from '@angular/cdk/coercion';
import {Store} from '@ngrx/store';
import {AppState} from '../../../../../../core/core.state';
import {MatChipList} from '@angular/material/chips';
import {MatAutocomplete} from '@angular/material/autocomplete';
import {Observable} from 'rxjs';
import {filter, map, mergeMap, share, tap} from 'rxjs/operators';
import {ObjectLwM2M} from './profile-config.models';
import {TranslateService} from '@ngx-translate/core';
import {DeviceProfileService} from '../../../../../../core/http/device-profile.service';
import {PageLink} from '../../../../../../shared/models/page/page-link';
import {Direction} from '../../../../../../shared/models/page/sort-order';

@Component({
  selector: 'tb-profile-lwm2m-object-list',
  templateUrl: './lwm2m-object-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObjectListComponent),
      multi: true
    }]
})
export class Lwm2mObjectListComponent implements ControlValueAccessor, OnInit, Validators {

  lwm2mObjectListFormGroup: FormGroup;
  private requiredValue: boolean;
  modelValue: Array<number> | null;
  objectsList: Array<ObjectLwM2M> = [];
  filteredObjectsList: Observable<Array<ObjectLwM2M>>;
  // TODO: voba - check this fix
  // private disabled = false as boolean;
  disabled = false as boolean;
  searchText = '' as string;
  private dirty = false as boolean;Lwm2mObjectAddInstancesListComponent

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

  @Output()
  addList = new EventEmitter<any>();

  @Output()
  removeList = new EventEmitter<any>();

  @ViewChild('objectInput') objectInput: ElementRef<HTMLInputElement>;
  @ViewChild('objectAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipList;

  private propagateChange = (v: any) => {
  };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private deviceProfileService: DeviceProfileService,
              private fb: FormBuilder) {
    this.lwm2mObjectListFormGroup = this.fb.group({
      objectsList: [this.objectsList],
      objectLwm2m: [null, this.required ? [Validators.required] : []]
    });
  }

  updateValidators() {
    this.lwm2mObjectListFormGroup.get('objectLwm2m').setValidators(this.required ? [Validators.required] : []);
    this.lwm2mObjectListFormGroup.get('objectLwm2m').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredObjectsList = this.lwm2mObjectListFormGroup.get('objectLwm2m').valueChanges
      .pipe(
        tap((value) => {
          if (value && typeof value !== 'string') {
            this.add(value);
          } else if (value === null) {
            this.clear(this.objectInput.nativeElement.value);
          }
        }),
        filter((value) => typeof value === 'string'),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchListObjects(name)),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.lwm2mObjectListFormGroup.disable({emitEvent: false});
    } else {
      this.lwm2mObjectListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: any): void {
    this.searchText = '';
    if (value.hasOwnProperty("objectIds") && value["objectIds"] != null && value["objectIds"].length > 0) {
      this.modelValue = [...value["objectIds"]];
      this.objectsList = value["objectsList"];
      this.lwm2mObjectListFormGroup.get('objectsList').setValue(this.objectsList);
    } else {
      this.objectsList = [];
      this.lwm2mObjectListFormGroup.get('objectsList').setValue(this.objectsList);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  reset() {
    this.objectsList = [];
    this.lwm2mObjectListFormGroup.get('objectsList').setValue(this.objectsList);
    this.modelValue = null;
    if (this.objectInput) {
      this.objectInput.nativeElement.value = '';
    }
    this.lwm2mObjectListFormGroup.get('objectLwm2m').patchValue('', {emitEvent: false});
    this.propagateChange(this.modelValue);
    this.dirty = true;
  }

  add(object: ObjectLwM2M): void {
    if (!this.modelValue || this.modelValue.indexOf(object.id) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(object.id);
      this.objectsList.push(object);
      this.lwm2mObjectListFormGroup.get('objectsList').setValue(this.objectsList);
      this.addList.next(this.objectsList);
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  remove(object: ObjectLwM2M) {
    let index = this.objectsList.indexOf(object);
    if (index >= 0) {
      this.objectsList.splice(index, 1);
      this.lwm2mObjectListFormGroup.get('objectsList').setValue(this.objectsList);
      index = this.modelValue.indexOf(object.id);
      this.modelValue.splice(index, 1);
      this.removeList.next(object);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.propagateChange(this.modelValue);
      this.clear();
    }
  }

  displayObjectLwm2mFn(object?: ObjectLwM2M): string | undefined {
    return object ? object.name : undefined;
  }

  fetchListObjects(searchText?: string): Observable<Array<ObjectLwM2M>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.deviceProfileService.getLwm2mObjectsPage(pageLink, {ignoreLoading: true}).pipe(
      map(pageData => {
        let data = pageData.data;
        return data;
      })
    );
  }

  onFocus() {
    if (this.dirty) {
      this.lwm2mObjectListFormGroup.get('objectLwm2m').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  clear(value: string = '') {
    this.objectInput.nativeElement.value = value;
    this.lwm2mObjectListFormGroup.get('objectLwm2m').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.objectInput.nativeElement.blur();
      this.objectInput.nativeElement.focus();
    }, 0);
  }
}
