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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { isDefinedAndNotNull } from '@core/utils';
import { accentPalette, primaryPalette } from '@shared/models/material.models';

export interface ColorPickerDialogData {
  color: string;
  useThemePalette?: boolean;
}

type ColorMode = 'color' | 'primary' | 'accent';

@Component({
  selector: 'tb-color-picker-dialog',
  templateUrl: './color-picker-dialog.component.html',
  styleUrls: ['./color-picker-dialog.component.scss']
})
export class ColorPickerDialogComponent extends DialogComponent<ColorPickerDialogComponent, string>
  implements OnInit {

  useThemePalette: boolean;
  colorMode: ColorMode = 'color';
  plainColor: string;
  primaryColor: string;
  accentColor: string;

  dirty = false;
  valid = true;
  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private utils: UtilsService,
              @Inject(MAT_DIALOG_DATA) public data: ColorPickerDialogData,
              public dialogRef: MatDialogRef<ColorPickerDialogComponent, string>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.useThemePalette = this.data.useThemePalette;
    if (this.useThemePalette) {
      if (this.data.color && this.data.color.startsWith('var(')) {
        this.colorMode = 'primary';
        if (Object.values(primaryPalette).indexOf(this.data.color) > -1) {
          this.primaryColor = this.data.color;
        } else if (Object.values(accentPalette).indexOf(this.data.color) > -1) {
          this.colorMode = 'accent';
          this.accentColor = this.data.color;
        }
        this.plainColor = this.utils.plainColorFromVariable(this.data.color);
      } else {
        this.plainColor = this.data.color;
      }
    } else {
      this.plainColor = this.data.color;
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
  }

  private updateValidity() {
    const color = this.getColor();
    this.valid = isDefinedAndNotNull(color);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  select(): void {
    // const color: string = this.colorPickerFormGroup.get('color').value;
    const color = this.getColor();
    this.dialogRef.close(color);
  }

  private getColor() {
    switch (this.colorMode) {
      case 'color':
        return this.plainColor;
      case 'primary':
        return this.primaryColor;
      case 'accent':
        return this.accentColor;
    }
  }
}
