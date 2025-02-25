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

import { PageComponent } from '@shared/components/page.component';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { coerceBoolean } from '@shared/decorators/coercion';
import { accentPalette, primaryPalette } from '@shared/models/material.models';
import { isDefinedAndNotNull, plainColorFromVariable } from '@core/utils';
import { UntypedFormControl } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

type ColorMode = 'color' | 'primary' | 'accent';

@Component({
  selector: 'tb-color-picker-panel',
  templateUrl: './color-picker-panel.component.html',
  providers: [],
  styleUrls: ['./color-picker-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ColorPickerPanelComponent extends PageComponent implements OnInit, OnDestroy{

  @Input()
  color: string;

  @Input()
  @coerceBoolean()
  colorClearButton = false;

  @Input()
  @coerceBoolean()
  useThemePalette: boolean;

  @Input()
  @coerceBoolean()
  colorCancelButton = false;

  @Input()
  popover: TbPopoverComponent<ColorPickerPanelComponent>;

  @Output()
  colorSelected = new EventEmitter<string>();

  @Output()
  colorCancelDialog = new EventEmitter();

  colorMode: ColorMode = 'color';
  plainColorControl = new UntypedFormControl();
  primaryColor: string;
  accentColor: string;

  dirty = false;
  valid = true;

  private destroy$ = new Subject<void>();


  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.plainColorControl.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.onPlainColorChange();
    });
    if (this.useThemePalette) {
      if (this.color && this.color.startsWith('var(')) {
        this.colorMode = 'primary';
        if (Object.values(primaryPalette).indexOf(this.color) > -1) {
          this.primaryColor = this.color;
        } else if (Object.values(accentPalette).indexOf(this.color) > -1) {
          this.colorMode = 'accent';
          this.accentColor = this.color;
        }
        this.plainColorControl.patchValue(plainColorFromVariable(this.color), {emitEvent: false});
      } else {
        this.plainColorControl.patchValue(this.color, {emitEvent: false});
      }
    } else {
      this.plainColorControl.patchValue(this.color, {emitEvent: false});
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  onPrimaryColorChange(color: string) {
    this.primaryColor = color;
    this.accentColor = null;
    this.plainColorControl.patchValue(plainColorFromVariable(this.primaryColor), {emitEvent: false});
    this.dirty = true;
    this.updateValidity();
  }

  onAccentColorChange(color: string) {
    this.accentColor = color;
    this.primaryColor = null;
    this.plainColorControl.patchValue(plainColorFromVariable(this.accentColor), {emitEvent: false});
    this.dirty = true;
    this.updateValidity();
  }

  onPlainColorChange() {
    this.primaryColor = null;
    this.accentColor = null;
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

  selectColor() {
    const color = this.getColor();
    this.colorSelected.emit(color);
  }

  getColor() {
    switch (this.colorMode) {
      case 'color':
        return this.plainColorControl.value;
      case 'primary':
        return this.primaryColor;
      case 'accent':
        return this.accentColor;
    }
  }

  clearColor() {
    this.colorSelected.emit(null);
  }

  cancelColor() {
    if (this.popover) {
      this.popover.hide();
    } else {
      this.colorCancelDialog.emit();
    }
  }}
