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

import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, Subject, Subscription, throwError } from 'rxjs';
import { map, mergeMap, publishReplay, refCount, startWith, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { DeviceService } from '@core/http/device.service';
import { EntitySubtype, EntityType } from '@app/shared/models/entity-type.models';
import { BroadcastService } from '@app/core/services/broadcast.service';
import { AssetService } from '@core/http/asset.service';
import { EdgeService } from '@core/http/edge.service';
import { EntityViewService } from '@core/http/entity-view.service';

@Component({
  selector: 'tb-entity-subtype-select',
  templateUrl: './entity-subtype-select.component.html',
  styleUrls: ['./entity-subtype-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntitySubTypeSelectComponent),
    multi: true
  }]
})
export class EntitySubTypeSelectComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  subTypeFormGroup: FormGroup;

  modelValue: string | null = '';

  @Input()
  entityType: EntityType;

  @Input()
  showLabel: boolean;

  @Input()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  typeTranslatePrefix: string;

  entitySubtypeTitle: string;
  entitySubtypeRequiredText: string;

  subTypesOptions: Observable<Array<EntitySubtype | string>>;

  private subTypesOptionsSubject: Subject<string> = new Subject();

  subTypes: Observable<Array<EntitySubtype | string>>;

  subTypesLoaded = false;

  private broadcastSubscription: Subscription;

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
      subType: ['']
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
        this.entitySubtypeTitle = 'asset.asset-type';
        this.entitySubtypeRequiredText = 'asset.asset-type-required';
        this.broadcastSubscription = this.broadcast.on('assetSaved', () => {
          this.subTypes = null;
          this.subTypesOptionsSubject.next('');
        });
        break;
      case EntityType.DEVICE:
        this.entitySubtypeTitle = 'device.device-type';
        this.entitySubtypeRequiredText = 'device.device-type-required';
        this.broadcastSubscription = this.broadcast.on('deviceSaved', () => {
          this.subTypes = null;
          this.subTypesOptionsSubject.next('');
        });
        break;
      case EntityType.ENTITY_VIEW:
        this.entitySubtypeTitle = 'entity-view.entity-view-type';
        this.entitySubtypeRequiredText = 'entity-view.entity-view-type-required';
        this.broadcastSubscription = this.broadcast.on('entityViewSaved', () => {
          this.subTypes = null;
          this.subTypesOptionsSubject.next('');
        });
        break;
      case EntityType.EDGE:
        this.entitySubtypeTitle = 'edge.edge-type';
        this.entitySubtypeRequiredText = 'edge.edge-type-required';
        this.broadcastSubscription = this.broadcast.on('edgeSaved', () => {
          this.subTypes = null;
          this.subTypesOptionsSubject.next('');
        });
        break;
    }

    this.subTypesOptions = this.subTypesOptionsSubject.asObservable().pipe(
      startWith<string | EntitySubtype>(''),
      mergeMap(() => this.getSubTypes())
    );

    this.subTypeFormGroup.get('subType').valueChanges.subscribe(
      (value) => {
        let modelValue;
        if (!value || value === '') {
          modelValue = '';
        } else {
          modelValue = value.type;
        }
        this.updateView(modelValue);
      }
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
      this.subTypeFormGroup.disable();
    } else {
      this.subTypeFormGroup.enable();
    }
  }

  writeValue(value: string | null): void {
    if (value != null && value !== '') {
      this.modelValue = value;
      this.findSubTypes(value).subscribe(
        (subTypes) => {
          const subType = subTypes && subTypes.length === 1 ? subTypes[0] : '';
          this.subTypeFormGroup.get('subType').patchValue(subType, {emitEvent: true});
        }
      );
    } else {
      this.modelValue = '';
      this.subTypeFormGroup.get('subType').patchValue('', {emitEvent: true});
    }
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displaySubTypeFn(subType?: EntitySubtype | string): string | undefined {
    if (subType && typeof subType !== 'string') {
      if (this.typeTranslatePrefix) {
        return this.translate.instant(this.typeTranslatePrefix + '.' + subType.type);
      } else {
        return subType.type;
      }
    } else {
      return this.translate.instant('entity.all-subtypes');
    }
  }

  findSubTypes(searchText?: string): Observable<Array<EntitySubtype | string>> {
    return this.getSubTypes().pipe(
      map(subTypes => subTypes.filter( subType => {
          return searchText ? (typeof subType === 'string' ? false : subType.type === searchText) : false;
      }))
    );
  }

  getSubTypes(): Observable<Array<EntitySubtype | string>> {
    if (!this.subTypes) {
      switch (this.entityType) {
        case EntityType.ASSET:
          this.subTypes = this.assetService.getAssetTypes({ignoreLoading: true});
          break;
        case EntityType.DEVICE:
          this.subTypes = this.deviceService.getDeviceTypes({ignoreLoading: true});
          break;
        case EntityType.ENTITY_VIEW:
          this.subTypes = this.entityViewService.getEntityViewTypes({ignoreLoading: true});
          break;
        case EntityType.EDGE:
          this.subTypes = this.edgeService.getEdgeTypes({ignoreLoading: true});
          break;
      }
      if (this.subTypes) {
        this.subTypes = this.subTypes.pipe(
          map((allSubtypes) => {
              allSubtypes.unshift('');
              this.subTypesLoaded = true;
              return allSubtypes;
          }),
          tap((subTypes) => {
            const type: EntitySubtype | string = this.subTypeFormGroup.get('subType').value;
            const strType = typeof type === 'string' ? type : type.type;
            const found = subTypes.find((subType) => {
              if (typeof subType === 'string') {
                return subType === type;
              } else {
                return subType.type === strType;
              }
            });
            if (found) {
              this.subTypeFormGroup.get('subType').patchValue(found);
            }
          }),
          publishReplay(1),
          refCount()
        );
      } else {
        return throwError(null);
      }
    }
    return this.subTypes;
  }
}
