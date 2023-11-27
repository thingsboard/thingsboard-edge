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

import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  ComponentRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Subscription } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { JsonFormComponent } from '@shared/components/json-form/json-form.component';
import { JsonFormComponentData } from '@shared/components/json-form/json-form-component.models';
import { IWidgetSettingsComponent, Widget, WidgetSettings } from '@shared/models/widget.models';
import { widgetSettingsComponentsMap } from '@home/components/widget/lib/settings/widget-settings.module';
import { Dashboard } from '@shared/models/dashboard.models';
import { WidgetService } from '@core/http/widget.service';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';

@Component({
  selector: 'tb-widget-settings',
  templateUrl: './widget-settings.component.html',
  styleUrls: ['./widget-settings.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => WidgetSettingsComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => WidgetSettingsComponent),
    multi: true
  }]
})
export class WidgetSettingsComponent implements ControlValueAccessor, OnInit, OnDestroy, AfterViewInit, OnChanges, Validator {

  @ViewChild('definedSettingsContent', {read: ViewContainerRef, static: true}) definedSettingsContainer: ViewContainerRef;

  @ViewChild('jsonFormComponent') jsonFormComponent: JsonFormComponent;

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  dashboard: Dashboard;

  @Input()
  widget: Widget;

  @Input()
  widgetConfig: WidgetConfigComponentData;

  private settingsDirective: string;

  definedDirectiveError: string;

  widgetSettingsFormGroup: UntypedFormGroup;

  changeSubscription: Subscription;

  private definedSettingsComponentRef: ComponentRef<IWidgetSettingsComponent>;
  private definedSettingsComponent: IWidgetSettingsComponent;

  private widgetSettingsFormData: JsonFormComponentData;
  private propagateChange = (v: any) => { };

  constructor(private translate: TranslateService,
              private cfr: ComponentFactoryResolver,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder) {
    this.widgetSettingsFormGroup = this.fb.group({
      settings: [null, Validators.required]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'widget') {
          if (this.definedSettingsComponent) {
            this.definedSettingsComponent.widget = this.widget;
          }
        }
        if (propName === 'dashboard') {
          if (this.definedSettingsComponent) {
            this.definedSettingsComponent.dashboard = this.dashboard;
          }
        }
        if (propName === 'aliasController') {
          if (this.definedSettingsComponent) {
            this.definedSettingsComponent.aliasController = this.aliasController;
          }
        }
        if (propName === 'widgetConfig') {
          if (this.definedSettingsComponent) {
            this.definedSettingsComponent.widgetConfig = this.widgetConfig;
          }
        }
      }
    }
  }

  ngOnDestroy(): void {
    if (this.definedSettingsComponentRef) {
      this.definedSettingsComponentRef.destroy();
    }
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.widgetSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.widgetSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: JsonFormComponentData): void {
    this.widgetSettingsFormData = value;
    if (this.changeSubscription) {
      this.changeSubscription.unsubscribe();
      this.changeSubscription = null;
    }
    if (this.settingsDirective !== this.widgetSettingsFormData.settingsDirective) {
      this.settingsDirective = this.widgetSettingsFormData.settingsDirective;
      this.validateDefinedDirective();
    }
    if (this.definedSettingsComponent) {
      this.definedSettingsComponent.settings = this.widgetSettingsFormData.model;
      this.changeSubscription = this.definedSettingsComponent.settingsChanged.subscribe((settings) => {
        this.updateModel(settings);
      });
    } else {
      this.widgetSettingsFormGroup.get('settings').patchValue(this.widgetSettingsFormData, {emitEvent: false});
      this.changeSubscription = this.widgetSettingsFormGroup.get('settings').valueChanges.subscribe(
        (widgetSettingsFormData: JsonFormComponentData) => {
          this.updateModel(widgetSettingsFormData.model);
        }
      );
    }
  }

  useDefinedDirective(): boolean {
    return this.settingsDirective &&
      this.settingsDirective.length && !this.definedDirectiveError;
  }

  useJsonForm(): boolean {
    return !this.settingsDirective || !this.settingsDirective.length;
  }

  private updateModel(settings: WidgetSettings) {
    this.widgetSettingsFormData.model = settings;
    this.propagateChange(this.widgetSettingsFormData);
  }

  private validateDefinedDirective() {
    if (this.definedSettingsComponentRef) {
      this.definedSettingsComponentRef.destroy();
      this.definedSettingsComponentRef = null;
      this.definedSettingsComponent = null;
    }
    if (this.settingsDirective && this.settingsDirective.length) {
      const componentType = widgetSettingsComponentsMap[this.settingsDirective];
      if (!componentType) {
        this.definedDirectiveError = this.translate.instant('widget-config.settings-component-not-found',
          {selector: this.settingsDirective});
      } else {
        if (this.changeSubscription) {
          this.changeSubscription.unsubscribe();
          this.changeSubscription = null;
        }
        this.definedSettingsContainer.clear();
        const factory = this.cfr.resolveComponentFactory(componentType);
        this.definedSettingsComponentRef = this.definedSettingsContainer.createComponent(factory);
        this.definedSettingsComponent = this.definedSettingsComponentRef.instance;
        this.definedSettingsComponent.aliasController = this.aliasController;
        this.definedSettingsComponent.dashboard = this.dashboard;
        this.definedSettingsComponent.widget = this.widget;
        this.definedSettingsComponent.widgetConfig = this.widgetConfig;
        this.definedSettingsComponent.functionScopeVariables = this.widgetService.getWidgetScopeVariables();
        this.changeSubscription = this.definedSettingsComponent.settingsChanged.subscribe((settings) => {
          this.updateModel(settings);
        });
      }
    }
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.useDefinedDirective()) {
      if (!this.definedSettingsComponent.validateSettings()) {
        return {
          widgetSettings: {
            valid: false
          }
        };
      }
    } else if (this.useJsonForm()) {
      if (!this.widgetSettingsFormGroup.valid) {
        return {
          widgetSettings: {
            valid: false
          }
        };
      }
    }
    return null;
  }
}
