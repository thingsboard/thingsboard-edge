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

import { Directive } from '@angular/core';
import { FormControl, FormGroup, ValidationErrors } from '@angular/forms';
import { takeUntil } from 'rxjs/operators';
import { isEqual } from '@core/utils';
import { GatewayConnectorBasicConfigDirective } from '@home/components/widget/lib/gateway/abstract/gateway-connector-basic-config.abstract';
import {
  ModbusBasicConfig,
  ModbusBasicConfig_v3_5_2,
} from '@home/components/widget/lib/gateway/gateway-widget.models';

@Directive()
export abstract class ModbusBasicConfigDirective<BasicConfig>
  extends GatewayConnectorBasicConfigDirective<ModbusBasicConfig_v3_5_2, BasicConfig> {

  enableSlaveControl: FormControl<boolean> = new FormControl(false);

  constructor() {
    super();

    this.enableSlaveControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(enable => {
        this.updateSlaveEnabling(enable);
        this.basicFormGroup.get('slave').updateValueAndValidity({ emitEvent: !!this.onChange });
      });
  }

  override writeValue(basicConfig: BasicConfig & ModbusBasicConfig): void {
    super.writeValue(basicConfig);
    this.onEnableSlaveControl(basicConfig);
  }

  override validate(): ValidationErrors | null {
    const { master, slave } = this.basicFormGroup.value;
    const isEmpty = !master?.slaves?.length && (isEqual(slave, {}) || !slave);
    if (!this.basicFormGroup.valid || isEmpty) {
      return { basicFormGroup: { valid: false } };
    }
    return null;
  }

  protected override initBasicFormGroup(): FormGroup {
    return this.fb.group({
      master: [],
      slave: [],
    });
  }

  private updateSlaveEnabling(isEnabled: boolean): void {
    if (isEnabled) {
      this.basicFormGroup.get('slave').enable({ emitEvent: false });
    } else {
      this.basicFormGroup.get('slave').disable({ emitEvent: false });
    }
  }

  private onEnableSlaveControl(basicConfig: ModbusBasicConfig): void {
    this.enableSlaveControl.setValue(!!basicConfig.slave && !isEqual(basicConfig.slave, {}));
  }
}
