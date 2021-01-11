///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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

  resourceTypeFormGroup: FormGroup;

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
      }
    }).sort(this.sortResource);

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              private fb: FormBuilder) {
    this.resourceTypeFormGroup = this.fb.group({
      resourceType: [null]
    });
  }

  private sortResource(a: ResourceTypeInfo, b: ResourceTypeInfo): number{
    if (a.value === 'ALL' || b.value === 'ALL') return a.value === 'ALL' ? -1 : 1;
    if (a.name > b.name) return 1;
    if (a.name < b.name) return -1;
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
        tap(value => {
          this.updateView(value);
        }),
        startWith<string | ResourceTypeInfo>(''),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(resource => this.fetchResources(resource) ),
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
    } else {
      res = null;
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
