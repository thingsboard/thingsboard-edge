///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { AfterViewInit, Component, forwardRef, Input, OnDestroy } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Palette } from '@shared/models/white-labeling.models';
import { deepClone, isEqual } from '@core/utils';
import { ColorPalette, getContrastColor, materialColorPalette } from '@shared/models/material.models';
import { PaletteDialogComponent, PaletteDialogData } from '@home/pages/admin/palette-dialog.component';

@Component({
  selector: 'tb-palette',
  templateUrl: './palette.component.html',
  styleUrls: ['./palette.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PaletteComponent),
      multi: true
    }
  ]
})
export class PaletteComponent extends PageComponent implements AfterViewInit, OnDestroy, ControlValueAccessor {

  @Input()
  label: string;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
    }
  }

  @Input()
  disabled: boolean;

  palette: Palette;

  palettes: Palette[] = [];

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog) {
    super(store);
    for (const paletteType of Object.keys(materialColorPalette)) {
      const palette: Palette = {
        type: paletteType
      };
      this.palettes.push(palette);
    }
  }

  ngAfterViewInit() {
  }

  ngOnDestroy() {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: Palette): void {
    this.palette = deepClone(value);
    if (!this.palette) {
      this.palette = {
        type: null
      };
    }
    if (this.palette.type === 'custom') {
      const customPaletteIndex = this.palettes.findIndex((palette) => palette.type === 'custom');
      if (customPaletteIndex === -1) {
        this.palettes.push(deepClone(this.palette));
      } else {
        this.palettes[customPaletteIndex] = deepClone(this.palette);
      }
    }
  }

  paletteName(palette: Palette): string {
    if (palette) {
      return palette.type.toUpperCase().replace('-', ' ');
    } else {
      return '';
    }
  }

  paletteStyle(palette: Palette): {[klass: string]: any} {
    if (palette && palette.type) {
      const key = palette.type === 'custom' ? palette.extends : palette.type;
      const paletteInfo = materialColorPalette[key];
      const hex = palette.colors && palette.colors['500']
          ? palette.colors['500'] : paletteInfo['500'];
      const contrast = getContrastColor(key, '500');
      return {
        backgroundColor: hex,
        color: contrast
      }
    } else {
      return {};
    }
  }

  paletteTypeChanged() {
    if (this.palette.type === 'custom') {
      const customPaletteResult = this.palettes.find((palette) => palette.type === 'custom');
      if (customPaletteResult) {
        this.palette = deepClone(customPaletteResult);
      }
    } else {
      delete this.palette.extends;
      delete this.palette.colors;
    }
    this.updateModel();
  }

  editPalette() {
    this.dialog.open<PaletteDialogComponent, PaletteDialogData, ColorPalette>(PaletteDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          palette: deepClone(this.palette)
        }
    }).afterClosed().subscribe((colors) => {
      if (colors) {
        if (isEqual(colors, {})) {
          colors = null;
        }
        this.updatePaletteColors(colors);
      }
    });
  }

  private updatePaletteColors(colors: ColorPalette) {
    if (colors) {
      this.palette.colors = colors;
      if (this.palette.type !== 'custom') {
        this.palette.extends = this.palette.type;
        this.palette.type = 'custom';
      }
      const customPaletteIndex = this.palettes.findIndex((palette) => palette.type === 'custom');
      if (customPaletteIndex === -1) {
        this.palettes.push(deepClone(this.palette));
      } else {
        this.palettes[customPaletteIndex] = deepClone(this.palette);
      }
    } else {
      delete this.palette.colors;
      if (this.palette.type === 'custom') {
        this.palette.type = this.palette.extends;
        delete this.palette.extends;
      }
    }
    this.updateModel();
  }

  private updateModel() {
    this.propagateChange(this.palette);
  }
}
