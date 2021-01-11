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

import { Observable, of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Injectable } from '@angular/core';
import { tap } from 'rxjs/operators';
import { BroadcastService } from '@core/services/broadcast.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { EntityView } from '@shared/models/entity-view.models';
import { EntityViewService } from '@core/http/entity-view.service';
import { EntityViewComponent } from '@home/pages/entity-view/entity-view.component';

@Injectable()
export class EntityViewGroupConfigFactory implements EntityGroupStateConfigFactory<EntityView> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<EntityView>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private entityViewService: EntityViewService,
              private broadcast: BroadcastService) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<EntityView>): Observable<GroupEntityTableConfig<EntityView>> {
    const config = new GroupEntityTableConfig<EntityView>(entityGroup, params);

    config.entityComponent = EntityViewComponent;

    config.entityTitle = (entityView) => entityView ?
      this.utils.customTranslation(entityView.name, entityView.name) : '';

    config.deleteEntityTitle = entityView =>
      this.translate.instant('entity-view.delete-entity-view-title', { entityViewName: entityView.name });
    config.deleteEntityContent = () => this.translate.instant('entity-view.delete-entity-view-text');
    config.deleteEntitiesTitle = count => this.translate.instant('entity-view.delete-entity-views-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('entity-view.delete-entity-views-text');

    config.loadEntity = id => this.entityViewService.getEntityView(id.id);
    config.saveEntity = entityView => {
      return this.entityViewService.saveEntityView(entityView).pipe(
        tap(() => {
          this.broadcast.broadcast('entityViewSaved');
        }));
    };
    config.deleteEntity = id => this.entityViewService.deleteEntityView(id.id);

    config.onEntityAction = action => this.onEntityViewAction(action);

    return of(this.groupConfigTableConfigService.prepareConfiguration(params, config));
  }

  onEntityViewAction(action: EntityAction<EntityView>): boolean {
    return false;
  }

}
