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

import { Component, forwardRef, Input, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AutoDateFormatSettings, defaultAutoDateFormatSettings } from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { deepClone, mergeDeep } from '@core/utils';
import {
  AutoDateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/auto-date-format-settings-panel.component';

@Component({
  selector: 'tb-auto-date-format-settings',
  templateUrl: './auto-date-format-settings.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AutoDateFormatSettingsComponent),
      multi: true
    }
  ]
})
export class AutoDateFormatSettingsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  defaultValues = defaultAutoDateFormatSettings;

  private modelValue: AutoDateFormatSettings;

  private propagateChange = null;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {}

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: AutoDateFormatSettings): void {
    this.modelValue = value;
  }

  openAutoFormatSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        autoDateFormatSettings: deepClone(this.modelValue),
        defaultValues: this.defaultValues
      };
      const autoDateFormatSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, AutoDateFormatSettingsPanelComponent, ['leftOnly', 'leftTopOnly', 'leftBottomOnly'], false, null,
        ctx,
        {},
        {}, {}, true);
      autoDateFormatSettingsPanelPopover.tbComponentRef.instance.popover = autoDateFormatSettingsPanelPopover;
      autoDateFormatSettingsPanelPopover.tbComponentRef.instance.autoDateFormatSettingsApplied.subscribe((autoDateFormatSettings) => {
        autoDateFormatSettingsPanelPopover.hide();
        this.modelValue = autoDateFormatSettings;
        this.propagateChange(this.modelValue);
      });
    }
  }

}
