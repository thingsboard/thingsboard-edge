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
import { IAliasController } from '@core/api/widget-api.models';
import { Filter, FilterInfo } from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import { deepClone } from '@core/utils';
import { UserFilterDialogComponent, UserFilterDialogData } from '@home/components/filter/user-filter-dialog.component';

export const FILTER_EDIT_PANEL_DATA = new InjectionToken<any>('FiltersEditPanelData');

export interface FiltersEditPanelData {
  aliasController: IAliasController;
  filtersInfo: {[filterId: string]: FilterInfo};
}

@Component({
  selector: 'tb-filters-edit-panel',
  templateUrl: './filters-edit-panel.component.html',
  styleUrls: ['./filters-edit-panel.component.scss']
})
export class FiltersEditPanelComponent {

  filtersInfo: {[filterId: string]: FilterInfo};

  constructor(@Inject(FILTER_EDIT_PANEL_DATA) public data: FiltersEditPanelData,
              private dialog: MatDialog) {
    this.filtersInfo = this.data.filtersInfo;
  }

  public editFilter(filterId: string, filter: FilterInfo) {
    const singleFilter: Filter = {id: filterId, ...deepClone(filter)};
    this.dialog.open<UserFilterDialogComponent, UserFilterDialogData,
      Filter>(UserFilterDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        filter: singleFilter
      }
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.filtersInfo[result.id] = result;
          this.data.aliasController.updateUserFilter(result);
        }
      });
  }
}
