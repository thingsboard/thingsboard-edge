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
  Inject,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { EMPTY, forkJoin, Observable, of } from 'rxjs';
import { expand, filter, map, mergeMap, publishReplay, reduce, refCount, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipList } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { RequestConfig } from '@core/http/http-utils';
import { AuthUser } from '@shared/models/user.model';
import { AddEntityGroupsToEdgeDialogData } from '@home/dialogs/add-entity-groups-to-edge-dialog.models';
import { CustomerService } from '@core/http/customer.service';

@Component({
  selector: 'tb-edge-entity-group-list',
  templateUrl: './edge-entity-group-list.component.html',
  styleUrls: ['./edge-entity-group-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EdgeEntityGroupListComponent),
      multi: true
    }
  ]
})
export class EdgeEntityGroupListComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnChanges {

  edgeEntityGroupListFormGroup: FormGroup;

  modelValue: Array<string> | null;

  @Input()
  groupType: EntityType;

  @Input()
  excludeGroupAll: boolean;

  private requiredValue: boolean;
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

  @Input()
  disabled: boolean;

  @ViewChild('entityGroupInput') entityGroupInput: ElementRef<HTMLInputElement>;
  @ViewChild('entityGroupAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipList;

  entityGroups: Array<EntityGroupInfo> = [];
  filteredEntityGroups: Observable<Array<EntityGroupInfo>>;
  ownerId: EntityId;
  edgeId: string;
  tenantId: string;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
              @Inject(MAT_DIALOG_DATA) public data: AddEntityGroupsToEdgeDialogData,
              private fb: FormBuilder) {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser.tenantId;
    this.ownerId = this.data.ownerId;
    this.edgeId = this.data.edgeId;
    this.groupType = this.data.groupType;
    this.edgeEntityGroupListFormGroup = this.fb.group({
      entityGroups: [this.entityGroups, this.required ? [Validators.required] : []],
      entityGroup: [null]
    });
  }

  updateValidators() {
    this.edgeEntityGroupListFormGroup.get('entityGroups').setValidators(this.required ? [Validators.required] : []);
    this.edgeEntityGroupListFormGroup.get('entityGroups').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredEntityGroups = this.edgeEntityGroupListFormGroup.get('entityGroup').valueChanges
      .pipe(
        tap((value) => {
          if (value && typeof value !== 'string') {
            this.add(value);
          } else if (value === null) {
            this.clear(this.entityGroupInput.nativeElement.value);
          }
        }),
        filter((value) => typeof value === 'string'),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchEntityGroups(name) ),
        share()
      );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'groupType') {
          this.reset();
        }
      }
    }
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.edgeEntityGroupListFormGroup.disable({emitEvent: false});
    } else {
      this.edgeEntityGroupListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.entityGroupService.getEntityGroups(this.groupType).subscribe(
        (entityGroups) => {
          this.entityGroups = entityGroups;
          this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
        }
      );
    } else {
      this.entityGroups = [];
      this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  reset() {
    this.entityGroups = [];
    this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
    this.modelValue = null;
    if (this.entityGroupInput) {
      this.entityGroupInput.nativeElement.value = '';
    }
    this.edgeEntityGroupListFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
    this.propagateChange(this.modelValue);
    this.dirty = true;
  }

  add(entityGroup: EntityGroupInfo): void {
    if (!this.modelValue || this.modelValue.indexOf(entityGroup.id.id) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(entityGroup.id.id);
      this.entityGroups.push(entityGroup);
      this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  remove(entityGroup: EntityGroupInfo) {
    const index = this.entityGroups.indexOf(entityGroup);
    if (index >= 0) {
      this.entityGroups.splice(index, 1);
      this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.propagateChange(this.modelValue);
      this.clear();
    }
  }

  displayEntityGroupFn(entityGroup?: EntityGroupInfo): string | undefined {
    return entityGroup ? entityGroup.name : undefined;
  }

  fetchEntityGroups(searchText?: string): Observable<Array<EntityGroupInfo>> {
    this.searchText = searchText;
    return this.getEntityGroups().pipe(
      map((groups) => groups.filter(group => {
        return searchText ? group.name.toUpperCase().startsWith(searchText.toUpperCase()) : true;
      }))
    );
  }

  getEntityGroups(): Observable<Array<EntityGroupInfo>> {
    const entityGroupsTasks: Observable<Array<EntityGroupInfo[]>> = this.getEntityGroupsTasks();
    return entityGroupsTasks.pipe(
      reduce((acc, val) => acc.concat.apply([], val), []),
      map((groups) => {
        if (groups) {
          if (this.excludeGroupAll) {
            return groups.filter(group => !group.groupAll);
          } else {
            return groups;
          }
        } else {
          return [];
        }
      }),
      publishReplay(1),
      refCount()
    );
  }

  onFocus() {
    if (this.dirty) {
      this.edgeEntityGroupListFormGroup.get('entityGroup').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  clear(value: string = '') {
    this.entityGroupInput.nativeElement.value = value;
    this.edgeEntityGroupListFormGroup.get('entityGroup').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entityGroupInput.nativeElement.blur();
      this.entityGroupInput.nativeElement.focus();
    }, 0);
  }

  private getHierarchyEntityIds(): Observable<Array<EntityId>> {
    const currentUserCustomerId = this.getCurrentUserCustomerId();
    return of(this.ownerId).pipe(
      expand((entityId) => {
        if (entityId && entityId.entityType === EntityType.CUSTOMER) {
          return this.customerService.getCustomer(entityId.id, {ignoreLoading: true}).pipe(
            map(customer => {
              if (currentUserCustomerId && customer.id.id === currentUserCustomerId.id) {
                return null; // stop iteration over hierarchy on current customer user
              } else {
                return customer.parentCustomerId;
              }
            })
          );
        } else {
          return EMPTY;
        }
      }),
      reduce((acc, val) => acc.concat(val), []),
    );
  }

  private getCurrentUserCustomerId(): EntityId {
    const currentUser: AuthUser = getCurrentAuthUser(this.store);
    if (currentUser.authority === Authority.TENANT_ADMIN) {
      return null;
    } else {
      return {
        entityType: EntityType.CUSTOMER,
        id: currentUser.customerId
      };
    }
  }

  private getEntityGroupsTasks(): Observable<Array<EntityGroupInfo[]>> {
    return this.getHierarchyEntityIds().pipe(
      mergeMap((entityIds) => this.createEntityGroupTasks(entityIds))
    );
  }

  private createEntityGroupTasks(entityIds: Array<EntityId>): Observable<Array<EntityGroupInfo[]>> {
    const tasks: Array<Observable<Array<EntityGroupInfo>>> = [];
    entityIds.forEach(entityId => {
      if (entityId) {
        tasks.push(this.getGroupsByOwnerId(entityId, this.groupType, {ignoreLoading: true}));
      }
    });
    const currentUser: AuthUser = getCurrentAuthUser(this.store);
    if (this.ownerId.entityType === EntityType.CUSTOMER && currentUser.authority === Authority.TENANT_ADMIN) {
      const tenantId: EntityId = {
        entityType: EntityType.TENANT,
        id: currentUser.tenantId
      };
      tasks.push(this.getGroupsByOwnerId(tenantId, this.groupType, {ignoreLoading: true}));
    }
    return forkJoin(tasks);
  }

  private getGroupsByOwnerId(ownerId: EntityId, groupType: EntityType, config: RequestConfig): Observable<EntityGroupInfo[]> {
    return this.entityGroupService.getEntityGroupsByOwnerId(ownerId.entityType as EntityType, ownerId.id, groupType, config);
  }

}
