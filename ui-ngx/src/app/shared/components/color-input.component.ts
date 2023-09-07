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

import { ChangeDetectorRef, Component, forwardRef, Input, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DialogService } from '@core/services/dialog.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TbPopoverService } from '@shared/components/popover.service';
import { ColorPickerPanelComponent } from '@shared/components/color-picker/color-picker-panel.component';
import { MatButton } from '@angular/material/button';

@Component({
  selector: 'tb-color-input',
  templateUrl: './color-input.component.html',
  styleUrls: ['./color-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ColorInputComponent),
      multi: true
    }
  ]
})
export class ColorInputComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  @coerceBoolean()
  asBoxInput = false;

  @Input()
  icon: string;

  @Input()
  label: string;

  @Input()
  requiredText: string;

  @Input()
  @coerceBoolean()
  colorClearButton = false;

  @Input()
  @coerceBoolean()
  useThemePalette = false;

  @Input()
  @coerceBoolean()
  openOnInput = false;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  disabled: boolean;

  private modelValue: string;

  private propagateChange = null;

  public colorFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private dialogs: DialogService,
              private translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.colorFormGroup = this.fb.group({
      color: [null, this.required ? [Validators.required] : []]
    });

    this.colorFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  updateValidators() {
    if (this.colorFormGroup) {
      this.colorFormGroup.get('color').setValidators(this.required ? [Validators.required] : []);
      this.colorFormGroup.get('color').updateValueAndValidity();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.colorFormGroup.disable({emitEvent: false});
    } else {
      this.colorFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.modelValue = value;
    this.colorFormGroup.patchValue(
      { color: this.modelValue }, {emitEvent: false}
    );
  }

  private updateModel() {
    const color: string = this.colorFormGroup.get('color').value;
    if (this.modelValue !== color) {
      this.modelValue = color;
      this.propagateChange(this.modelValue);
    }
  }

  showColorPicker($event: MouseEvent) {
    $event.stopPropagation();
    this.dialogs.colorPicker(this.colorFormGroup.get('color').value,
      this.colorClearButton, this.useThemePalette).subscribe(
      (result) => {
        if (!result?.canceled) {
          this.colorFormGroup.patchValue(
            {color: result?.color}, {emitEvent: true}
          );
          this.cd.markForCheck();
        }
      }
    );
  }

  openColorPickerPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const colorPickerPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, ColorPickerPanelComponent, 'left', true, null,
        {
          color: this.colorFormGroup.get('color').value,
          colorClearButton: this.colorClearButton,
          useThemePalette: this.useThemePalette
        },
        {},
        {}, {}, true);
      colorPickerPopover.tbComponentRef.instance.popover = colorPickerPopover;
      colorPickerPopover.tbComponentRef.instance.colorSelected.subscribe((color) => {
        colorPickerPopover.hide();
        this.colorFormGroup.patchValue(
          {color}, {emitEvent: true}
        );
        this.cd.markForCheck();
      });
    }
  }

  clear() {
    this.colorFormGroup.get('color').patchValue(null, {emitEvent: true});
    this.cd.markForCheck();
  }
}
