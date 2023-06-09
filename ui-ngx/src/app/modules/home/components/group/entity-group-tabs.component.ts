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
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';
import { entityGroupActionSources, entityGroupActionTypes, EntityGroupInfo } from '@shared/models/entity-group.models';
import { PageLink } from '@shared/models/page/page-link';
import { EntityGroupsTableConfig } from '@home/components/group/entity-groups-table-config';
import { exportableEntityTypes } from '@shared/models/vc.models';
import { groupResourceByGroupType, Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
  selector: 'tb-entity-group-tabs',
  templateUrl: './entity-group-tabs.component.html',
  styleUrls: []
})
export class EntityGroupTabsComponent extends EntityTabsComponent<EntityGroupInfo, PageLink, EntityGroupInfo, EntityGroupsTableConfig> {

  entityGroupActionTypesList = entityGroupActionTypes;

  entityGroupActionSourcesList = entityGroupActionSources;

  constructor(private userPermissionsService: UserPermissionsService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  validateAndMark() {
    this.validate();
    this.detailsForm.markAsDirty();
  }

  onPermissionsChanged() {
    this.entitiesTableConfig.onGroupUpdated();
  }

  hasVersionControl(): boolean {
    if (!this.sharedGroup() &&
      this.authUser.authority === this.authorities.TENANT_ADMIN && this.entity && exportableEntityTypes.includes(this.entity.type)) {
      const entityResource = groupResourceByGroupType.get(this.entity.type);
      return this.userPermissionsService.hasResourcesGenericPermission([Resource.VERSION_CONTROL, entityResource], Operation.READ);
    } else {
      return false;
    }
  }

  sharedGroup(): boolean {
    if (this.entitiesTableConfig) {
      return this.entitiesTableConfig.componentsData.shared === true;
    } else {
      return false;
    }
  }

  private validate() {
    const columnsValid = this.entity.configuration.columns !== null;
    const settingsValid = this.entity.configuration.settings !== null;
    if (!columnsValid || !settingsValid) {
      const errors: any = {};
      if (!columnsValid) {
        errors.columns = true;
      }
      if (!settingsValid) {
        errors.settings = true;
      }
      this.detailsForm.setErrors(errors);
    } else {
      this.detailsForm.setErrors(null);
    }
  }

  protected setEntity(entity: EntityGroupInfo) {
    super.setEntity(entity);
  }
}
