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

import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { isEmpty, isUndefinedOrNull } from '@core/utils';
import { Lwm2mAttributesDialogComponent, Lwm2mAttributesDialogData } from './lwm2m-attributes-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { AttributesNameValueMap } from './lwm2m-profile-config.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-profile-lwm2m-attributes',
  templateUrl: './lwm2m-attributes.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Lwm2mAttributesComponent),
    multi: true
  }]
})
export class Lwm2mAttributesComponent implements ControlValueAccessor, OnDestroy {
  attributesFormGroup: UntypedFormGroup;

  private requiredValue: boolean;
  private destroy$ = new Subject<void>();

  @Input()
  isAttributeTelemetry: boolean;

  @Input()
  modelName: string;

  @Input()
  disabled: boolean;

  @Input()
  isResource = false;

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private propagateChange = (v: any) => {
  }

  constructor(private dialog: MatDialog,
              private fb: UntypedFormBuilder) {
    this.attributesFormGroup = this.fb.group({
      attributes: [{}]
    });
    this.attributesFormGroup.get('attributes').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(attributes => {
      this.propagateChange(attributes);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.attributesFormGroup.disable({emitEvent: false});
    } else {
      this.attributesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AttributesNameValueMap | null) {
    this.attributesFormGroup.patchValue({attributes: value}, {emitEvent: false});
  }

  get attributesValueMap(): AttributesNameValueMap {
    return this.attributesFormGroup.get('attributes').value;
  }

  isDisableBtn(): boolean {
    return !this.disabled && this.isAttributeTelemetry;
  }

  isEmpty(): boolean {
    const value = this.attributesValueMap;
    return isUndefinedOrNull(value) || isEmpty(value);
  }

  get tooltipSetAttributesTelemetry(): string {
    return this.isDisableBtn() ? 'device-profile.lwm2m.edit-attributes-select' : '';
  }

  get tooltipButton(): string {
    if (this.disabled) {
      return 'device-profile.lwm2m.view-attribute';
    } else if (this.isEmpty()) {
      return 'device-profile.lwm2m.add-attribute';
    }
    return 'device-profile.lwm2m.edit-attribute';
  }

  get iconButton(): string {
    if (this.disabled) {
      return 'visibility';
    } else if (this.isEmpty()) {
      return 'add';
    }
    return 'edit';
  }

  public editAttributesLwm2m = ($event: Event): void => {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<Lwm2mAttributesDialogComponent, Lwm2mAttributesDialogData, AttributesNameValueMap>(Lwm2mAttributesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        readonly: this.disabled,
        attributes: this.attributesValueMap,
        modelName: this.modelName,
        isResource: this.isResource
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.attributesFormGroup.patchValue({attributes: result});
      }
    });
  }
}
