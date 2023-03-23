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
  selector: 'tb-markdown-widget-settings',
  templateUrl: './markdown-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class MarkdownWidgetSettingsComponent extends WidgetSettingsComponent {

  markdownWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.markdownWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      useMarkdownTextFunction: false,
      markdownTextPattern: '# Markdown/HTML card \\n - **Current entity**: **${entityName}**. \\n - **Current value**: **${Random}**.',
      markdownTextFunction: 'return \'# Some title\\\\n - Entity name: \' + data[0][\'entityName\'];',
      applyDefaultMarkdownStyle: true,
      markdownCss: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.markdownWidgetSettingsForm = this.fb.group({
      useMarkdownTextFunction: [settings.useMarkdownTextFunction, []],
      markdownTextPattern: [settings.markdownTextPattern, []],
      markdownTextFunction: [settings.markdownTextFunction, []],
      applyDefaultMarkdownStyle: [settings.applyDefaultMarkdownStyle, []],
      markdownCss: [settings.markdownCss, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useMarkdownTextFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useMarkdownTextFunction: boolean = this.markdownWidgetSettingsForm.get('useMarkdownTextFunction').value;
    if (useMarkdownTextFunction) {
      this.markdownWidgetSettingsForm.get('markdownTextPattern').disable();
      this.markdownWidgetSettingsForm.get('markdownTextFunction').enable();
    } else {
      this.markdownWidgetSettingsForm.get('markdownTextPattern').enable();
      this.markdownWidgetSettingsForm.get('markdownTextFunction').disable();
    }
    this.markdownWidgetSettingsForm.get('markdownTextPattern').updateValueAndValidity({emitEvent});
    this.markdownWidgetSettingsForm.get('markdownTextFunction').updateValueAndValidity({emitEvent});
  }

}
