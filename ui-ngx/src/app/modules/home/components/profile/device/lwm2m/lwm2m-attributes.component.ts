///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import {Component, EventEmitter, forwardRef, Inject, Input, Output} from "@angular/core";
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR} from "@angular/forms";
import {coerceBooleanProperty} from "@angular/cdk/coercion";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {DeviceProfileService} from "@core/http/device-profile.service";
import {WINDOW} from "@core/services/window.service";
import {deepClone, isDefinedAndNotNull, isEmpty} from "@core/utils";
import {
  Lwm2mAttributesDialogComponent,
  Lwm2mAttributesDialogData
} from "@home/components/profile/device/lwm2m/lwm2m-attributes-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {ATTRIBUTE_LWM2M_LABEL} from "@home/components/profile/device/lwm2m/lwm2m-profile-config.models";


@Component({
  selector: 'tb-profile-lwm2m-attributes',
  templateUrl: './lwm2m-attributes.component.html',
  styleUrls: ['./lwm2m-attributes.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Lwm2mAttributesComponent),
    multi: true
  }]
})
export class Lwm2mAttributesComponent implements ControlValueAccessor {
  attributeLwm2mFormGroup: FormGroup;
  attributeLwm2mLabel = ATTRIBUTE_LWM2M_LABEL;

  private requiredValue: boolean;
  private dirty = false;

  @Input()
  attributeLwm2m: {};

  @Input()
  isAttributeTelemetry: boolean;

  @Input()
  destName: string;

  @Input()
  disabled: boolean;

  @Output()
  updateAttributeLwm2m = new EventEmitter<any>();

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private propagateChange = (v: any) => {
  }

  constructor(private store: Store<AppState>,
              private dialog: MatDialog,
              private fb: FormBuilder,
              private deviceProfileService: DeviceProfileService,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.attributeLwm2mFormGroup.disable({emitEvent: false});
    } else {
      this.attributeLwm2mFormGroup.enable({emitEvent: false});
    }
  }

  ngOnInit() {
    this.attributeLwm2mFormGroup = this.fb.group({
      attributeLwm2m: [this.attributeLwm2m]
    });
  }

  writeValue(value: {} | null): void {}

  attributeLwm2mToString = (): string => {
    return this.isIconEditAdd () ? this.attributeLwm2mLabelToString() : this.translate.instant('device-profile.lwm2m.no-data');
  }

  private attributeLwm2mLabelToString = (): string => {
    let label = JSON.stringify(this.attributeLwm2m);
    label = deepClone(label.replace('{', ''));
    label = deepClone(label.replace('}', ''));
    this.attributeLwm2mLabel.forEach((value: string, key: string) => {
      const dest = '\"' + key + '\"\:';
      label = deepClone(label.replace(dest, value));
    });
    return label;
  }

  isDisableBtn (): boolean {
    return this.disabled || this.isAttributeTelemetry ? !(isDefinedAndNotNull(this.attributeLwm2m) &&
      !isEmpty(this.attributeLwm2m) && this.disabled) :  this.disabled;
  }

  isIconView (): boolean {
    return this.isAttributeTelemetry || this.disabled;
  }

  isIconEditAdd (): boolean {
    return isDefinedAndNotNull(this.attributeLwm2m) && !isEmpty(this.attributeLwm2m);
  }

  isToolTipLabel (): string {
    return this.disabled ? this.translate.instant('device-profile.lwm2m.attribute-lwm2m-tip') :
      this.isAttributeTelemetry ? this.translate.instant('device-profile.lwm2m.attribute-lwm2m-disable-tip') :
        this.translate.instant('device-profile.lwm2m.attribute-lwm2m-tip');
  }

  public editAttributesLwm2m = ($event: Event): void => {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<Lwm2mAttributesDialogComponent, Lwm2mAttributesDialogData, Object>(Lwm2mAttributesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        readonly: this.disabled,
        attributeLwm2m: this.disabled ? this.attributeLwm2m : deepClone(this.attributeLwm2m),
        destName: this.destName
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.attributeLwm2m = result;
        this.attributeLwm2mFormGroup.patchValue({attributeLwm2m: this.attributeLwm2m});
        this.updateAttributeLwm2m.next(this.attributeLwm2m);
      }
    });
  }
}
