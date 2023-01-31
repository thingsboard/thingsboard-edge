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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of, Subscription, throwError } from 'rxjs';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  publishReplay,
  refCount,
  switchMap,
  tap
} from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { DeviceService } from '@core/http/device.service';
import { EntitySubtype, EntityType } from '@app/shared/models/entity-type.models';
import { BroadcastService } from '@app/core/services/broadcast.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { EdgeService } from '@core/http/edge.service';

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
  entitySubtypeMaxLength: string;

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
              private edgeService: EdgeService,
              private fb: FormBuilder) {
    this.subTypeFormGroup = this.fb.group({
      subType: [null, Validators.maxLength(255)]
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
        this.entitySubtypeMaxLength = 'asset.asset-type-max-length';
        this.broadcastSubscription = this.broadcast.on('assetSaved', () => {
          this.subTypes = null;
        });
        break;
      case EntityType.DEVICE:
        this.selectEntitySubtypeText = 'device.select-device-type';
        this.entitySubtypeText = 'device.device-type';
        this.entitySubtypeRequiredText = 'device.device-type-required';
        this.entitySubtypeMaxLength = 'device.device-type-max-length';
        this.broadcastSubscription = this.broadcast.on('deviceSaved', () => {
          this.subTypes = null;
        });
        break;
      case EntityType.EDGE:
        this.selectEntitySubtypeText = 'edge.select-edge-type';
        this.entitySubtypeText = 'edge.edge-type';
        this.entitySubtypeRequiredText = 'edge.edge-type-required';
        this.entitySubtypeMaxLength = 'edge.type-max-length';
        this.broadcastSubscription = this.broadcast.on('edgeSaved', () => {
          this.subTypes = null;
        });
        break;
      case EntityType.ENTITY_VIEW:
        this.selectEntitySubtypeText = 'entity-view.select-entity-view-type';
        this.entitySubtypeText = 'entity-view.entity-view-type';
        this.entitySubtypeRequiredText = 'entity-view.entity-view-type-required';
        this.entitySubtypeMaxLength = 'entity-view.type-max-length'
        this.broadcastSubscription = this.broadcast.on('entityViewSaved', () => {
          this.subTypes = null;
        });
        break;
    }

    this.filteredSubTypes = this.subTypeFormGroup.get('subType').valueChanges
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(value => {
          this.updateView(value);
        }),
        // startWith<string | EntitySubtype>(''),
        map(value => value ? value : ''),
        switchMap(type => this.fetchSubTypes(type))
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
      map(subTypes => subTypes.filter(subType => {
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
        case EntityType.EDGE:
          subTypesObservable = this.edgeService.getEdgeTypes({ignoreLoading: true});
          break;
      }
      if (subTypesObservable) {
        this.subTypes = subTypesObservable.pipe(
          catchError(() => of([] as Array<EntitySubtype>)),
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
