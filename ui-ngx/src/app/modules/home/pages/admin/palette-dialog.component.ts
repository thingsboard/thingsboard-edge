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

import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { NgForm } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { Palette } from '@shared/models/white-labeling.models';
import { ColorPalette, getContrastColor, materialColorPalette } from '@shared/models/material.models';
import { TranslateService } from '@ngx-translate/core';
import * as tinycolor_ from 'tinycolor2';
import { DialogService } from '@core/services/dialog.service';

const tinycolor = tinycolor_;

export interface PaletteDialogData {
  palette: Palette;
}

@Component({
  selector: 'tb-palette-dialog',
  templateUrl: './palette-dialog.component.html',
  styleUrls: []
})
export class PaletteDialogComponent extends
  DialogComponent<PaletteDialogComponent, ColorPalette> implements OnInit {

  @ViewChild('paletteForm')
  paletteForm: NgForm;

  palette: Palette;
  colors: ColorPalette;
  paletteInfo: ColorPalette;
  hues: string[];
  paletteKey: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private translate: TranslateService,
              private dialogs: DialogService,
              @Inject(MAT_DIALOG_DATA) public data: PaletteDialogData,
              public dialogRef: MatDialogRef<PaletteDialogComponent, ColorPalette>) {
    super(store, router, dialogRef);

    this.palette = this.data.palette;
    this.colors = this.palette.colors || {};

    this.paletteKey = this.palette.type === 'custom' ? this.palette.extends : this.palette.type;
    this.paletteInfo = materialColorPalette[this.paletteKey];
    this.hues = Object.keys(this.paletteInfo);
    for (const hue of this.hues) {
      if (!this.colors[hue]) {
        this.colors[hue] = this.paletteInfo[hue];
      }
    }
  }

  ngOnInit(): void {
  }

  hueStyle(hue: string): {[klass: string]: any} {
    if (hue) {
      const hex = this.colors[hue];
      const contrast = getContrastColor(this.paletteKey, hue);
      return {
        backgroundColor: hex,
        color: contrast
      }
    } else {
      return {};
    }
  }

  hueName(hue: string): string {
    if (hue === '500') {
      return this.translate.instant('white-labeling.primary-background');
    }
    if (hue === '600') {
      return this.translate.instant('white-labeling.secondary-background');
    }
    if (hue === '300') {
      return this.translate.instant('white-labeling.hue1');
    }
    if (hue === '800') {
      return this.translate.instant('white-labeling.hue2');
    }
    if (hue === 'A100') {
      return this.translate.instant('white-labeling.hue3');
    }
    return '';
  };

  editColor(hue: string) {
    this.dialogs.colorPicker(tinycolor(this.colors[hue]).toRgbString()).subscribe((color) => {
      if (color) {
        this.colors[hue] = tinycolor(color).toHexString();
        this.paletteForm.form.markAsDirty();
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  savePalette(): void {
    this.paletteForm.form.markAsPristine();
    this.dialogRef.close(this.normalizeColors(this.colors));
  }

  private normalizeColors(colors: ColorPalette): ColorPalette {
    for (const hue of Object.keys(colors)) {
      const origHex = this.paletteInfo[hue];
      if (colors[hue] === origHex) {
        delete colors[hue];
      }
    }
    return colors;
  }

}
