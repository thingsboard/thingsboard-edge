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

import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  DEFAULT_EDRX_CYCLE,
  DEFAULT_PAGING_TRANSMISSION_WINDOW, DEFAULT_PSM_ACTIVITY_TIMER,
  PowerMode,
  PowerModeTranslationMap
} from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-power-mode-settings',
  templateUrl: './power-mode-setting.component.html',
  styleUrls: []
})
export class PowerModeSettingComponent implements OnInit, OnDestroy {

  powerMods = Object.values(PowerMode);
  powerModeTranslationMap = PowerModeTranslationMap;

  private destroy$ = new Subject();

  @Input()
  parentForm: FormGroup;

  @Input()
  isDeviceSetting = false;

  ngOnInit() {
    this.parentForm.get('powerMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((powerMode: PowerMode) => {
      if (powerMode === PowerMode.E_DRX) {
        this.parentForm.get('edrxCycle').enable({emitEvent: false});
        this.parentForm.get('pagingTransmissionWindow').enable({emitEvent: false});
        this.disablePSKMode();
      } else if (powerMode === PowerMode.PSM) {
        this.parentForm.get('psmActivityTimer').enable({emitEvent: false});
        this.disableEdrxMode();
      } else {
        this.disableEdrxMode();
        this.disablePSKMode();
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private disablePSKMode() {
    this.parentForm.get('psmActivityTimer').disable({emitEvent: false});
    this.parentForm.get('psmActivityTimer').reset(DEFAULT_PSM_ACTIVITY_TIMER, {emitEvent: false});
  }

  private disableEdrxMode() {
    this.parentForm.get('edrxCycle').disable({emitEvent: false});
    this.parentForm.get('edrxCycle').reset(DEFAULT_EDRX_CYCLE, {emitEvent: false});
    this.parentForm.get('pagingTransmissionWindow').disable({emitEvent: false});
    this.parentForm.get('pagingTransmissionWindow').reset(DEFAULT_PAGING_TRANSMISSION_WINDOW, {emitEvent: false});
  }
}
