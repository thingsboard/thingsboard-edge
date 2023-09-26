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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import {
  debounceTime,
  distinctUntilChanged,
  map,
  publishReplay,
  refCount,
  startWith,
  switchMap,
  tap
} from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Resource, resourceTypeTranslationMap } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

interface ResourceTypeInfo {
  name: string;
  value: Resource;
}

@Component({
  selector: 'tb-resource-type-autocomplete',
  templateUrl: './resource-type-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ResourceTypeAutocompleteComponent),
    multi: true
  }]
})
export class ResourceTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  resourceTypeFormGroup: UntypedFormGroup;

  modelValue: Resource | null;

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

  @ViewChild('resourceTypeInput', {static: true}) resourceTypeInput: ElementRef<HTMLInputElement>;

  filteredResources: Observable<Array<ResourceTypeInfo>>;
  resources: Array<ResourceTypeInfo> = this.userPermissionsService
    .getAllowedResources().map(resource => {
      return {
        name: this.translate.instant(resourceTypeTranslationMap.get(resource)),
        value: resource
      };
    }).sort(this.sortResource);

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              private fb: UntypedFormBuilder) {
    this.resourceTypeFormGroup = this.fb.group({
      resourceType: [null]
    });
  }

  private sortResource(a: ResourceTypeInfo, b: ResourceTypeInfo): number {
    if (a.value === 'ALL' || b.value === 'ALL') { return a.value === 'ALL' ? -1 : 1; }
    if (a.name > b.name) { return 1; }
    if (a.name < b.name) { return -1; }
    return 0;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredResources = this.resourceTypeFormGroup.get('resourceType').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          this.updateView(value);
        }),
        startWith<string | ResourceTypeInfo>(''),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(resource => this.fetchResources(resource) ),
        publishReplay(1),
        refCount()
      );
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.resourceTypeFormGroup.disable({emitEvent: false});
    } else {
      this.resourceTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Resource | null): void {
    this.searchText = '';
    const resourceTypeInfo = this.resources.find(resource => resource.value === value);
    if (resourceTypeInfo) {
      this.modelValue = resourceTypeInfo.value;
    } else {
      this.modelValue = null;
    }
    this.resourceTypeFormGroup.get('resourceType').patchValue(resourceTypeInfo, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.resourceTypeFormGroup.get('resourceType').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: ResourceTypeInfo | string | null) {
    let res: Resource = null;
    if (value && typeof value !== 'string') {
      res = value.value;
    }
    if (this.modelValue !== res) {
      this.modelValue = res;
      this.propagateChange(this.modelValue);
    }
  }

  displayResourceTypeFn(resource?: ResourceTypeInfo): string | undefined {
    if (resource) {
      return resource.name;
    }
    return undefined;
  }

  fetchResources(searchText?: string): Observable<Array<ResourceTypeInfo>> {
    this.searchText = searchText;
    let result = this.resources;
    if (searchText && searchText.length) {
      result = this.resources.filter((resourceTypeInfo) =>
        resourceTypeInfo.name.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
  }

  clear(value: string = '') {
    this.resourceTypeInput.nativeElement.value = value;
    this.resourceTypeFormGroup.get('resourceType').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.resourceTypeInput.nativeElement.blur();
      this.resourceTypeInput.nativeElement.focus();
    }, 0);
  }

}
