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
