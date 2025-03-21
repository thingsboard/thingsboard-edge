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

import {
  ChangeDetectorRef,
  Component,
  forwardRef,
  HostBinding,
  Input,
  OnInit,
  Renderer2,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { SetValueAction, SetValueSettings, ValueToDataType } from '@shared/models/action-widget-settings.models';
import { TranslateService } from '@ngx-translate/core';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice, widgetType } from '@shared/models/widget.models';
import { isDefinedAndNotNull } from '@core/utils';
import {
  SetValueActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/set-value-action-settings-panel.component';
import { ValueType } from '@shared/models/constants';

@Component({
  selector: 'tb-set-value-action-settings',
  templateUrl: './action-settings-button.component.html',
  styleUrls: ['./action-settings-button.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SetValueActionSettingsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class SetValueActionSettingsComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.overflow')
  overflow = 'hidden';

  @Input()
  panelTitle: string;

  @Input()
  valueType = ValueType.BOOLEAN;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  widgetType: widgetType;

  @Input()
  disabled = false;

  modelValue: SetValueSettings;

  displayValue: string;

  private propagateChange = null;

  constructor(private translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (this.disabled !== isDisabled) {
      this.disabled = isDisabled;
    }
  }

  writeValue(value: SetValueSettings): void {
    this.modelValue = value;
    this.updateDisplayValue();
  }

  openActionSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        setValueSettings: this.modelValue,
        panelTitle: this.panelTitle,
        valueType: this.valueType,
        aliasController: this.aliasController,
        targetDevice: this.targetDevice,
        widgetType: this.widgetType
      };
     const setValueSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, SetValueActionSettingsPanelComponent,
       ['leftTopOnly', 'leftOnly', 'leftBottomOnly'], false, null,
        ctx,
        {},
        {}, {}, true);
      setValueSettingsPanelPopover.tbComponentRef.instance.popover = setValueSettingsPanelPopover;
      setValueSettingsPanelPopover.tbComponentRef.instance.setValueSettingsApplied.subscribe((setValueSettings) => {
        setValueSettingsPanelPopover.hide();
        this.modelValue = setValueSettings;
        this.updateDisplayValue();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateDisplayValue() {
    let value: any;
    switch (this.modelValue.valueToData.type) {
      case ValueToDataType.VALUE:
        value = 'value';
        break;
      case ValueToDataType.CONSTANT:
        value = this.modelValue.valueToData.constantValue;
        break;
      case ValueToDataType.FUNCTION:
        value = 'f(value)';
        break;
      case ValueToDataType.NONE:
        break;
    }
    switch (this.modelValue.action) {
      case SetValueAction.EXECUTE_RPC:
        let methodName = this.modelValue.executeRpc.method;
        if (isDefinedAndNotNull(value)) {
          methodName = `${methodName}(${value})`;
        }
        this.displayValue = this.translate.instant('widgets.value-action.execute-rpc-text', {methodName});
        break;
      case SetValueAction.SET_ATTRIBUTE:
        this.displayValue = this.translate.instant('widgets.value-action.set-attribute-to-value-text',
          {key: this.modelValue.setAttribute.key, value});
        break;
      case SetValueAction.ADD_TIME_SERIES:
        this.displayValue = this.translate.instant('widgets.value-action.add-time-series-value-text',
          {key: this.modelValue.putTimeSeries.key, value});
        break;
    }
    this.cd.markForCheck();
  }

}
