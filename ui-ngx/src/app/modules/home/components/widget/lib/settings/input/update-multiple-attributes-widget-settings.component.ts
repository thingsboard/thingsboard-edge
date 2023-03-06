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

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-update-multiple-attributes-widget-settings',
  templateUrl: './update-multiple-attributes-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UpdateMultipleAttributesWidgetSettingsComponent extends WidgetSettingsComponent {

  updateMultipleAttributesWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.updateMultipleAttributesWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      showResultMessage: true,
      showActionButtons: true,
      updateAllValues: false,
      saveButtonLabel: '',
      resetButtonLabel: '',
      showGroupTitle: false,
      groupTitle: '',
      fieldsAlignment: 'row',
      fieldsInRow: 2
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateMultipleAttributesWidgetSettingsForm = this.fb.group({

      // General settings

      widgetTitle: [settings.widgetTitle, []],
      showResultMessage: [settings.showResultMessage, []],

      // Action button settings

      showActionButtons: [settings.showActionButtons, []],
      updateAllValues: [settings.updateAllValues, []],
      saveButtonLabel: [settings.saveButtonLabel, []],
      resetButtonLabel: [settings.resetButtonLabel, []],

      // Group settings

      showGroupTitle: [settings.showGroupTitle, []],
      groupTitle: [settings.groupTitle, []],

      // Fields alignment

      fieldsAlignment: [settings.fieldsAlignment, []],
      fieldsInRow: [settings.fieldsInRow, [Validators.min(1)]],
    });
  }

  protected validatorTriggers(): string[] {
    return ['showActionButtons', 'showGroupTitle', 'fieldsAlignment'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showActionButtons: boolean = this.updateMultipleAttributesWidgetSettingsForm.get('showActionButtons').value;
    const showGroupTitle: boolean = this.updateMultipleAttributesWidgetSettingsForm.get('showGroupTitle').value;
    const fieldsAlignment: string = this.updateMultipleAttributesWidgetSettingsForm.get('fieldsAlignment').value;

    if (showActionButtons) {
      this.updateMultipleAttributesWidgetSettingsForm.get('updateAllValues').enable();
      this.updateMultipleAttributesWidgetSettingsForm.get('saveButtonLabel').enable();
      this.updateMultipleAttributesWidgetSettingsForm.get('resetButtonLabel').enable();
    } else {
      this.updateMultipleAttributesWidgetSettingsForm.get('updateAllValues').disable();
      this.updateMultipleAttributesWidgetSettingsForm.get('saveButtonLabel').disable();
      this.updateMultipleAttributesWidgetSettingsForm.get('resetButtonLabel').disable();
    }
    if (showGroupTitle) {
      this.updateMultipleAttributesWidgetSettingsForm.get('groupTitle').enable();
    } else {
      this.updateMultipleAttributesWidgetSettingsForm.get('groupTitle').disable();
    }
    if (fieldsAlignment === 'row') {
      this.updateMultipleAttributesWidgetSettingsForm.get('fieldsInRow').enable();
    } else {
      this.updateMultipleAttributesWidgetSettingsForm.get('fieldsInRow').disable();
    }
    this.updateMultipleAttributesWidgetSettingsForm.get('updateAllValues').updateValueAndValidity({emitEvent});
    this.updateMultipleAttributesWidgetSettingsForm.get('saveButtonLabel').updateValueAndValidity({emitEvent});
    this.updateMultipleAttributesWidgetSettingsForm.get('resetButtonLabel').updateValueAndValidity({emitEvent});
    this.updateMultipleAttributesWidgetSettingsForm.get('groupTitle').updateValueAndValidity({emitEvent});
    this.updateMultipleAttributesWidgetSettingsForm.get('fieldsInRow').updateValueAndValidity({emitEvent});
  }
}
