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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { merge } from 'rxjs';
import {
  defaultGetValueSettings, defaultSetValueSettings, defaultWidgetActionSettings,
  ScadaSymbolBehavior,
  ScadaSymbolBehaviorType,
  scadaSymbolBehaviorTypes,
  scadaSymbolBehaviorTypeTranslations
} from '@app/modules/home/components/widget/lib/scada/scada-symbol.models';
import { ValueType, valueTypesMap } from '@shared/models/constants';
import { GetValueSettings, SetValueSettings, ValueToDataType } from '@shared/models/action-widget-settings.models';
import { WidgetService } from '@core/http/widget.service';
import { mergeDeep } from '@core/utils';
import { WidgetAction, widgetType } from '@shared/models/widget.models';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-scada-symbol-behavior-panel',
  templateUrl: './scada-symbol-behavior-panel.component.html',
  styleUrls: ['./scada-symbol-behavior-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolBehaviorPanelComponent implements OnInit {

  widgetType = widgetType;

  ScadaSymbolBehaviorType = ScadaSymbolBehaviorType;

  ValueType = ValueType;

  ValueToDataType = ValueToDataType;

  scadaSymbolBehaviorTypes = scadaSymbolBehaviorTypes;
  scadaSymbolBehaviorTypeTranslations = scadaSymbolBehaviorTypeTranslations;

  valueTypes = Object.keys(ValueType) as ValueType[];

  valueTypesMap = valueTypesMap;

  @Input()
  isAdd = false;

  @Input()
  behavior: ScadaSymbolBehavior;

  @Input()
  aliasController: IAliasController;

  @Input()
  callbacks: WidgetActionCallbacks;

  @Input()
  disabled: boolean;

  @Input()
  popover: TbPopoverComponent<ScadaSymbolBehaviorPanelComponent>;

  @Output()
  behaviorSettingsApplied = new EventEmitter<ScadaSymbolBehavior>();

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  panelTitle: string;

  behaviorFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.panelTitle = this.isAdd ? 'scada.behavior.add-behavior' : 'scada.behavior.behavior-settings';
    this.behaviorFormGroup = this.fb.group(
      {
        id: [this.behavior.id, [Validators.required]],
        name: [this.behavior.name, [Validators.required]],
        hint: [this.behavior.hint, []],
        group: [this.behavior.group, []],
        type: [this.behavior.type, [Validators.required]],
        valueType: [this.behavior.valueType, [Validators.required]],
        trueLabel: [this.behavior.trueLabel, []],
        falseLabel: [this.behavior.falseLabel, []],
        stateLabel: [this.behavior.stateLabel, []],
        defaultGetValueSettings: [this.behavior.defaultGetValueSettings, [Validators.required]],
        defaultSetValueSettings: [this.behavior.defaultSetValueSettings, [Validators.required]],
        defaultWidgetActionSettings: [this.behavior.defaultWidgetActionSettings, [Validators.required]]
      }
    );
    if (this.disabled) {
      this.behaviorFormGroup.disable({emitEvent: false});
    } else {
      merge(this.behaviorFormGroup.get('type').valueChanges,
        this.behaviorFormGroup.get('valueType').valueChanges
      ).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateValidators();
      });
      this.updateValidators();
    }
  }

  cancel() {
    this.popover?.hide();
  }

  applyBehaviorSettings() {
    const behavior = this.behaviorFormGroup.getRawValue();
    this.behaviorSettingsApplied.emit(behavior);
  }

  private updateValidators() {
    const type: ScadaSymbolBehaviorType = this.behaviorFormGroup.get('type').value;
    const valueType: ValueType = this.behaviorFormGroup.get('valueType').value;
    let defaultGetValueSettingsValue = this.behaviorFormGroup.get('defaultGetValueSettings').value;
    let defaultSetValueSettingsValue = this.behaviorFormGroup.get('defaultSetValueSettings').value;
    let defaultWidgetActionSettingsValue = this.behaviorFormGroup.get('defaultWidgetActionSettings').value;
    this.behaviorFormGroup.disable({emitEvent: false});
    this.behaviorFormGroup.get('id').enable({emitEvent: false});
    this.behaviorFormGroup.get('name').enable({emitEvent: false});
    this.behaviorFormGroup.get('type').enable({emitEvent: false});
    this.behaviorFormGroup.get('hint').enable({emitEvent: false});
    this.behaviorFormGroup.get('group').enable({emitEvent: false});
    switch (type) {
      case ScadaSymbolBehaviorType.value:
        this.behaviorFormGroup.get('valueType').enable({emitEvent: false});
        this.behaviorFormGroup.get('defaultGetValueSettings').enable({emitEvent: false});
        if (valueType === ValueType.BOOLEAN) {
          this.behaviorFormGroup.get('trueLabel').enable({emitEvent: false});
          this.behaviorFormGroup.get('falseLabel').enable({emitEvent: false});
          this.behaviorFormGroup.get('stateLabel').enable({emitEvent: false});
        }
        if (!defaultGetValueSettingsValue) {
          defaultGetValueSettingsValue = mergeDeep({} as GetValueSettings<any>, defaultGetValueSettings(valueType));
          this.behaviorFormGroup.get('defaultGetValueSettings').patchValue(defaultGetValueSettingsValue, {emitEvent: true});
        }
        if (defaultSetValueSettingsValue) {
          this.behaviorFormGroup.get('defaultSetValueSettings').patchValue(null, {emitEvent: true});
        }
        if (defaultWidgetActionSettingsValue) {
          this.behaviorFormGroup.get('defaultWidgetActionSettings').patchValue(null, {emitEvent: true});
        }
        break;
      case ScadaSymbolBehaviorType.action:
        this.behaviorFormGroup.get('valueType').enable({emitEvent: false});
        this.behaviorFormGroup.get('defaultSetValueSettings').enable({emitEvent: false});
        if (!defaultSetValueSettingsValue) {
          defaultSetValueSettingsValue = mergeDeep({} as SetValueSettings, defaultSetValueSettings(valueType));
          this.behaviorFormGroup.get('defaultSetValueSettings').patchValue(defaultSetValueSettingsValue, {emitEvent: true});
        }
        if (defaultGetValueSettingsValue) {
          this.behaviorFormGroup.get('defaultGetValueSettings').patchValue(null, {emitEvent: true});
        }
        if (defaultWidgetActionSettingsValue) {
          this.behaviorFormGroup.get('defaultWidgetActionSettings').patchValue(null, {emitEvent: true});
        }
        break;
      case ScadaSymbolBehaviorType.widgetAction:
        this.behaviorFormGroup.get('defaultWidgetActionSettings').enable({emitEvent: false});
        if (!defaultWidgetActionSettingsValue) {
          defaultWidgetActionSettingsValue = mergeDeep({} as WidgetAction, defaultWidgetActionSettings);
          this.behaviorFormGroup.get('defaultWidgetActionSettings').patchValue(defaultWidgetActionSettingsValue, {emitEvent: true});
        }
        if (defaultGetValueSettingsValue) {
          this.behaviorFormGroup.get('defaultGetValueSettings').patchValue(null, {emitEvent: true});
        }
        if (defaultSetValueSettingsValue) {
          this.behaviorFormGroup.get('defaultSetValueSettings').patchValue(null, {emitEvent: true});
        }
        break;
    }
  }
}
