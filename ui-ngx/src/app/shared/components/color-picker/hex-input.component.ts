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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { Color } from '@iplab/ngx-color-picker';

@Component({
  selector: `tb-hex-input`,
  templateUrl: `./hex-input.component.html`,
  styleUrls: ['./hex-input.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HexInputComponent {

  @Input()
  public color: Color;

  @Output()
  public colorChange = new EventEmitter<Color>(false);

  @Input()
  public labelVisible = false;

  @Input()
  public prefixValue = '#';

  public get value() {
    return this.color ? this.color.toHexString(this.color.getRgba().alpha < 1).replace('#', '') : '';
  }

  public get copyColor() {
    return this.prefixValue + this.value;
  }

  public get hueValue(): string {
    return this.color ? Math.round(this.color.getRgba().alpha * 100).toString() : '';
  }

  public onHueInputChange(event: KeyboardEvent, inputValue: string): void {
    const color = this.color.getRgba();
    const alpha = +inputValue / 100;
    if (color.getAlpha() !== alpha) {
      const newColor = new Color().setRgba(color.red, color.green, color.blue, alpha).toHexString(true);
      this.colorChange.emit(new Color(newColor));
    }
  }

  public onInputChange(event: KeyboardEvent, inputValue: string): void {
    const value = inputValue.replace('#', '').toLowerCase();
    if (
      ((event.keyCode === 13 || event.key.toLowerCase() === 'enter') && value.length === 3)
      || value.length === 6 || value.length === 8
    ) {
      const hex = parseInt(value, 16);
      const hexStr = hex.toString(16);
      if (hexStr.padStart(value.length, '0') === value && this.value.toLowerCase() !== value) {
        const newColor = new Color(`#${value}`);
        this.colorChange.emit(newColor);
      }
    }
  }
}
