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
  selector: 'tb-entities-table-widget-settings',
  templateUrl: './entities-table-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class EntitiesTableWidgetSettingsComponent extends WidgetSettingsComponent {

  entitiesTableWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.entitiesTableWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      entitiesTitle: '',
      enableSearch: true,
      enableSelectColumnDisplay: true,
      enableStickyHeader: true,
      enableStickyAction: true,
      reserveSpaceForHiddenAction: 'true',
      displayEntityName: true,
      entityNameColumnTitle: '',
      displayEntityLabel: false,
      entityLabelColumnTitle: '',
      displayEntityType: true,
      displayPagination: true,
      defaultPageSize: 10,
      defaultSortOrder: 'entityName',
      useRowStyleFunction: false,
      rowStyleFunction: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.entitiesTableWidgetSettingsForm = this.fb.group({
      entitiesTitle: [settings.entitiesTitle, []],
      enableSearch: [settings.enableSearch, []],
      enableSelectColumnDisplay: [settings.enableSelectColumnDisplay, []],
      enableStickyHeader: [settings.enableStickyHeader, []],
      enableStickyAction: [settings.enableStickyAction, []],
      reserveSpaceForHiddenAction: [settings.reserveSpaceForHiddenAction, []],
      displayEntityName: [settings.displayEntityName, []],
      entityNameColumnTitle: [settings.entityNameColumnTitle, []],
      displayEntityLabel: [settings.displayEntityLabel, []],
      entityLabelColumnTitle: [settings.entityLabelColumnTitle, []],
      displayEntityType: [settings.displayEntityType, []],
      displayPagination: [settings.displayPagination, []],
      defaultPageSize: [settings.defaultPageSize, [Validators.min(1)]],
      defaultSortOrder: [settings.defaultSortOrder, []],
      useRowStyleFunction: [settings.useRowStyleFunction, []],
      rowStyleFunction: [settings.rowStyleFunction, [Validators.required]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useRowStyleFunction', 'displayPagination', 'displayEntityName', 'displayEntityLabel'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useRowStyleFunction: boolean = this.entitiesTableWidgetSettingsForm.get('useRowStyleFunction').value;
    const displayPagination: boolean = this.entitiesTableWidgetSettingsForm.get('displayPagination').value;
    const displayEntityName: boolean = this.entitiesTableWidgetSettingsForm.get('displayEntityName').value;
    const displayEntityLabel: boolean = this.entitiesTableWidgetSettingsForm.get('displayEntityLabel').value;
    if (useRowStyleFunction) {
      this.entitiesTableWidgetSettingsForm.get('rowStyleFunction').enable();
    } else {
      this.entitiesTableWidgetSettingsForm.get('rowStyleFunction').disable();
    }
    if (displayPagination) {
      this.entitiesTableWidgetSettingsForm.get('defaultPageSize').enable();
    } else {
      this.entitiesTableWidgetSettingsForm.get('defaultPageSize').disable();
    }
    if (displayEntityName) {
      this.entitiesTableWidgetSettingsForm.get('entityNameColumnTitle').enable();
    } else {
      this.entitiesTableWidgetSettingsForm.get('entityNameColumnTitle').disable();
    }
    if (displayEntityLabel) {
      this.entitiesTableWidgetSettingsForm.get('entityLabelColumnTitle').enable();
    } else {
      this.entitiesTableWidgetSettingsForm.get('entityLabelColumnTitle').disable();
    }
    this.entitiesTableWidgetSettingsForm.get('rowStyleFunction').updateValueAndValidity({emitEvent});
    this.entitiesTableWidgetSettingsForm.get('defaultPageSize').updateValueAndValidity({emitEvent});
    this.entitiesTableWidgetSettingsForm.get('entityNameColumnTitle').updateValueAndValidity({emitEvent});
    this.entitiesTableWidgetSettingsForm.get('entityLabelColumnTitle').updateValueAndValidity({emitEvent});
  }

}
