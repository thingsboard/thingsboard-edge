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
  selector: 'tb-blob-entities-widget-settings',
  templateUrl: './blob-entities-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class BlobEntitiesWidgetSettingsComponent extends WidgetSettingsComponent {

  blobEntitiesWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.blobEntitiesWidgetSettingsForm;
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
      noDataDisplayMessage: '',
      forceDefaultType: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.blobEntitiesWidgetSettingsForm = this.fb.group({
      title: [settings.title, []],
      displayCreatedTime: [settings.displayCreatedTime, []],
      displayType: [settings.displayType, []],
      displayCustomer: [settings.displayCustomer, []],
      displayPagination: [settings.displayPagination, []],
      defaultPageSize: [settings.defaultPageSize, [Validators.min(1)]],
      defaultSortOrder: [settings.defaultSortOrder, []],
      noDataDisplayMessage: [settings.noDataDisplayMessage, []],
      forceDefaultType: [settings.forceDefaultType, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['displayPagination'];
  }

  protected updateValidators(emitEvent: boolean) {
    const displayPagination: boolean = this.blobEntitiesWidgetSettingsForm.get('displayPagination').value;
    if (displayPagination) {
      this.blobEntitiesWidgetSettingsForm.get('defaultPageSize').enable();
    } else {
      this.blobEntitiesWidgetSettingsForm.get('defaultPageSize').disable();
    }
    this.blobEntitiesWidgetSettingsForm.get('defaultPageSize').updateValueAndValidity({emitEvent});
  }
}
