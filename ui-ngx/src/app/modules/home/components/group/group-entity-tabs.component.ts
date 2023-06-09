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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { BaseData, HasId } from '@shared/models/base-data';
import { PageLink } from '@shared/models/page/page-link';
import { EntityGroupInfo, ShortEntityView } from '@shared/models/entity-group.models';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Operation, Resource, resourceByEntityType } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { exportableEntityTypes } from '@shared/models/vc.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-group-entity-tabs',
  templateUrl: './group-entity-tabs.component.html',
  styleUrls: []
})
export class GroupEntityTabsComponent<T extends BaseData<HasId>>
  extends EntityTabsComponent<T, PageLink, T | ShortEntityView, EntityTableConfig<T> | GroupEntityTableConfig<T>> {

  entityGroup: EntityGroupInfo;
  entityType: EntityType;
  entityResource: Resource;

  constructor(private userPermissionsService: UserPermissionsService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  hasVersionControl(): boolean {
    if (this.authUser.authority === this.authorities.TENANT_ADMIN && this.entityType &&
      exportableEntityTypes.includes(this.entityType) && EntityType.USER !== this.entityType) {
      return this.userPermissionsService.hasResourcesGenericPermission([Resource.VERSION_CONTROL, this.entityResource], Operation.READ);
    } else {
      return false;
    }
  }

  protected setEntitiesTableConfig(entitiesTableConfig: EntityTableConfig<T> | GroupEntityTableConfig<T>) {
    super.setEntitiesTableConfig(entitiesTableConfig);
    if (entitiesTableConfig) {
      this.entityGroup = (entitiesTableConfig as GroupEntityTableConfig<T>).entityGroup;
      if (this.entityGroup) {
        this.entityType = this.entityGroup.type;
      } else {
        this.entityType = entitiesTableConfig.entityType;
      }
      this.entityResource = resourceByEntityType.get(this.entityType);
    }
  }

  protected isGroupMode(): boolean {
    return this.entitiesTableConfig && this.entitiesTableConfig instanceof GroupEntityTableConfig;
  }

}
