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

import { Component, Inject, InjectionToken } from '@angular/core';
import { AliasInfo, IAliasController } from '@core/api/widget-api.models';
import { deepClone } from '@core/utils';

export const ALIASES_ENTITY_SELECT_PANEL_DATA = new InjectionToken<any>('AliasesEntitySelectPanelData');

export interface AliasesEntitySelectPanelData {
  aliasController: IAliasController;
}

@Component({
  selector: 'tb-aliases-entity-select-panel',
  templateUrl: './aliases-entity-select-panel.component.html',
  styleUrls: ['./aliases-entity-select-panel.component.scss']
})
export class AliasesEntitySelectPanelComponent {

  entityAliasesInfo: {[aliasId: string]: AliasInfo} = {};

  constructor(@Inject(ALIASES_ENTITY_SELECT_PANEL_DATA) public data: AliasesEntitySelectPanelData) {
    const allEntityAliases = this.data.aliasController.getEntityAliases();
    for (const aliasId of Object.keys(allEntityAliases)) {
      const aliasInfo = this.data.aliasController.getInstantAliasInfo(aliasId);
      if (aliasInfo && !aliasInfo.resolveMultiple && aliasInfo.currentEntity
        && aliasInfo.resolvedEntities.length > 1) {
        this.entityAliasesInfo[aliasId] = deepClone(aliasInfo);
        this.entityAliasesInfo[aliasId].selectedId = aliasInfo.currentEntity.id;
      }
    }
  }

  public currentAliasEntityChanged(aliasId: string, selectedId: string) {
    const resolvedEntities = this.entityAliasesInfo[aliasId].resolvedEntities;
    const selected = resolvedEntities.find((entity) => entity.id === selectedId);
    if (selected) {
      this.data.aliasController.updateCurrentAliasEntity(aliasId, selected);
    }
  }


}
