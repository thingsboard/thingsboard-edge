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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { WidgetConfig, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { isUndefined } from '@core/utils';
import { cssSizeToStrSize, resolveCssSize } from '@shared/models/widget-settings.models';
import {
  unreadNotificationDefaultSettings,
  UnreadNotificationWidgetSettings
} from '@home/components/widget/lib/cards/unread-notification-widget.models';

@Component({
  selector: 'tb-unread-notification-basic-config',
  templateUrl: './unread-notification-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class UnreadNotificationBasicConfigComponent extends BasicWidgetConfigComponent {

  unreadNotificationWidgetConfigForm: UntypedFormGroup;

  countPreviewFn = this._countPreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.unreadNotificationWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const iconSize = resolveCssSize(configData.config.iconSize);
    const settings: UnreadNotificationWidgetSettings = {...unreadNotificationDefaultSettings, ...(configData.config.settings || {})};
    this.unreadNotificationWidgetConfigForm = this.fb.group({

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      maxNotificationDisplay: [settings.maxNotificationDisplay, [Validators.required, Validators.min(1)]],
      showCounter: [settings.showCounter, []],
      counterValueFont: [settings.counterValueFont, []],
      counterValueColor: [settings.counterValueColor, []],
      counterColor: [settings.counterColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      actions: [configData.config.actions || {}, []]
    });
  }
  protected validatorTriggers(): string[] {
    return ['showCounter', 'showTitle', 'showIcon'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showCounter: boolean = this.unreadNotificationWidgetConfigForm.get('showCounter').value;
    const showTitle: boolean = this.unreadNotificationWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.unreadNotificationWidgetConfigForm.get('showIcon').value;

    if (showIcon) {
      this.unreadNotificationWidgetConfigForm.get('iconSize').enable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('iconSizeUnit').enable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('icon').enable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('iconColor').enable({emitEvent});
    } else {
      this.unreadNotificationWidgetConfigForm.get('iconSize').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('iconSizeUnit').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('icon').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('iconColor').disable({emitEvent});
    }

    if (showCounter) {
      this.unreadNotificationWidgetConfigForm.get('counterValueFont').enable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('counterValueColor').enable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('counterColor').enable({emitEvent});
    } else {
      this.unreadNotificationWidgetConfigForm.get('counterValueFont').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('counterValueColor').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('counterColor').disable({emitEvent});
    }

    if (showTitle) {
      this.unreadNotificationWidgetConfigForm.get('title').enable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('titleFont').enable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('titleColor').enable({emitEvent});

      this.unreadNotificationWidgetConfigForm.get('showCounter').enable({emitEvent: false});
      this.unreadNotificationWidgetConfigForm.get('showIcon').enable({emitEvent: false});
    } else {
      this.unreadNotificationWidgetConfigForm.get('title').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('titleFont').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('titleColor').disable({emitEvent});

      this.unreadNotificationWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.unreadNotificationWidgetConfigForm.get('iconSize').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('iconSizeUnit').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('icon').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('iconColor').disable({emitEvent});

      this.unreadNotificationWidgetConfigForm.get('showCounter').disable({emitEvent: false});
      this.unreadNotificationWidgetConfigForm.get('counterValueFont').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('counterValueColor').disable({emitEvent});
      this.unreadNotificationWidgetConfigForm.get('counterColor').disable({emitEvent});
    }
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.icon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.maxNotificationDisplay = config.maxNotificationDisplay;
    this.widgetConfig.config.settings.showCounter = config.showCounter;
    this.widgetConfig.config.settings.counterValueFont = config.counterValueFont;
    this.widgetConfig.config.settings.counterValueColor = config.counterValueColor;
    this.widgetConfig.config.settings.counterColor = config.counterColor;

    this.widgetConfig.config.settings.background = config.background;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    return this.widgetConfig;
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.settings?.enableViewAll) || config.settings?.enableViewAll) {
      buttons.push('viewAll');
    }
    if (isUndefined(config.settings?.enableFilter) || config.settings?.enableFilter) {
      buttons.push('filter');
    }
    if (isUndefined(config.settings?.enableMarkAsRead) || config.settings?.enableMarkAsRead) {
      buttons.push('markAsRead');
    }
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.settings.enableViewAll = buttons.includes('viewAll');
    config.settings.enableFilter = buttons.includes('filter');
    config.settings.enableMarkAsRead = buttons.includes('markAsRead');

    config.enableFullscreen = buttons.includes('fullscreen');
  }

  private _countPreviewFn(): string {
    return this.unreadNotificationWidgetConfigForm.get('maxNotificationDisplay').value?.toString() || '6';
  }

}
