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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { LegendConfig, LegendData, LegendDirection, LegendKey, LegendPosition } from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';

@Component({
  selector: 'tb-legend',
  templateUrl: './legend.component.html',
  styleUrls: ['./legend.component.scss']
})
export class LegendComponent implements OnInit {

  @Input()
  legendConfig: LegendConfig;

  @Input()
  legendData: LegendData;

  @Output()
  legendKeyHiddenChange = new EventEmitter<number>();

  displayHeader: boolean;

  isHorizontal: boolean;

  isRowDirection: boolean;

  constructor(private utils: UtilsService) {
  }

  ngOnInit(): void {
    this.displayHeader = this.legendConfig.showMin === true ||
      this.legendConfig.showMax === true ||
      this.legendConfig.showAvg === true ||
      this.legendConfig.showTotal === true ||
      this.legendConfig.showLatest === true;

    this.isHorizontal = this.legendConfig.position === LegendPosition.bottom ||
      this.legendConfig.position === LegendPosition.top;

    this.isRowDirection = this.legendConfig.direction === LegendDirection.row;
  }

  toggleHideData(index: number) {
    const dataKey = this.legendData.keys.find(key => key.dataIndex === index).dataKey;
    if (!dataKey.settings.disableDataHiding) {
      dataKey.hidden = !dataKey.hidden;
      this.legendKeyHiddenChange.emit(index);
    }
  }

  legendKeys(): LegendKey[] {
    try {
      let keys = this.legendData.keys;
      if (this.legendConfig.sortDataKeys) {
        keys = this.legendData.keys.sort((key1, key2) => key1.dataKey.label.localeCompare(key2.dataKey.label));
      }
      return keys.filter(legendKey => this.legendData.keys[legendKey.dataIndex].dataKey.inLegend);
    } catch (e) {}
  }

  getDataKeyLabel(label: string): string {
    return this.utils.customTranslation(label, label);
  }

}
