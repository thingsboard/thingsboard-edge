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

import { PageComponent } from '@shared/components/page.component';
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { coerceBoolean } from '@shared/decorators/coercion';
import { accentPalette, primaryPalette } from '@shared/models/material.models';
import { UtilsService } from '@core/services/utils.service';
import { isDefinedAndNotNull } from '@core/utils';

type ColorMode = 'color' | 'primary' | 'accent';

@Component({
  selector: 'tb-color-picker-panel',
  templateUrl: './color-picker-panel.component.html',
  providers: [],
  styleUrls: ['./color-picker-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ColorPickerPanelComponent extends PageComponent implements OnInit {

  @Input()
  color: string;

  @Input()
  @coerceBoolean()
  colorClearButton = false;

  @Input()
  @coerceBoolean()
  useThemePalette: boolean;

  @Input()
  popover: TbPopoverComponent<ColorPickerPanelComponent>;

  @Output()
  colorSelected = new EventEmitter<string>();

  colorMode: ColorMode = 'color';
  plainColor: string;
  primaryColor: string;
  accentColor: string;

  dirty = false;
  valid = true;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService) {
    super(store);
  }

  ngOnInit(): void {
    if (this.useThemePalette) {
      if (this.color && this.color.startsWith('var(')) {
        this.colorMode = 'primary';
        if (Object.values(primaryPalette).indexOf(this.color) > -1) {
          this.primaryColor = this.color;
        } else if (Object.values(accentPalette).indexOf(this.color) > -1) {
          this.colorMode = 'accent';
          this.accentColor = this.color;
        }
        this.plainColor = this.utils.plainColorFromVariable(this.color);
      } else {
        this.plainColor = this.color;
      }
    } else {
      this.plainColor = this.color;
    }
  }

  onPrimaryColorChange(color: string) {
    this.primaryColor = color;
    this.accentColor = null;
    this.plainColor = this.utils.plainColorFromVariable(this.primaryColor);
    this.dirty = true;
    this.updateValidity();
  }

  onAccentColorChange(color: string) {
    this.accentColor = color;
    this.primaryColor = null;
    this.plainColor = this.utils.plainColorFromVariable(this.accentColor);
    this.dirty = true;
    this.updateValidity();
  }

  onPlainColorChange(color: string) {
    this.primaryColor = null;
    this.accentColor = null;
    this.plainColor = color;
    this.dirty = true;
    this.updateValidity();
  }

  selectedIndexChange(index: number) {
    switch (index) {
      case 0:
        this.colorMode = 'color';
        break;
      case 1:
        this.colorMode = 'primary';
        break;
      case 2:
        this.colorMode = 'accent';
        break;
    }
    this.dirty = true;
    this.updateValidity();
    setTimeout(() => {
      this.popover?.updatePosition();
    }, 0);
  }

  private updateValidity() {
    const color = this.getColor();
    this.valid = isDefinedAndNotNull(color);
  }

  selectColor() {
    const color = this.getColor();
    this.colorSelected.emit(color);
  }

  getColor() {
    switch (this.colorMode) {
      case 'color':
        return this.plainColor;
      case 'primary':
        return this.primaryColor;
      case 'accent':
        return this.accentColor;
    }
  }

  clearColor() {
    this.colorSelected.emit(null);
  }
}
