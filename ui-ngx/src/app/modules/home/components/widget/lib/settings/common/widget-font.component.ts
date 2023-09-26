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

import { Component, forwardRef, HostBinding, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';

export interface WidgetFont {
  family: string;
  size: number;
  style: 'normal' | 'italic' | 'oblique';
  weight: 'normal' | 'bold' | 'bolder' | 'lighter' | '100' | '200' | '300' | '400' | '500' | '600' | '700' | '800' | '900';
  color: string;
  shadowColor?: string;
}

@Component({
  selector: 'tb-widget-font',
  templateUrl: './widget-font.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WidgetFontComponent),
      multi: true
    }
  ]
})
export class WidgetFontComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.display') display = 'block';

  @Input()
  disabled: boolean;

  @Input()
  hasShadowColor = false;

  @Input()
  sizeTitle = 'widgets.widget-font.size';

  private modelValue: WidgetFont;

  private propagateChange = null;

  public widgetFontFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.widgetFontFormGroup = this.fb.group({
      family: [null, []],
      size: [null, [Validators.min(1)]],
      style: [null, []],
      weight: [null, []],
      color: [null, []]
    });
    if (this.hasShadowColor) {
      this.widgetFontFormGroup.addControl('shadowColor', this.fb.control(null, []));
    }
    this.widgetFontFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.widgetFontFormGroup.disable({emitEvent: false});
    } else {
      this.widgetFontFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: WidgetFont): void {
    this.modelValue = value;
    this.widgetFontFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateModel() {
    const value: WidgetFont = this.widgetFontFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
