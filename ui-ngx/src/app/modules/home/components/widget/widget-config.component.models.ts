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

import { WidgetActionCallbacks } from './action/manage-widget-actions.component.models';
import { DatasourceCallbacks } from '@home/components/widget/datasource.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import { AfterViewInit, Directive, EventEmitter, Inject, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AbstractControl, UntypedFormGroup } from '@angular/forms';
import { WidgetConfigMode } from '@shared/models/widget.models';

export type WidgetConfigCallbacks = DatasourceCallbacks & WidgetActionCallbacks;

export interface IBasicWidgetConfigComponent {

  widgetConfig: WidgetConfigComponentData;
  widgetConfigChanged: Observable<WidgetConfigComponentData>;
  validateConfig(): boolean;

}

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class BasicWidgetConfigComponent extends PageComponent implements
  IBasicWidgetConfigComponent, OnInit, AfterViewInit {

  basicMode = WidgetConfigMode.basic;

  widgetConfigValue: WidgetConfigComponentData;

  set widgetConfig(value: WidgetConfigComponentData) {
    this.widgetConfigValue = value;
    this.setupConfig(this.widgetConfigValue);
  }

  get widgetConfig(): WidgetConfigComponentData {
    return this.widgetConfigValue;
  }

  widgetConfigChangedEmitter = new EventEmitter<WidgetConfigComponentData>();
  widgetConfigChanged = this.widgetConfigChangedEmitter.asObservable();

  protected constructor(@Inject(Store) protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (!this.validateConfig()) {
        this.onConfigChanged(this.prepareOutputConfig(this.configForm().value));
      }
    }, 0);
  }

  protected setupConfig(widgetConfig: WidgetConfigComponentData) {
    this.onConfigSet(widgetConfig);
    this.updateValidators(false);
    for (const trigger of this.validatorTriggers()) {
      const path = trigger.split('.');
      let control: AbstractControl = this.configForm();
      for (const part of path) {
        control = control.get(part);
      }
      control.valueChanges.subscribe(() => {
        this.updateValidators(true, trigger);
      });
    }
    this.configForm().valueChanges.subscribe((updated: any) => {
      this.onConfigChanged(this.prepareOutputConfig(updated));
    });
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
  }

  protected validatorTriggers(): string[] {
    return [];
  }

  protected onConfigChanged(widgetConfig: WidgetConfigComponentData) {
    this.widgetConfigValue = widgetConfig;
    this.widgetConfigChangedEmitter.emit(this.widgetConfigValue);
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    return config;
  }

  public validateConfig(): boolean {
    return this.configForm().valid;
  }

  protected abstract configForm(): UntypedFormGroup;

  protected abstract onConfigSet(configData: WidgetConfigComponentData);

}
