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

import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatDialog } from '@angular/material/dialog';
import { Dashboard, WidgetLayout } from '@shared/models/dashboard.models';
import { IAliasController, IStateController } from '@core/api/widget-api.models';
import { Widget, WidgetConfigMode } from '@shared/models/widget.models';
import { WidgetComponentService } from '@home/components/widget/widget-component.service';
import { WidgetConfigComponentData } from '../../models/widget-component.models';
import { isDefined, isDefinedAndNotNull, isString } from '@core/utils';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';

@Component({
  selector: 'tb-edit-widget',
  templateUrl: './edit-widget.component.html',
  styleUrls: ['./edit-widget.component.scss']
})
export class EditWidgetComponent extends PageComponent implements OnInit, OnChanges {

  @Input()
  dashboard: Dashboard;

  @Input()
  aliasController: IAliasController;

  @Input()
  stateController: IStateController;

  @Input()
  widgetEditMode: boolean;

  @Input()
  widget: Widget;

  @Input()
  widgetLayout: WidgetLayout;

  @Output()
  applyWidgetConfig = new EventEmitter<void>();

  @Output()
  revertWidgetConfig = new EventEmitter<void>();

  widgetFormGroup: UntypedFormGroup;

  widgetConfig: WidgetConfigComponentData;

  previewMode = false;

  hasBasicMode = false;

  get widgetConfigMode(): WidgetConfigMode {
    return this.hasBasicMode ? (this.widgetConfig?.config?.configMode || WidgetConfigMode.advanced) : WidgetConfigMode.advanced;
  }

  set widgetConfigMode(widgetConfigMode: WidgetConfigMode) {
    if (this.hasBasicMode) {
      this.widgetConfig.config.configMode = widgetConfigMode;
      this.widgetFormGroup.markAsDirty();
    }
  }

  private currentWidgetConfigChanged = false;

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder,
              private widgetComponentService: WidgetComponentService) {
    super(store);
    this.widgetFormGroup = this.fb.group({
      widgetConfig: [null]
    });
  }

  ngOnInit(): void {
    this.loadWidgetConfig();
  }

  ngOnChanges(changes: SimpleChanges): void {
    let reloadConfig = false;
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['widget', 'widgetLayout'].includes(propName)) {
          reloadConfig = true;
        }
      }
    }
    if (reloadConfig) {
      if (this.currentWidgetConfigChanged) {
        this.currentWidgetConfigChanged = false;
      } else {
        this.previewMode = false;
      }
      this.loadWidgetConfig();
    }
  }

  onApplyWidgetConfig() {
    if (this.widgetFormGroup.valid) {
      this.currentWidgetConfigChanged = true;
      this.applyWidgetConfig.emit();
    }
  }

  onRevertWidgetConfig() {
    this.currentWidgetConfigChanged = true;
    this.revertWidgetConfig.emit();
  }

  private loadWidgetConfig() {
    if (!this.widget) {
      return;
    }
    const widgetInfo = this.widgetComponentService.getInstantWidgetInfo(this.widget);
    const rawSettingsSchema = widgetInfo.typeSettingsSchema || widgetInfo.settingsSchema;
    const rawDataKeySettingsSchema = widgetInfo.typeDataKeySettingsSchema || widgetInfo.dataKeySettingsSchema;
    const rawLatestDataKeySettingsSchema = widgetInfo.typeLatestDataKeySettingsSchema || widgetInfo.latestDataKeySettingsSchema;
    const typeParameters = widgetInfo.typeParameters;
    const actionSources = widgetInfo.actionSources;
    const isDataEnabled = isDefined(widgetInfo.typeParameters) ? !widgetInfo.typeParameters.useCustomDatasources : true;
    let settingsSchema;
    if (!rawSettingsSchema || rawSettingsSchema === '') {
      settingsSchema = {};
    } else {
      settingsSchema = isString(rawSettingsSchema) ? JSON.parse(rawSettingsSchema) : rawSettingsSchema;
    }
    let dataKeySettingsSchema;
    if (!rawDataKeySettingsSchema || rawDataKeySettingsSchema === '') {
      dataKeySettingsSchema = {};
    } else {
      dataKeySettingsSchema = isString(rawDataKeySettingsSchema) ? JSON.parse(rawDataKeySettingsSchema) : rawDataKeySettingsSchema;
    }
    let latestDataKeySettingsSchema;
    if (!rawLatestDataKeySettingsSchema || rawLatestDataKeySettingsSchema === '') {
      latestDataKeySettingsSchema = {};
    } else {
      latestDataKeySettingsSchema = isString(rawLatestDataKeySettingsSchema) ?
        JSON.parse(rawLatestDataKeySettingsSchema) : rawLatestDataKeySettingsSchema;
    }
    this.widgetConfig = {
      widgetName: widgetInfo.widgetName,
      config: this.widget.config,
      layout: this.widgetLayout,
      widgetType: this.widget.type,
      typeParameters,
      actionSources,
      isDataEnabled,
      settingsSchema,
      dataKeySettingsSchema,
      latestDataKeySettingsSchema,
      settingsDirective: widgetInfo.settingsDirective,
      dataKeySettingsDirective: widgetInfo.dataKeySettingsDirective,
      latestDataKeySettingsDirective: widgetInfo.latestDataKeySettingsDirective,
    };
    this.hasBasicMode = isDefinedAndNotNull(widgetInfo.hasBasicMode) ? widgetInfo.hasBasicMode : false;
    this.widgetFormGroup.reset({widgetConfig: this.widgetConfig});
  }
}
