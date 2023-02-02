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

import { Component, forwardRef, Input, OnInit, QueryList, ViewChildren } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  WidgetMobileActionDescriptor,
  WidgetMobileActionType,
  widgetMobileActionTypeTranslationMap
} from '@shared/models/widget.models';
import { CustomActionEditorCompleter } from '@home/components/widget/action/custom-action.models';
import { JsFuncComponent } from '@shared/components/js-func.component';
import {
  getDefaultGetLocationFunction,
  getDefaultGetPhoneNumberFunction,
  getDefaultHandleEmptyResultFunction,
  getDefaultHandleErrorFunction,
  getDefaultProcessImageFunction,
  getDefaultProcessLaunchResultFunction,
  getDefaultProcessLocationFunction,
  getDefaultProcessQrCodeFunction
} from '@home/components/widget/action/mobile-action-editor.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-mobile-action-editor',
  templateUrl: './mobile-action-editor.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => MobileActionEditorComponent),
    multi: true
  }]
})
export class MobileActionEditorComponent implements ControlValueAccessor, OnInit {

  @ViewChildren(JsFuncComponent) jsFuncComponents: QueryList<JsFuncComponent>;

  mobileActionTypes = Object.keys(WidgetMobileActionType);
  mobileActionTypeTranslations = widgetMobileActionTypeTranslationMap;
  mobileActionType = WidgetMobileActionType;

  customActionEditorCompleter = CustomActionEditorCompleter;

  mobileActionFormGroup: FormGroup;
  mobileActionTypeFormGroup: FormGroup;

