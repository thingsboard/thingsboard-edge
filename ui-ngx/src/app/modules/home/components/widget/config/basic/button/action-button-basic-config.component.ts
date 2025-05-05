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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  actionDescriptorToAction,
  Datasource,
  defaultWidgetAction,
  TargetDevice,
  WidgetAction,
  WidgetConfig,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { guid } from '@core/utils';
import { ValueType } from '@shared/models/constants';
import { getTargetDeviceFromDatasources } from '@shared/models/widget-settings.models';
import {
  actionButtonDefaultSettings,
  ActionButtonWidgetSettings
} from '@home/components/widget/lib/button/action-button-widget.models';

@Component({
  selector: 'tb-action-button-basic-config',
  templateUrl: './action-button-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class ActionButtonBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    const datasources: Datasource[] = this.actionButtonWidgetConfigForm.get('datasources').value;
    return getTargetDeviceFromDatasources(datasources);
  }

  valueType = ValueType;

  actionButtonWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.actionButtonWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: ActionButtonWidgetSettings = {...actionButtonDefaultSettings, ...(configData.config.settings || {})};
    const onClickAction = this.getOnClickAction(configData.config);
    this.actionButtonWidgetConfigForm = this.fb.group({
      datasources: [configData.config.datasources, []],

      onClickAction: [onClickAction, []],
      activatedState: [settings.activatedState, []],
      disabledState: [settings.disabledState, []],

      appearance: [settings.appearance, []],

      borderRadius: [configData.config.borderRadius, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {

    this.widgetConfig.config.datasources = config.datasources;
    this.setOnClickAction(this.widgetConfig.config, config.onClickAction);

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.activatedState = config.activatedState;
    this.widgetConfig.config.settings.disabledState = config.disabledState;

    this.widgetConfig.config.settings.appearance = config.appearance;

    this.widgetConfig.config.borderRadius = config.borderRadius;

    return this.widgetConfig;
  }

  private getOnClickAction(config: WidgetConfig): WidgetAction {
    let clickAction: WidgetAction;
    const actions = config.actions;
    if (actions && actions.click) {
      const descriptors = actions.click;
      if (descriptors?.length) {
        const descriptor = descriptors[0];
        clickAction = actionDescriptorToAction(descriptor);
      }
    }
    if (!clickAction) {
      clickAction = defaultWidgetAction();
    }
    return clickAction;
  }

  private setOnClickAction(config: WidgetConfig, clickAction: WidgetAction): void {
    let actions = config.actions;
    if (!actions) {
      actions = {};
      config.actions = actions;
    }
    let descriptors = actions.click;
    if (!descriptors) {
      descriptors = [];
      actions.click = descriptors;
    }
    let descriptor = descriptors[0];
    if (!descriptor) {
      descriptor = {
        id: guid(),
        name: 'onClick',
        icon: 'more_horiz',
        ...clickAction
      };
      descriptors[0] = descriptor;
    } else {
      descriptors[0] = {...descriptor, ...clickAction};
    }
  }
}
