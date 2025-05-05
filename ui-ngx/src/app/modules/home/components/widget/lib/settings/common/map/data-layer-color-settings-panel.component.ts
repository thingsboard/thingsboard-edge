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
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DataLayerColorSettings, DataLayerColorType, MapType } from '@shared/models/widget/maps/map.models';
import { DataKey, DatasourceType, widgetType } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
  selector: 'tb-data-layer-color-settings-panel',
  templateUrl: './data-layer-color-settings-panel.component.html',
  providers: [],
  styleUrls: ['./data-layer-color-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DataLayerColorSettingsPanelComponent extends PageComponent implements OnInit {

  widgetType = widgetType;

  DataKeyType = DataKeyType;

  @Input()
  colorSettings: DataLayerColorSettings;

  @Input()
  context: MapSettingsContext;

  @Input()
  dsType: DatasourceType;

  @Input()
  dsEntityAliasId: string;

  @Input()
  dsDeviceId: string;

  @Input()
  helpId = 'widget/lib/map/color_fn';

  @Input()
  popover: TbPopoverComponent<DataLayerColorSettingsPanelComponent>;

  @Output()
  colorSettingsApplied = new EventEmitter<DataLayerColorSettings>();

  DataLayerColorType = DataLayerColorType;

  colorSettingsFormGroup: UntypedFormGroup;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              protected store: Store<AppState>,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.colorSettingsFormGroup = this.fb.group(
      {
        type: [this.colorSettings?.type || DataLayerColorType.constant, []],
        color: [this.colorSettings?.color, []],
        rangeKey: [this.colorSettings?.rangeKey, [Validators.required]],
        range: [this.colorSettings?.range, []],
        colorFunction: [this.colorSettings?.colorFunction, []]
      }
    );
    this.colorSettingsFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applyColorSettings() {
    const colorSettings: DataLayerColorSettings = this.colorSettingsFormGroup.getRawValue();
    this.colorSettingsApplied.emit(colorSettings);
  }

  public editRangeKey() {
    const targetDataKey: DataKey = this.colorSettingsFormGroup.get('rangeKey').value;
    this.context.editKey(targetDataKey,
      this.dsDeviceId, this.dsEntityAliasId, widgetType.latest).subscribe(
      (updatedDataKey) => {
        if (updatedDataKey) {
          this.colorSettingsFormGroup.get('rangeKey').patchValue(updatedDataKey);
          this.colorSettingsFormGroup.markAsDirty();
        }
      }
    );
  }

  private updateValidators() {
    const type: DataLayerColorType = this.colorSettingsFormGroup.get('type').value;
    if (type === DataLayerColorType.range) {
      this.colorSettingsFormGroup.get('rangeKey').enable({emitEvent: false});
      this.colorSettingsFormGroup.get('range').enable({emitEvent: false});
    } else {
      this.colorSettingsFormGroup.get('rangeKey').disable({emitEvent: false});
      this.colorSettingsFormGroup.get('range').disable({emitEvent: false});
    }
    if (type === DataLayerColorType.function) {
      this.colorSettingsFormGroup.get('colorFunction').enable({emitEvent: false});
    } else {
      this.colorSettingsFormGroup.get('colorFunction').disable({emitEvent: false});
    }
  }
}
