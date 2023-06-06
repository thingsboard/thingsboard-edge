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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { widgetType } from '@shared/models/widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';

export interface TimewindowConfigData {
  useDashboardTimewindow: boolean;
  displayTimewindow: boolean;
  timewindow: Timewindow;
}

@Component({
  selector: 'tb-timewindow-config-panel',
  templateUrl: './timewindow-config-panel.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimewindowConfigPanelComponent),
      multi: true
    }
  ]
})
export class TimewindowConfigPanelComponent implements ControlValueAccessor, OnInit {

  widgetTypes = widgetType;

  public get widgetType(): widgetType {
    return this.widgetConfigComponent.widgetType;
  }

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  onlyHistoryTimewindow = false;


  timewindowConfig: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              public translate: TranslateService,
              private widgetConfigComponent: WidgetConfigComponent) {
  }

  ngOnInit() {
    this.timewindowConfig = this.fb.group({
      useDashboardTimewindow: [null, []],
      displayTimewindow: [null, []],
      timewindow: [null, []]
    });
    this.timewindowConfig.valueChanges.subscribe(
      (val) => this.propagateChange(val)
    );
    this.timewindowConfig.get('useDashboardTimewindow').valueChanges.subscribe(() => {
      this.updateTimewindowConfigEnabledState();
    });
  }

  writeValue(data?: TimewindowConfigData): void {
    this.timewindowConfig.patchValue(data || {}, {emitEvent: false});
    this.updateTimewindowConfigEnabledState();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.timewindowConfig.disable({emitEvent: false});
    } else {
      this.timewindowConfig.enable({emitEvent: false});
      this.updateTimewindowConfigEnabledState();
    }
  }

  private updateTimewindowConfigEnabledState() {
    const useDashboardTimewindow: boolean = this.timewindowConfig.get('useDashboardTimewindow').value;
    if (useDashboardTimewindow) {
      this.timewindowConfig.get('displayTimewindow').disable({emitEvent: false});
      this.timewindowConfig.get('timewindow').disable({emitEvent: false});
    } else {
      this.timewindowConfig.get('displayTimewindow').enable({emitEvent: false});
      this.timewindowConfig.get('timewindow').enable({emitEvent: false});
    }
  }

}
