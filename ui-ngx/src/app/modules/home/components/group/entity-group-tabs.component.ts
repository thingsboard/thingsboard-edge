///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';
import { entityGroupActionSources, entityGroupActionTypes, EntityGroupInfo } from '@shared/models/entity-group.models';
import { WidgetActionsData } from '@home/components/widget/action/manage-widget-actions.component.models';
import { PageLink } from '@shared/models/page/page-link';
import { EntityGroupsTableConfig } from '@home/components/group/entity-groups-table-config';

@Component({
  selector: 'tb-entity-group-tabs',
  templateUrl: './entity-group-tabs.component.html',
  styleUrls: []
})
export class EntityGroupTabsComponent extends EntityTabsComponent<EntityGroupInfo, PageLink, EntityGroupInfo, EntityGroupsTableConfig> {

  actionsData: WidgetActionsData;

  entityGroupActionTypesList = entityGroupActionTypes;

  constructor(protected store: Store<AppState>) {
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
    this.actionsData = {
      actionsMap: (entity && entity.configuration ? entity.configuration.actions : {}) || {},
      actionSources: entityGroupActionSources
    };
  }
}
