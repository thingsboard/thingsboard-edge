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
import tinycolor from 'tinycolor2';
import { DialogService } from '@core/services/dialog.service';

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
