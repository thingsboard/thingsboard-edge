///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Inject, OnInit, SkipSelf, ViewChild, ViewEncapsulation } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroupDirective, NgForm, UntypedFormBuilder, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { Widget, WidgetConfigMode, widgetTypesData } from '@shared/models/widget.models';
import { Dashboard } from '@app/shared/models/dashboard.models';
import { IAliasController, IStateController } from '@core/api/widget-api.models';
import { WidgetConfigComponentData, WidgetInfo } from '@home/models/widget-component.models';
import { isDefined, isDefinedAndNotNull, isString } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeySettingsFunction } from '@home/components/widget/config/data-keys.component.models';

export interface AddWidgetDialogData {
  dashboard: Dashboard;
  aliasController: IAliasController;
  stateController: IStateController;
  widget: Widget;
  widgetInfo: WidgetInfo;
  showLayoutConfig: boolean;
  isDefaultBreakpoint: boolean;
}

@Component({
  selector: 'tb-add-widget-dialog',
  templateUrl: './add-widget-dialog.component.html',
  providers: [/*{provide: ErrorStateMatcher, useExisting: AddWidgetDialogComponent}*/],
  styleUrls: ['./add-widget-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class AddWidgetDialogComponent extends DialogComponent<AddWidgetDialogComponent, Widget>
  implements OnInit, ErrorStateMatcher {

  @ViewChild('widgetConfigComponent')
  widgetConfigComponent: WidgetConfigComponent;

  widgetFormGroup: UntypedFormGroup;

  dashboard: Dashboard;
  aliasController: IAliasController;
  stateController: IStateController;
  widget: Widget;

  showLayoutConfig = true;
  isDefaultBreakpoint = true;

  widgetConfig: WidgetConfigComponentData;

  previewMode = false;

  hideHeader = false;

  private readonly initialWidgetConfigMode: WidgetConfigMode;

  get widgetConfigMode(): WidgetConfigMode {
    return this.widgetConfigComponent?.widgetConfigMode || this.initialWidgetConfigMode;
  }

  set widgetConfigMode(widgetConfigMode: WidgetConfigMode) {
    this.widgetConfigComponent.setWidgetConfigMode(widgetConfigMode);
    this.hideHeader = this.widgetConfigComponent?.widgetConfigMode === WidgetConfigMode.basic;
  }

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: AddWidgetDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddWidgetDialogComponent, Widget>,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.dashboard = this.data.dashboard;
    this.aliasController = this.data.aliasController;
    this.stateController = this.data.stateController;
    this.widget = this.data.widget;
    this.showLayoutConfig = this.data.showLayoutConfig;
    this.isDefaultBreakpoint = this.data.isDefaultBreakpoint;

    const widgetInfo = this.data.widgetInfo;

    const rawSettingsSchema = widgetInfo.typeSettingsSchema || widgetInfo.settingsSchema;
    const rawDataKeySettingsSchema = widgetInfo.typeDataKeySettingsSchema || widgetInfo.dataKeySettingsSchema;
    const rawLatestDataKeySettingsSchema = widgetInfo.typeLatestDataKeySettingsSchema || widgetInfo.latestDataKeySettingsSchema;
    const typeParameters = widgetInfo.typeParameters;
    const dataKeySettingsFunction: DataKeySettingsFunction = typeParameters?.dataKeySettingsFunction;
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
      layout: {},
      widgetType: this.widget.type,
      typeParameters,
      actionSources,
      isDataEnabled,
      settingsSchema,
      dataKeySettingsSchema,
      latestDataKeySettingsSchema,
      dataKeySettingsFunction,
      settingsDirective: widgetInfo.settingsDirective,
      dataKeySettingsDirective: widgetInfo.dataKeySettingsDirective,
      latestDataKeySettingsDirective: widgetInfo.latestDataKeySettingsDirective,
      hasBasicMode: isDefinedAndNotNull(widgetInfo.hasBasicMode) ? widgetInfo.hasBasicMode : false,
      basicModeDirective: widgetInfo.basicModeDirective
    };
    if (this.widgetConfig.hasBasicMode && this.widgetConfig.config?.configMode === WidgetConfigMode.basic) {
      this.hideHeader = true;
      this.initialWidgetConfigMode = WidgetConfigMode.basic;
    } else {
      this.initialWidgetConfigMode = WidgetConfigMode.advanced;
    }

    this.widgetFormGroup = this.fb.group({
        widgetConfig: [this.widgetConfig, []]
      }
    );
  }

  ngOnInit() {
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  helpLinkIdForWidgetType(): string {
    let link = 'widgetsConfig';
    if (this.widget && this.widget.type) {
      link = widgetTypesData.get(this.widget.type).configHelpLinkId;
    }
    return link;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.submitted = true;
    const widgetConfig: WidgetConfigComponentData = this.widgetFormGroup.get('widgetConfig').value;
    this.widget.config = widgetConfig.config;
    this.widget.config.mobileOrder = widgetConfig.layout.mobileOrder;
    this.widget.config.mobileHeight = widgetConfig.layout.mobileHeight;
    this.widget.config.mobileHide = widgetConfig.layout.mobileHide;
    this.widget.config.desktopHide = widgetConfig.layout.desktopHide;
    this.dialogRef.close(this.widget);
  }
}
