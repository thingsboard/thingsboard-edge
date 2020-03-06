///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import {AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild, OnDestroy} from '@angular/core';
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR} from '@angular/forms';
import {Observable, of, throwError, Subscription} from 'rxjs';
import {PageLink} from '@shared/models/page/page-link';
import {Direction} from '@shared/models/page/sort-order';
import {filter, map, mergeMap, publishReplay, refCount, startWith, tap, publish} from 'rxjs/operators';
import {PageData, emptyPageData} from '@shared/models/page/page-data';
import {DashboardInfo} from '@app/shared/models/dashboard.models';
import {DashboardId} from '@app/shared/models/id/dashboard-id';
import {DashboardService} from '@core/http/dashboard.service';
import {Store} from '@ngrx/store';
import {AppState} from '@app/core/core.state';
import {getCurrentAuthUser} from '@app/core/auth/auth.selectors';
import {Authority} from '@shared/models/authority.enum';
import {TranslateService} from '@ngx-translate/core';
import {DeviceService} from '@core/http/device.service';
import {EntitySubtype, EntityType} from '@app/shared/models/entity-type.models';
import {BroadcastService} from '@app/core/services/broadcast.service';
import {coerceBooleanProperty} from '@angular/cdk/coercion';
import {AssetService} from '@core/http/asset.service';
import {EntityViewService} from '@core/http/entity-view.service';

@Component({
  selector: 'tb-entity-subtype-autocomplete',
  templateUrl: './entity-subtype-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntitySubTypeAutocompleteComponent),
    multi: true
  }]
})
export class EntitySubTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  subTypeFormGroup: FormGroup;

  modelValue: string | null;

  @Input()
  entityType: EntityType;

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

  @ViewChild('subTypeInput', {static: true}) subTypeInput: ElementRef;

  selectEntitySubtypeText: string;
  entitySubtypeText: string;
  entitySubtypeRequiredText: string;

  filteredSubTypes: Observable<Array<string>>;

  subTypes: Observable<Array<string>>;

  private broadcastSubscription: Subscription;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              public translate: TranslateService,
              private deviceService: DeviceService,
              private assetService: AssetService,
              private entityViewService: EntityViewService,
              private fb: FormBuilder) {
    this.subTypeFormGroup = this.fb.group({
      subType: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {

    switch (this.entityType) {
      case EntityType.ASSET:
        this.selectEntitySubtypeText = 'asset.select-asset-type';
        this.entitySubtypeText = 'asset.asset-type';
        this.entitySubtypeRequiredText = 'asset.asset-type-required';
        this.broadcastSubscription = this.broadcast.on('assetSaved', () => {
          this.subTypes = null;
        });
        break;
      case EntityType.DEVICE:
        this.selectEntitySubtypeText = 'device.select-device-type';
        this.entitySubtypeText = 'device.device-type';
        this.entitySubtypeRequiredText = 'device.device-type-required';
        this.broadcastSubscription = this.broadcast.on('deviceSaved', () => {
          this.subTypes = null;
        });
        break;
      case EntityType.ENTITY_VIEW:
        this.selectEntitySubtypeText = 'entity-view.select-entity-view-type';
        this.entitySubtypeText = 'entity-view.entity-view-type';
        this.entitySubtypeRequiredText = 'entity-view.entity-view-type-required';
        this.broadcastSubscription = this.broadcast.on('entityViewSaved', () => {
          this.subTypes = null;
        });
        break;
    }

    this.filteredSubTypes = this.subTypeFormGroup.get('subType').valueChanges
      .pipe(
        tap(value => {
            this.updateView(value);
        }),
        // startWith<string | EntitySubtype>(''),
        map(value => value ? value : ''),
        mergeMap(type => this.fetchSubTypes(type) )
      );
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
    if (this.broadcastSubscription) {
      this.broadcastSubscription.unsubscribe();
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.subTypeFormGroup.disable({emitEvent: false});
    } else {
      this.subTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value;
    this.subTypeFormGroup.get('subType').patchValue(value, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.subTypeFormGroup.get('subType').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displaySubTypeFn(subType?: string): string | undefined {
    return subType ? subType : undefined;
  }

  fetchSubTypes(searchText?: string, strictMatch: boolean = false): Observable<Array<string>> {
    this.searchText = searchText;
    return this.getSubTypes().pipe(
      map(subTypes => subTypes.filter( subType => {
        if (strictMatch) {
          return searchText ? subType === searchText : false;
        } else {
          return searchText ? subType.toUpperCase().startsWith(searchText.toUpperCase()) : true;
        }
      }))
    );
  }

  getSubTypes(): Observable<Array<string>> {
    if (!this.subTypes) {
      let subTypesObservable: Observable<Array<EntitySubtype>>;
      switch (this.entityType) {
        case EntityType.ASSET:
          subTypesObservable = this.assetService.getAssetTypes({ignoreLoading: true});
          break;
        case EntityType.DEVICE:
          subTypesObservable = this.deviceService.getDeviceTypes({ignoreLoading: true});
          break;
        case EntityType.ENTITY_VIEW:
          subTypesObservable = this.entityViewService.getEntityViewTypes({ignoreLoading: true});
          break;
      }
      if (subTypesObservable) {
        this.subTypes = subTypesObservable.pipe(
          map(subTypes => subTypes.map(subType => subType.type)),
          publishReplay(1),
          refCount()
        );
      } else {
        return throwError(null);
      }
    }
    return this.subTypes;
  }

  clear() {
    this.subTypeFormGroup.get('subType').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.subTypeInput.nativeElement.blur();
      this.subTypeInput.nativeElement.focus();
    }, 0);
  }

}
