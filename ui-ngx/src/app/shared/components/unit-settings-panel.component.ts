///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { AllMeasures, TbUnit, UnitInfo, UnitSystem } from '@shared/models/unit.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UnitService } from '@core/services/unit.service';
import { debounceTime } from 'rxjs/operators';
import { isUndefinedOrNull } from '@core/utils';

@Component({
  selector: 'tb-covert-unit-settings-panel',
  templateUrl: './unit-settings-panel.component.html',
  styleUrls: ['./unit-settings-panel.component.scss'],
  providers: [],
  encapsulation: ViewEncapsulation.None
})
export class UnitSettingsPanelComponent implements OnInit {

  @Input()
  unit: TbUnit;

  @Input()
  required: boolean;

  @Input()
  disabled: boolean;

  @Output()
  unitSettingsApplied = new EventEmitter<TbUnit>();

  @Input()
  tagFilter: string;

  @Input()
  measure: AllMeasures;

  UnitSystem = UnitSystem;

  targetMeasure: AllMeasures;

  unitSettingForm =  this.fb.group({
    from: [''],
    convertUnit: [true],
    METRIC: [''],
    IMPERIAL: [''],
    HYBRID: ['']
  })

  constructor(
    private popover: TbPopoverComponent<UnitSettingsPanelComponent>,
    private fb: FormBuilder,
    private unitService: UnitService
  ) {
    this.unitSettingForm.get('from').valueChanges.pipe(
      debounceTime(200),
      takeUntilDestroyed()
    ).subscribe(unit => {
      const unitDescription = this.unitService.getUnitInfo(unit);
      const units = unitDescription ? this.unitService.getUnits(unitDescription.measure) : [];
      if (unitDescription && units.length > 1) {
        this.unitSettingForm.get('convertUnit').enable({emitEvent: true});
        this.targetMeasure = unitDescription.measure;
        if (unitDescription.system === UnitSystem.IMPERIAL) {
          this.unitSettingForm.get('METRIC').setValue(this.unitService.getDefaultUnit(this.targetMeasure, UnitSystem.METRIC), {emitEvent: false});
          this.unitSettingForm.get('IMPERIAL').setValue(unit, {emitEvent: false});
          this.unitSettingForm.get('HYBRID').setValue(unit, {emitEvent: false});
        } else {
          this.unitSettingForm.get('METRIC').setValue(unit, {emitEvent: false});
          this.unitSettingForm.get('IMPERIAL').setValue(this.unitService.getDefaultUnit(this.targetMeasure, UnitSystem.IMPERIAL), {emitEvent: false});
          this.unitSettingForm.get('HYBRID').setValue(unit, {emitEvent: false});
        }
      } else {
        this.unitSettingForm.get('convertUnit').setValue(false, {onlySelf: true});
        this.unitSettingForm.get('convertUnit').disable({emitEvent: false});
        this.unitSettingForm.patchValue({
          METRIC: '',
          IMPERIAL: '',
          HYBRID: ''
        }, {emitEvent: false});
      }
    })

    this.unitSettingForm.get('convertUnit').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value) {
        this.unitSettingForm.get('METRIC').enable({emitEvent: false});
        this.unitSettingForm.get('IMPERIAL').enable({emitEvent: false});
        this.unitSettingForm.get('HYBRID').enable({emitEvent: false});
      } else {
        this.unitSettingForm.get('METRIC').disable({emitEvent: false});
        this.unitSettingForm.get('IMPERIAL').disable({emitEvent: false});
        this.unitSettingForm.get('HYBRID').disable({emitEvent: false});
      }
    });
  }

  ngOnInit() {
    let unitDescription: UnitInfo;
    if (this.required) {
      this.unitSettingForm.get('from').setValidators(Validators.required);
    }
    if (typeof this.unit === 'string') {
      this.unitSettingForm.get('convertUnit').setValue(false, {onlySelf: true});
      this.unitSettingForm.get('from').setValue(this.unit);
      unitDescription = this.unitService.getUnitInfo(this.unit);
    } else if (isUndefinedOrNull(this.unit)) {
      this.unitSettingForm.get('convertUnit').setValue(false, {onlySelf: true});
      this.unitSettingForm.get('from').setValue(null);
    } else {
      this.unitSettingForm.patchValue(this.unit, {emitEvent: false});
      unitDescription = this.unitService.getUnitInfo(this.unit.from);
    }

    if (unitDescription?.measure) {
      this.targetMeasure = unitDescription.measure;
    } else {
      this.unitSettingForm.get('convertUnit').disable({emitEvent: false});
    }
    if (this.disabled) {
      this.unitSettingForm.disable({emitEvent: false});
    }
  }

  clearUnit() {
    this.unitSettingsApplied.emit(null);
  }

  cancel() {
    this.popover.hide();
  }

  applyUnitSettings() {
    if (this.unitSettingForm.value.convertUnit) {
      const formValue = this.unitSettingForm.value;
      delete formValue.convertUnit;
      this.unitSettingsApplied.emit(formValue as TbUnit);
    } else {
      this.unitSettingsApplied.emit(this.unitSettingForm.value.from);
    }
  }
}
