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
import { RpcInitialStateAction, RpcInitialStateSettings } from '@shared/models/rpc-widget-settings.models';
import { TranslateService } from '@ngx-translate/core';
import { ValueType } from '@shared/models/constants';
import {
  RpcInitialStateSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/rpc/rpc-initial-state-settings-panel.component';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice } from '@shared/models/widget.models';

@Component({
  selector: 'tb-rpc-initial-state-settings',
  templateUrl: './rpc-state-settings-button.component.html',
  styleUrls: ['./rpc-state-settings-button.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RpcInitialStateSettingsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class RpcInitialStateSettingsComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.overflow')
  overflow = 'hidden';

  @Input()
  stateValueType: ValueType;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  disabled = false;

  modelValue: RpcInitialStateSettings<any>;

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

  writeValue(value: RpcInitialStateSettings<any>): void {
    this.modelValue = value;
    this.updateDisplayValue();
  }

  openRpcStateSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        initialState: this.modelValue,
        stateValueType: this.stateValueType,
        aliasController: this.aliasController,
        targetDevice: this.targetDevice
      };
      const initialStateSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, RpcInitialStateSettingsPanelComponent,
        ['leftTopOnly', 'leftOnly', 'leftBottomOnly'], true, null,
        ctx,
        {},
        {}, {}, true);
      initialStateSettingsPanelPopover.tbComponentRef.instance.popover = initialStateSettingsPanelPopover;
      initialStateSettingsPanelPopover.tbComponentRef.instance.initialStateSettingsApplied.subscribe((initialState) => {
        initialStateSettingsPanelPopover.hide();
        this.modelValue = initialState;
        this.updateDisplayValue();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateDisplayValue() {
    switch (this.modelValue.action) {
      case RpcInitialStateAction.DO_NOTHING:
        if (this.stateValueType === ValueType.BOOLEAN) {
          this.displayValue = this.translate.instant(!!this.modelValue.defaultValue ? 'widgets.rpc-state.on' : 'widgets.rpc-state.off');
        } else {
          this.displayValue = this.modelValue.defaultValue + '';
        }
        break;
      case RpcInitialStateAction.EXECUTE_RPC:
        const methodName = this.modelValue.executeRpc.method;
        this.displayValue = this.translate.instant('widgets.rpc-state.execute-rpc-text', {methodName});
        break;
      case RpcInitialStateAction.GET_ATTRIBUTE:
        this.displayValue = this.translate.instant('widgets.rpc-state.get-attribute-text', {key: this.modelValue.getAttribute.key});
        break;
      case RpcInitialStateAction.GET_TIME_SERIES:
        this.displayValue = this.translate.instant('widgets.rpc-state.get-time-series-text', {key: this.modelValue.getTimeSeries.key});
        break;
    }
    this.cd.markForCheck();
  }

}
