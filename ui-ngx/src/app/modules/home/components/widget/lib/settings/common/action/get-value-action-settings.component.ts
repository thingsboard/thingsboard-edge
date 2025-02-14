///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import { DataToValueType, GetValueAction, GetValueSettings } from '@shared/models/action-widget-settings.models';
import { TranslateService } from '@ngx-translate/core';
import { ValueType } from '@shared/models/constants';
import {
  GetValueActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/get-value-action-settings-panel.component';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice, widgetType } from '@shared/models/widget.models';

@Component({
  selector: 'tb-get-value-action-settings',
  templateUrl: './action-settings-button.component.html',
  styleUrls: ['./action-settings-button.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GetValueActionSettingsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class GetValueActionSettingsComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.overflow')
  overflow = 'hidden';

  @Input()
  panelTitle: string;

  @Input()
  valueType: ValueType;

  @Input()
  trueLabel: string;

  @Input()
  falseLabel: string;

  @Input()
  stateLabel: string;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  widgetType: widgetType;

  @Input()
  disabled = false;

  modelValue: GetValueSettings<any>;

  displayValue: string;

  private propagateChange = null;

  constructor(private translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
    if (!this.trueLabel) {
      this.trueLabel = this.translate.instant('value.true');
    }
    if (!this.falseLabel) {
      this.falseLabel = this.translate.instant('value.false');
    }
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

  writeValue(value: GetValueSettings<any>): void {
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
        getValueSettings: this.modelValue,
        panelTitle: this.panelTitle,
        valueType: this.valueType,
        trueLabel: this.trueLabel,
        falseLabel: this.falseLabel,
        stateLabel: this.stateLabel,
        aliasController: this.aliasController,
        targetDevice: this.targetDevice,
        widgetType: this.widgetType
      };
      const getValueSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, GetValueActionSettingsPanelComponent,
        ['leftTopOnly', 'leftOnly', 'leftBottomOnly'], true, null,
        ctx,
        {},
        {}, {}, true);
      getValueSettingsPanelPopover.tbComponentRef.instance.popover = getValueSettingsPanelPopover;
      getValueSettingsPanelPopover.tbComponentRef.instance.getValueSettingsApplied.subscribe((getValueSettings) => {
        getValueSettingsPanelPopover.hide();
        this.modelValue = getValueSettings;
        this.updateDisplayValue();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateDisplayValue() {
    switch (this.modelValue.action) {
      case GetValueAction.DO_NOTHING:
        if (this.valueType === ValueType.BOOLEAN) {
          this.displayValue =
            !!this.modelValue.defaultValue ? this.trueLabel : this.falseLabel;
        } else {
          this.displayValue = this.modelValue.defaultValue + '';
        }
        break;
      case GetValueAction.EXECUTE_RPC:
        const methodName = this.modelValue.executeRpc.method;
        this.displayValue = this.translate.instant('widgets.value-action.execute-rpc-text', {methodName});
        break;
      case GetValueAction.GET_ATTRIBUTE:
        this.displayValue = this.translate.instant('widgets.value-action.get-attribute-text', {key: this.modelValue.getAttribute.key});
        break;
      case GetValueAction.GET_TIME_SERIES:
        this.displayValue = this.translate.instant('widgets.value-action.get-time-series-text', {key: this.modelValue.getTimeSeries.key});
        break;
      case GetValueAction.GET_ALARM_STATUS:
        this.displayValue = this.translate.instant('widgets.value-action.get-alarm-status-text');
        break;
      case GetValueAction.GET_DASHBOARD_STATE:
        if (this.valueType === ValueType.BOOLEAN) {
          const state = this.modelValue.dataToValue?.compareToValue;
          if (this.modelValue.dataToValue?.type === DataToValueType.FUNCTION) {
            this.displayValue = this.translate.instant('widgets.value-action.when-dashboard-state-function-is-text', {state});
          } else {
            this.displayValue = this.translate.instant('widgets.value-action.when-dashboard-state-is-text', {state});
          }
        } else {
          this.displayValue = this.translate.instant('widgets.value-action.get-dashboard-state-text');
        }
        break;
      case GetValueAction.GET_DASHBOARD_STATE_OBJECT:
        if (this.valueType === ValueType.BOOLEAN) {
          const state = this.modelValue.dataToValue?.compareToValue;
          this.displayValue = this.translate.instant('widgets.value-action.when-dashboard-state-object-function-is-text', {state});
        } else {
          this.displayValue = this.translate.instant('widgets.value-action.get-dashboard-state-object-text');
        }
        break;
    }
    this.cd.markForCheck();
  }

}
