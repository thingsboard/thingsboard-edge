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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, mergeMap, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupService } from '@core/http/entity-group.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { ContactBased } from '@shared/models/contact-based.model';

@Component({
  selector: 'tb-owner-autocomplete',
  templateUrl: './owner-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => OwnerAutocompleteComponent),
    multi: true
  }]
})
export class OwnerAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  selectOwnerFormGroup: FormGroup;

  modelValue: EntityId | null;

  @Input()
  excludeOwnerIds: Array<string>;

  @Input()
  placeholderText: string;

  @Input()
  notFoundText: string;

  @Input()
  requiredText: string;

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

  @ViewChild('ownerInput', {static: true}) ownerInput: ElementRef<HTMLInputElement>;

  filteredOwners: Observable<Array<ContactBased<EntityId>>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityService: EntityService,
              private entityGroupService: EntityGroupService,
              private fb: FormBuilder) {
    this.selectOwnerFormGroup = this.fb.group({
      owner: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredOwners = this.selectOwnerFormGroup.get('owner').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        // startWith<string | BaseData<EntityId>>(''),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchOwners(name) ),
        share()
      );
  }

  ngAfterViewInit(): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectOwnerFormGroup.disable({emitEvent: false});
    } else {
      this.selectOwnerFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: EntityId | null): void {
    this.searchText = '';
    if (value !== null && value.entityType && value.id) {
        const targetEntityType = value.entityType as EntityType;
        this.entityService.getEntity(targetEntityType, value.id, {ignoreLoading: true}).subscribe(
          (owner) => {
            this.modelValue = owner.id;
            this.selectOwnerFormGroup.get('owner').patchValue(owner, {emitEvent: false});
          }
        );
    } else {
      this.modelValue = null;
      this.selectOwnerFormGroup.get('owner').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectOwnerFormGroup.get('owner').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  reset() {
    this.selectOwnerFormGroup.get('owner').patchValue('', {emitEvent: false});
  }

  updateView(value: EntityId | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayOwnerFn(owner?: ContactBased<EntityId>): string | undefined {
    return owner ? owner.name : undefined;
  }

  fetchOwners(searchText?: string): Observable<Array<ContactBased<EntityId>>> {
    this.searchText = searchText;
    let limit = 50;
    if (this.excludeOwnerIds && this.excludeOwnerIds.length) {
      limit += this.excludeOwnerIds.length;
    }
    const pageLink = new PageLink(limit, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.entityGroupService.getOwners(pageLink, {ignoreLoading: true}).pipe(
      map((data) => {
          if (data) {
            if (this.excludeOwnerIds && this.excludeOwnerIds.length) {
              const owners: Array<ContactBased<EntityId>> = [];
              data.data.forEach((owner) => {
                if (this.excludeOwnerIds.indexOf(owner.id.id) === -1) {
                  owners.push(owner);
                }
              });
              return owners;
            } else {
              return data.data;
            }
          } else {
            return [];
          }
        }
      ));
  }

  clear() {
    this.selectOwnerFormGroup.get('owner').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.ownerInput.nativeElement.blur();
      this.ownerInput.nativeElement.focus();
    }, 0);
  }

}
