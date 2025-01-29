///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { PageComponent } from '@shared/components/page.component';
import { EntitySearchDirection, entitySearchDirectionTranslations } from '@app/shared/models/relation.models';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

interface DeviceRelationsQuery {
  fetchLastLevelOnly: boolean;
  direction: EntitySearchDirection;
  maxLevel?: number;
  relationType?: string;
  deviceTypes: string[];
}

@Component({
  selector: 'tb-device-relations-query-config',
  templateUrl: './device-relations-query-config.component.html',
  styleUrls: ['./device-relations-query-config.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceRelationsQueryConfigComponent),
      multi: true
    }
  ]
})
export class DeviceRelationsQueryConfigComponent extends PageComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  directionTypes: Array<EntitySearchDirection> = Object.values(EntitySearchDirection);
  directionTypeTranslations = entitySearchDirectionTranslations;

  entityType = EntityType;

  deviceRelationsQueryFormGroup: FormGroup;

  private propagateChange = null;

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.deviceRelationsQueryFormGroup = this.fb.group({
      fetchLastLevelOnly: [false, []],
      direction: [null, [Validators.required]],
      maxLevel: [null, [Validators.min(1)]],
      relationType: [null],
      deviceTypes: [null, [Validators.required]]
    });
    this.deviceRelationsQueryFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((query: DeviceRelationsQuery) => {
      if (this.deviceRelationsQueryFormGroup.valid) {
        this.propagateChange(query);
      } else {
        this.propagateChange(null);
      }
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceRelationsQueryFormGroup.disable({emitEvent: false});
    } else {
      this.deviceRelationsQueryFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(query: DeviceRelationsQuery): void {
    this.deviceRelationsQueryFormGroup.reset(query, {emitEvent: false});
  }
}
