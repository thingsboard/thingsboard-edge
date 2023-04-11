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

import { ChangeDetectionStrategy, Component, forwardRef, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { accentPalette, ColorPalette, getContrastColor, primaryPalette } from '@shared/models/material.models';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { UtilsService } from '@core/services/utils.service';

@Component({
  selector: 'tb-theme-color-select',
  templateUrl: './theme-color-select.component.html',
  styleUrls: ['./theme-color-select.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ThemeColorSelectComponent),
      multi: true
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ThemeColorSelectComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  palette: 'primary' | 'accent' = 'primary';

  private modelValue: string;

  private propagateChange = null;

  paletteInfo: ColorPalette;
  hues: string[];
  colors: ColorPalette;
  paletteKey: string;
  selectedHue: string;

  constructor(protected store: Store<AppState>,
              private wl: WhiteLabelingService,
              private utils: UtilsService) {
    super(store);
  }

  ngOnInit(): void {
    this.paletteInfo = this.palette === 'primary' ? primaryPalette : accentPalette;
    const palette = this.palette === 'primary' ? this.wl.primaryPalette : this.wl.accentPalette;
    this.paletteKey = ['custom', 'tb-primary', 'tb-accent'].includes(palette.type) ? palette.extends : palette.type;
    this.hues = Object.keys(this.paletteInfo);
    this.colors = {};
    for (const hue of this.hues) {
      this.colors[hue] = this.utils.plainColorFromVariable(this.paletteInfo[hue]);
    }
  }

  hueStyle(hue: string): {[klass: string]: any} {
    if (hue) {
      const color = this.colors[hue];
      const contrast = getContrastColor(this.paletteKey, hue);
      return {
        backgroundColor: color,
        color: contrast
      };
    } else {
      return {};
    }
  }

  selectedColorStyle(): {[klass: string]: any} {
    if (this.selectedHue) {
      const color = this.colors[this.selectedHue];
      return {
        background: color
      };
    } else {
      return {};
    }
  }

  updateValidators() {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: string): void {
    this.modelValue = value;
    let hue = null;
    if (value) {
      const colorIndex = Object.values(this.paletteInfo).indexOf(value);
      if (colorIndex) {
        hue = this.hues[colorIndex];
      }
    }
    this.selectedHue = hue;
  }

  selectColor(hue: string) {
    if (hue) {
      this.selectedHue = hue;
      const color = this.paletteInfo[hue];
      if (this.modelValue !== color) {
        this.modelValue = color;
        this.propagateChange(this.modelValue);
      }
    }
  }

}
