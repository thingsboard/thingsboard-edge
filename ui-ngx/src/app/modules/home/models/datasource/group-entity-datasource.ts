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

import { EntityBooleanFunction } from '@home/models/entity/entities-table-config.models';
import { EntityGroupColumn, ShortEntityView } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntitiesDataSource } from '@home/models/datasource/entity-datasource';
import { deepClone } from '@core/utils';
import { PageLink } from '@shared/models/page/page-link';

export class GroupEntitiesDataSource extends EntitiesDataSource<ShortEntityView> {

  constructor(private columnsMap: Map<string, EntityGroupColumn>,
              private entityGroupId: string,
              private entityGroupService: EntityGroupService,
              protected selectionEnabledFunction: EntityBooleanFunction<ShortEntityView>,
              protected dataLoadedFunction: () => void) {
    super(
      (pageLink =>
        {
          if (pageLink.sortOrder && pageLink.sortOrder.property) {
            const column = this.columnsMap.get(pageLink.sortOrder.property);
            let sortOrder = null;
            if (column) {
              const newProperty = this.columnsMap.get(pageLink.sortOrder.property).property;
              sortOrder = deepClone(pageLink.sortOrder);
              sortOrder.property = newProperty;
            }
            pageLink = new PageLink(pageLink.pageSize, pageLink.page, pageLink.textSearch, sortOrder);
          }
          return this.entityGroupService.getEntityGroupEntities<ShortEntityView>(this.entityGroupId, pageLink)
        }),
      selectionEnabledFunction,
      dataLoadedFunction
    )
  }

}
