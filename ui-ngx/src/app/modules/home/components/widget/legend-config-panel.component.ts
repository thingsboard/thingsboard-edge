///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, Inject, InjectionToken, OnInit, ViewContainerRef } from '@angular/core';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup } from '@angular/forms';
import {
  LegendConfig,
  LegendDirection,
  legendDirectionTranslationMap,
  LegendPosition,
  legendPositionTranslationMap
} from '@shared/models/widget.models';

export const LEGEND_CONFIG_PANEL_DATA = new InjectionToken<any>('LegendConfigPanelData');

export interface LegendConfigPanelData {
  legendConfig: LegendConfig;
  legendConfigUpdated: (legendConfig: LegendConfig) => void;
}

@Component({
  selector: 'tb-legend-config-panel',
  templateUrl: './legend-config-panel.component.html',
  styleUrls: ['./legend-config-panel.component.scss']
})
export class LegendConfigPanelComponent extends PageComponent implements OnInit {

  legendConfigForm: FormGroup;

  legendDirection = LegendDirection;

  legendDirections = Object.keys(LegendDirection);

  legendDirectionTranslations = legendDirectionTranslationMap;

  legendPosition = LegendPosition;

  legendPositions = Object.keys(LegendPosition);

  legendPositionTranslations = legendPositionTranslationMap;

  constructor(@Inject(LEGEND_CONFIG_PANEL_DATA) public data: LegendConfigPanelData,
              public overlayRef: OverlayRef,
              protected store: Store<AppState>,
              public fb: FormBuilder,
              private overlay: Overlay,
              public viewContainerRef: ViewContainerRef) {
    super(store);
  }

  ngOnInit(): void {
    this.legendConfigForm = this.fb.group({
      direction: [this.data.legendConfig.direction, []],
      position: [this.data.legendConfig.position, []],
      showMin: [this.data.legendConfig.showMin, []],
      showMax: [this.data.legendConfig.showMax, []],
      showAvg: [this.data.legendConfig.showAvg, []],
      showTotal: [this.data.legendConfig.showTotal, []]
    });
    this.legendConfigForm.get('direction').valueChanges.subscribe((direction: LegendDirection) => {
      this.onDirectionChanged(direction);
    });
    this.onDirectionChanged(this.data.legendConfig.direction);
    this.legendConfigForm.valueChanges.subscribe(() => {
      this.update();
    });
  }

  private onDirectionChanged(direction: LegendDirection) {
    if (direction === LegendDirection.row) {
      let position: LegendPosition = this.legendConfigForm.get('position').value;
      if (position !== LegendPosition.bottom && position !== LegendPosition.top) {
        position = LegendPosition.bottom;
      }
      this.legendConfigForm.patchValue(
        {
          position
        }, {emitEvent: false}
      );
    }
  }

  update() {
    const newLegendConfig: LegendConfig = this.legendConfigForm.value;
    this.data.legendConfigUpdated(newLegendConfig);
  }

}