  functionScopeVariables: string[];

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder,
              private widgetService: WidgetService) {
    this.functionScopeVariables = this.widgetService.getWidgetScopeVariables();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.mobileActionFormGroup = this.fb.group({
      type: [null, Validators.required],
      handleEmptyResultFunction: [null],
      handleErrorFunction: [null]
    });
    this.mobileActionFormGroup.get('type').valueChanges.subscribe((type: WidgetMobileActionType) => {
      let action: WidgetMobileActionDescriptor = this.mobileActionFormGroup.value;
      if (this.mobileActionTypeFormGroup) {
        action = {...action, ...this.mobileActionTypeFormGroup.value};
      }
      this.updateMobileActionType(type, action);
    });
    this.mobileActionFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.mobileActionFormGroup.disable({emitEvent: false});
      if (this.mobileActionTypeFormGroup) {
        this.mobileActionTypeFormGroup.disable({emitEvent: false});
      }
    } else {
      this.mobileActionFormGroup.enable({emitEvent: false});
      if (this.mobileActionTypeFormGroup) {
        this.mobileActionTypeFormGroup.enable({emitEvent: false});
      }
    }
  }

  writeValue(value: WidgetMobileActionDescriptor | null): void {
    this.mobileActionFormGroup.patchValue({type: value?.type,
                                                 handleEmptyResultFunction: value?.handleEmptyResultFunction,
                                                 handleErrorFunction: value?.handleErrorFunction}, {emitEvent: false});
    this.updateMobileActionType(value?.type, value);
  }

  private updateModel() {
    let descriptor: WidgetMobileActionDescriptor = null;
    if (this.mobileActionFormGroup.valid && this.mobileActionTypeFormGroup.valid) {
      descriptor = { ...this.mobileActionFormGroup.getRawValue(), ...this.mobileActionTypeFormGroup.getRawValue() };
    }
    this.propagateChange(descriptor);
  }

  private updateMobileActionType(type?: WidgetMobileActionType, action?: WidgetMobileActionDescriptor) {
    const prevType = action?.type;
    const targetType = type || prevType;
    const changed = prevType !== type;
    if (changed && targetType) {
      let handleEmptyResultFunction = action?.handleEmptyResultFunction;
      const defaultHandleEmptyResultFunction = getDefaultHandleEmptyResultFunction(targetType);
      if (defaultHandleEmptyResultFunction !== handleEmptyResultFunction) {
        handleEmptyResultFunction = getDefaultHandleEmptyResultFunction(type);
        this.mobileActionFormGroup.patchValue({handleEmptyResultFunction}, {emitEvent: false});
      }
      let handleErrorFunction = action?.handleErrorFunction;
      const defaultHandleErrorFunction = getDefaultHandleErrorFunction(targetType);
      if (defaultHandleErrorFunction !== handleErrorFunction) {
        handleErrorFunction = getDefaultHandleErrorFunction(type);
        this.mobileActionFormGroup.patchValue({handleErrorFunction}, {emitEvent: false});
      }
    }
    this.mobileActionTypeFormGroup = this.fb.group({});
    if (type) {
      switch (type) {
        case WidgetMobileActionType.takePictureFromGallery:
        case WidgetMobileActionType.takePhoto:
        case WidgetMobileActionType.takeScreenshot:
          let processImageFunction = action?.processImageFunction;
          if (changed) {
            const defaultProcessImageFunction = getDefaultProcessImageFunction(targetType);
            if (defaultProcessImageFunction !== processImageFunction) {
              processImageFunction = getDefaultProcessImageFunction(type);
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'processImageFunction',
            this.fb.control(processImageFunction, [])
          );
          break;
        case WidgetMobileActionType.mapDirection:
        case WidgetMobileActionType.mapLocation:
          let getLocationFunction = action?.getLocationFunction;
          let processLaunchResultFunction = action?.processLaunchResultFunction;
          if (changed) {
            const defaultGetLocationFunction = getDefaultGetLocationFunction();
            if (defaultGetLocationFunction !== getLocationFunction) {
              getLocationFunction = defaultGetLocationFunction;
            }
            const defaultProcessLaunchResultFunction = getDefaultProcessLaunchResultFunction(targetType);
            if (defaultProcessLaunchResultFunction !== processLaunchResultFunction) {
              processLaunchResultFunction = getDefaultProcessLaunchResultFunction(type);
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'getLocationFunction',
            this.fb.control(getLocationFunction, [Validators.required])
          );
          this.mobileActionTypeFormGroup.addControl(
            'processLaunchResultFunction',
            this.fb.control(processLaunchResultFunction, [])
          );
          break;
        case WidgetMobileActionType.scanQrCode:
          let processQrCodeFunction = action?.processQrCodeFunction;
          if (changed) {
            const defaultProcessQrCodeFunction = getDefaultProcessQrCodeFunction();
            if (defaultProcessQrCodeFunction !== processQrCodeFunction) {
              processQrCodeFunction = defaultProcessQrCodeFunction;
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'processQrCodeFunction',
            this.fb.control(processQrCodeFunction, [])
          );
          break;
        case WidgetMobileActionType.makePhoneCall:
          let getPhoneNumberFunction = action?.getPhoneNumberFunction;
          processLaunchResultFunction = action?.processLaunchResultFunction;
          if (changed) {
            const defaultGetPhoneNumberFunction = getDefaultGetPhoneNumberFunction();
            if (defaultGetPhoneNumberFunction !== getPhoneNumberFunction) {
              getPhoneNumberFunction = defaultGetPhoneNumberFunction;
            }
            const defaultProcessLaunchResultFunction = getDefaultProcessLaunchResultFunction(targetType);
            if (defaultProcessLaunchResultFunction !== processLaunchResultFunction) {
              processLaunchResultFunction = getDefaultProcessLaunchResultFunction(type);
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'getPhoneNumberFunction',
            this.fb.control(getPhoneNumberFunction, [Validators.required])
          );
          this.mobileActionTypeFormGroup.addControl(
            'processLaunchResultFunction',
            this.fb.control(processLaunchResultFunction, [])
          );
          break;
        case WidgetMobileActionType.getLocation:
          let processLocationFunction = action?.processLocationFunction;
          if (changed) {
            const defaultProcessLocationFunction = getDefaultProcessLocationFunction();
            if (defaultProcessLocationFunction !== processLocationFunction) {
              processLocationFunction = defaultProcessLocationFunction;
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'processLocationFunction',
            this.fb.control(processLocationFunction, [Validators.required])
          );
          break;
      }
    }
    this.mobileActionTypeFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  public validateOnSubmit() {
    for (const jsFuncComponent of this.jsFuncComponents.toArray()) {
      jsFuncComponent.validateOnSubmit();
    }
  }
}
