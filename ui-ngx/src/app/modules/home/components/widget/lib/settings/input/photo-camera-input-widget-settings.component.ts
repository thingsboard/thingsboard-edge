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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-photo-camera-input-widget-settings',
  templateUrl: './photo-camera-input-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class PhotoCameraInputWidgetSettingsComponent extends WidgetSettingsComponent {

  photoCameraInputWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.photoCameraInputWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',

      imageFormat: 'image/png',
      imageQuality: 0.92,
      maxWidth: 640,
      maxHeight: 480
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.photoCameraInputWidgetSettingsForm = this.fb.group({

      // General settings

      widgetTitle: [settings.widgetTitle, []],

      // Image settings

      imageFormat: [settings.imageFormat, []],
      imageQuality: [settings.imageQuality, [Validators.min(0), Validators.max(1)]],
      maxWidth: [settings.maxWidth, [Validators.min(1)]],
      maxHeight: [settings.maxHeight, [Validators.min(1)]]
    });
  }
}
