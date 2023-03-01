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

import { Component, Inject, InjectionToken } from '@angular/core';
import { DisplayColumn } from '@home/components/widget/lib/table-widget.models';

export const DISPLAY_COLUMNS_PANEL_DATA = new InjectionToken<any>('DisplayColumnsPanelData');

export interface DisplayColumnsPanelData {
  columns: DisplayColumn[];
  columnsUpdated: (columns: DisplayColumn[]) => void;
}

@Component({
  selector: 'tb-display-columns-panel',
  templateUrl: './display-columns-panel.component.html',
  styleUrls: ['./display-columns-panel.component.scss']
})
export class DisplayColumnsPanelComponent {

  columns: DisplayColumn[];

  constructor(@Inject(DISPLAY_COLUMNS_PANEL_DATA) public data: DisplayColumnsPanelData) {
    this.columns = this.data.columns;
  }

  public update() {
    this.data.columnsUpdated(this.columns);
  }
}
