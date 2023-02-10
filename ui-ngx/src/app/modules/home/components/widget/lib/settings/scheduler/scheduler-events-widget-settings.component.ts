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
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CustomSchedulerEventType } from '@home/components/scheduler/scheduler-events.models';
import {
  customSchedulerEventTypeValidator
} from '@home/components/widget/lib/settings/scheduler/custom-scheduler-event-type.component';

@Component({
  selector: 'tb-scheduler-events-widget-settings',
  templateUrl: './scheduler-events-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class SchedulerEventsWidgetSettingsComponent extends WidgetSettingsComponent {

  schedulerEventsWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.schedulerEventsWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      displayCreatedTime: true,
      displayType: true,
      displayCustomer: true,
      displayPagination: true,
      defaultPageSize: 10,
      defaultSortOrder: 'name',
      enabledViews: 'both',
      noDataDisplayMessage: '',
      forceDefaultEventType: '',
      customEventTypes: []
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.schedulerEventsWidgetSettingsForm = this.fb.group({
      title: [settings.title, []],
      displayCreatedTime: [settings.displayCreatedTime, []],
      displayType: [settings.displayType, []],
      displayCustomer: [settings.displayCustomer, []],
      displayPagination: [settings.displayPagination, []],
      defaultPageSize: [settings.defaultPageSize, [Validators.min(1)]],
      defaultSortOrder: [settings.defaultSortOrder, []],
      enabledViews: [settings.enabledViews, []],
      noDataDisplayMessage: [settings.noDataDisplayMessage, []],
      forceDefaultEventType: [settings.forceDefaultEventType, []],
      customEventTypes: this.prepareCustomEventTypesFormArray(settings.customEventTypes)
    });
  }

  protected doUpdateSettings(settingsForm: UntypedFormGroup, settings: WidgetSettings) {
    settingsForm.setControl('customEventTypes', this.prepareCustomEventTypesFormArray(settings.customEventTypes), {emitEvent: false});
  }

  private prepareCustomEventTypesFormArray(customEventTypes: CustomSchedulerEventType[] | undefined): UntypedFormArray {
    const customEventTypesControls: Array<AbstractControl> = [];
    if (customEventTypes) {
      customEventTypes.forEach((customEventType) => {
        customEventTypesControls.push(this.fb.control(customEventType, [customSchedulerEventTypeValidator]));
      });
    }
    return this.fb.array(customEventTypesControls, []);
  }

  customEventTypesFormArray(): UntypedFormArray {
    return this.schedulerEventsWidgetSettingsForm.get('customEventTypes') as UntypedFormArray;
  }

  public trackByCustomEventType(index: number, customEventTypeControl: AbstractControl): any {
    return customEventTypeControl;
  }

  public removeCustomEventType(index: number) {
    (this.schedulerEventsWidgetSettingsForm.get('customEventTypes') as UntypedFormArray).removeAt(index);
  }

  public addCustomEventType() {
    const customEventType: CustomSchedulerEventType = {
      name: null,
      value: null,
      originator: null,
      msgType: null,
      metadata: null,
      template: null
    };
    const customEventTypesArray = this.schedulerEventsWidgetSettingsForm.get('customEventTypes') as UntypedFormArray;
    const customEventTypeControl = this.fb.control(customEventType, [customSchedulerEventTypeValidator]);
    (customEventTypeControl as any).new = true;
    customEventTypesArray.push(customEventTypeControl);
    this.schedulerEventsWidgetSettingsForm.updateValueAndValidity();
    if (!this.schedulerEventsWidgetSettingsForm.valid) {
      this.onSettingsChanged(this.schedulerEventsWidgetSettingsForm.value);
    }
  }

  protected validatorTriggers(): string[] {
    return ['displayPagination'];
  }

  protected updateValidators(emitEvent: boolean) {
    const displayPagination: boolean = this.schedulerEventsWidgetSettingsForm.get('displayPagination').value;
    if (displayPagination) {
      this.schedulerEventsWidgetSettingsForm.get('defaultPageSize').enable();
    } else {
      this.schedulerEventsWidgetSettingsForm.get('defaultPageSize').disable();
    }
    this.schedulerEventsWidgetSettingsForm.get('defaultPageSize').updateValueAndValidity({emitEvent});
  }
}
