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

import {
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId, entityIdsEquals } from '@shared/models/id/entity-id';
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
export class OwnerAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnChanges {

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
  private excludeOwnerIdsChanged = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
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
        debounceTime(150),
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
        distinctUntilChanged((name1, name2) => {
          if (this.excludeOwnerIdsChanged) {
            this.excludeOwnerIdsChanged = false;
            return false;
          } else {
            return name1 === name2;
          }
        }),
        switchMap(name => this.fetchOwners(name) ),
        share()
      );
  }

  ngAfterViewInit(): void {}

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'excludeOwnerIds' && !entityIdsEquals(change.currentValue, change.previousValue)) {
          this.dirty = true;
          this.excludeOwnerIdsChanged = true;
        }
      }
    }
  }

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
      catchError(() => of(null)),
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
