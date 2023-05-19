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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { Widget, widgetTypesData } from '@shared/models/widget.models';
import { Dashboard } from '@app/shared/models/dashboard.models';
import { IAliasController, IStateController } from '@core/api/widget-api.models';
import { WidgetConfigComponentData, WidgetInfo } from '@home/models/widget-component.models';
import { isDefined, isString } from '@core/utils';

export interface AddWidgetDialogData {
  dashboard: Dashboard;
  aliasController: IAliasController;
  stateController: IStateController;
  widget: Widget;
  widgetInfo: WidgetInfo;
}

@Component({
  selector: 'tb-add-widget-dialog',
  templateUrl: './add-widget-dialog.component.html',
  providers: [/*{provide: ErrorStateMatcher, useExisting: AddWidgetDialogComponent}*/],
  styleUrls: []
})
export class AddWidgetDialogComponent extends DialogComponent<AddWidgetDialogComponent, Widget>
  implements OnInit, ErrorStateMatcher {

  widgetFormGroup: UntypedFormGroup;

  dashboard: Dashboard;
  aliasController: IAliasController;
  stateController: IStateController;
  widget: Widget;

  previewMode = false;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddWidgetDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddWidgetDialogComponent, Widget>,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.dashboard = this.data.dashboard;
    this.aliasController = this.data.aliasController;
    this.stateController = this.data.stateController;
    this.widget = this.data.widget;

    const widgetInfo = this.data.widgetInfo;

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
    const widgetConfig: WidgetConfigComponentData = {
      config: this.widget.config,
      layout: {},
      widgetType: this.widget.type,
      typeParameters,
      actionSources,
      isDataEnabled,
      settingsSchema,
      dataKeySettingsSchema,
      latestDataKeySettingsSchema,
      settingsDirective: widgetInfo.settingsDirective,
      dataKeySettingsDirective: widgetInfo.dataKeySettingsDirective,
      latestDataKeySettingsDirective: widgetInfo.latestDataKeySettingsDirective
    };

    this.widgetFormGroup = this.fb.group({
        widgetConfig: [widgetConfig, []]
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
