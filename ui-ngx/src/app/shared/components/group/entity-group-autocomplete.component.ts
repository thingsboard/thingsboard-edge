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
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { merge, Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupService } from '@core/http/entity-group.service';
import { isEqual, isString } from '@core/utils';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityInfoData } from '@shared/models/entity.models';

@Component({
  selector: 'tb-entity-group-autocomplete',
  templateUrl: './entity-group-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityGroupAutocompleteComponent),
    multi: true
  }]
})
export class EntityGroupAutocompleteComponent implements ControlValueAccessor, OnInit, OnChanges, OnDestroy {

  selectEntityGroupFormGroup: UntypedFormGroup;

  modelValue: string | null = null;

  @Input()
  groupType: EntityType;

  private ownerIdValue: EntityId | null = null;
  get ownerId(): EntityId {
    return this.ownerIdValue;
  }

  @Input()
  set ownerId(value: EntityId | null) {
    if (!isEqual(this.ownerIdValue, value)) {
      const currentEntityGroup = this.getCurrentEntityGroup();
      const keepEntityGroup = currentEntityGroup === null;
      this.reset(keepEntityGroup);
    }
    this.ownerIdValue = value;
  }

  @Input()
  excludeGroupIds: Array<string>;

  @Input()
  excludeGroupAll: boolean;

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

  @Output()
  entityGroupLoaded = new EventEmitter<EntityInfoData>();

  @ViewChild('entityGroupInput', {static: true}) entityGroupInput: ElementRef<HTMLInputElement>;

  filteredEntityGroups: Observable<Array<EntityInfoData>>;

  allEntityGroups: Observable<Array<EntityInfoData>>;

  searchText = '';

  private pristine = true;
  private cleanFilteredEntityGroups: Subject<Array<EntityInfoData>> = new Subject();

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityGroupService: EntityGroupService,
              private fb: UntypedFormBuilder) {
    this.selectEntityGroupFormGroup = this.fb.group({
      entityGroup: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    const getEntityGroups =  this.selectEntityGroupFormGroup.get('entityGroup').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id.id;
          }
          this.updateView(modelValue, value);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        switchMap(name => this.fetchEntityGroups(name)),
        share()
      );

    this.filteredEntityGroups = merge(
      this.cleanFilteredEntityGroups,
      getEntityGroups
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && !isEqual(change.currentValue, change.previousValue)) {
        if (propName === 'groupType') {
          const currentEntityGroup = this.getCurrentEntityGroup();
          if (!currentEntityGroup) {
            this.reset();
          }
        }
      }
    }
  }

  ngOnDestroy() {
    this.cleanFilteredEntityGroups.complete();
    this.cleanFilteredEntityGroups = null;
  }

  getCurrentEntityGroup(): EntityInfoData | null {
    const currentEntityGroup = this.selectEntityGroupFormGroup.get('entityGroup').value;
    if (currentEntityGroup && typeof currentEntityGroup !== 'string') {
      return currentEntityGroup as EntityInfoData;
    } else {
      return null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectEntityGroupFormGroup.disable({emitEvent: false});
    } else {
      this.selectEntityGroupFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | EntityInfoData | null): void {
    this.searchText = '';
    if (value !== null) {
      if ((value as EntityInfoData).id) {
        const entityGroup = value as EntityInfoData;
        this.modelValue = entityGroup.id.id;
        this.selectEntityGroupFormGroup.get('entityGroup').patchValue(entityGroup, {emitEvent: false});
        this.entityGroupLoaded.next(entityGroup);
      } else {
        this.entityGroupService.getEntityGroupEntityInfo(value as string, {ignoreLoading: true}).subscribe({
          next: (entityGroup) => {
            this.modelValue = entityGroup.id.id;
            this.selectEntityGroupFormGroup.get('entityGroup').patchValue(entityGroup, {emitEvent: false});
            this.entityGroupLoaded.next(entityGroup);
          },
          error: () => {
            this.modelValue = null;
            this.selectEntityGroupFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
            this.entityGroupLoaded.next(null);
          }
        });
      }
    } else {
      this.modelValue = null;
      this.selectEntityGroupFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
      this.entityGroupLoaded.next(null);
    }
    this.pristine = true;
  }

  onFocus() {
    if (this.pristine) {
      this.selectEntityGroupFormGroup.get('entityGroup').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.pristine = false;
    }
  }

  reset(keepEntityGroup = false) {
    this.cleanFilteredEntityGroups.next([]);
    this.allEntityGroups = null;
    this.pristine = true;
    if (!keepEntityGroup) {
      this.selectEntityGroupFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
      setTimeout(() => this.updateView(null, this.getCurrentEntityGroup()));
    }
  }

  updateView(value: string | null, entityGroup: EntityInfoData | string | null ) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
      if (!(isString(entityGroup) || entityGroup === null)) {
        // @ts-ignore
        this.entityGroupLoaded.next(entityGroup);
      }
    }
  }

  displayEntityGroupFn(entityGroup?: EntityInfoData): string | undefined {
    return entityGroup ? entityGroup.name : undefined;
  }

  fetchEntityGroups(searchText?: string): Observable<Array<EntityInfoData>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.getEntityGroups(pageLink).pipe(
      catchError(() => of(emptyPageData<EntityInfoData>())),
      map(pageData => {
        let data = pageData.data;
        if (this.excludeGroupAll) {
          data = data.filter(group => group.name !== 'All');
        }
        if (this.excludeGroupIds && this.excludeGroupIds.length) {
          const groups: Array<EntityInfoData> = [];
          data.forEach((group) => {
            if (this.excludeGroupIds.indexOf(group.id.id) === -1) {
              groups.push(group);
            }
          });
          return groups;
        } else {
          return data;
        }
      })
    );
  }

  getEntityGroups(pageLink: PageLink): Observable<PageData<EntityInfoData>> {
    if (this.ownerId) {
      return this.entityGroupService
        .getEntityGroupEntityInfosByOwnerId(pageLink, this.ownerId.entityType as EntityType,
          this.ownerId.id, this.groupType, {ignoreLoading: true});
    } else {
      return this.entityGroupService.getEntityGroupEntityInfos(pageLink, this.groupType, true, {ignoreLoading: true});
    }
  }

  clear() {
    this.selectEntityGroupFormGroup.get('entityGroup').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.entityGroupInput.nativeElement.blur();
      this.entityGroupInput.nativeElement.focus();
    }, 0);
  }

}
