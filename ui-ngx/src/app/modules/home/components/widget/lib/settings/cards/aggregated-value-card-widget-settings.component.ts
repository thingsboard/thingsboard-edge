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

import { Component, Injector } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DateFormatProcessor, DateFormatSettings } from '@shared/models/widget-settings.models';
import { aggregatedValueCardDefaultSettings } from '@home/components/widget/lib/cards/aggregated-value-card.models';

@Component({
  selector: 'tb-aggregated-value-card-widget-settings',
  templateUrl: './aggregated-value-card-widget-settings.component.html',
  styleUrls: []
})
export class AggregatedValueCardWidgetSettingsComponent extends WidgetSettingsComponent {

  aggregatedValueCardWidgetSettingsForm: UntypedFormGroup;

  datePreviewFn = this._datePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.aggregatedValueCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {...aggregatedValueCardDefaultSettings};
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.aggregatedValueCardWidgetSettingsForm = this.fb.group({

      showSubtitle: [settings.showSubtitle, []],
      subtitle: [settings.subtitle, []],
      subtitleFont: [settings.subtitleFont, []],
      subtitleColor: [settings.subtitleColor, []],

      showDate: [settings.showDate, []],
      dateFormat: [settings.dateFormat, []],
      dateFont: [settings.dateFont, []],
      dateColor: [settings.dateColor, []],

      showChart: [settings.showChart, []],

      background: [settings.background, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showSubtitle', 'showDate'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showSubtitle: boolean = this.aggregatedValueCardWidgetSettingsForm.get('showSubtitle').value;
    const showDate: boolean = this.aggregatedValueCardWidgetSettingsForm.get('showDate').value;

    if (showSubtitle) {
      this.aggregatedValueCardWidgetSettingsForm.get('subtitle').enable();
      this.aggregatedValueCardWidgetSettingsForm.get('subtitleFont').enable();
      this.aggregatedValueCardWidgetSettingsForm.get('subtitleColor').enable();
    } else {
      this.aggregatedValueCardWidgetSettingsForm.get('subtitle').disable();
      this.aggregatedValueCardWidgetSettingsForm.get('subtitleFont').disable();
      this.aggregatedValueCardWidgetSettingsForm.get('subtitleColor').disable();
    }

    if (showDate) {
      this.aggregatedValueCardWidgetSettingsForm.get('dateFormat').enable();
      this.aggregatedValueCardWidgetSettingsForm.get('dateFont').enable();
      this.aggregatedValueCardWidgetSettingsForm.get('dateColor').enable();
    } else {
      this.aggregatedValueCardWidgetSettingsForm.get('dateFormat').disable();
      this.aggregatedValueCardWidgetSettingsForm.get('dateFont').disable();
      this.aggregatedValueCardWidgetSettingsForm.get('dateColor').disable();
    }

    this.aggregatedValueCardWidgetSettingsForm.get('subtitle').updateValueAndValidity({emitEvent});
    this.aggregatedValueCardWidgetSettingsForm.get('subtitleFont').updateValueAndValidity({emitEvent});
    this.aggregatedValueCardWidgetSettingsForm.get('subtitleColor').updateValueAndValidity({emitEvent});
    this.aggregatedValueCardWidgetSettingsForm.get('dateFormat').updateValueAndValidity({emitEvent});
    this.aggregatedValueCardWidgetSettingsForm.get('dateFont').updateValueAndValidity({emitEvent});
    this.aggregatedValueCardWidgetSettingsForm.get('dateColor').updateValueAndValidity({emitEvent});
  }

  private _datePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.aggregatedValueCardWidgetSettingsForm.get('dateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }
}
