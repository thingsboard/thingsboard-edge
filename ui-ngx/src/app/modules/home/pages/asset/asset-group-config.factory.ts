///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { EntityType } from '@shared/models/entity-type.models';
import { tap } from 'rxjs/operators';
import { BroadcastService } from '@core/services/broadcast.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerId } from '@shared/models/id/customer-id';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { Asset } from '@shared/models/asset.models';
import { AssetService } from '@core/http/asset.service';
import { AssetComponent } from '@home/pages/asset/asset.component';
import { Operation } from '@shared/models/security.models';

@Injectable()
export class AssetGroupConfigFactory implements EntityGroupStateConfigFactory<Asset> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<Asset>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private assetService: AssetService,
              private broadcast: BroadcastService) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<Asset>): Observable<GroupEntityTableConfig<Asset>> {
    const config = new GroupEntityTableConfig<Asset>(entityGroup, params);

    config.entityComponent = AssetComponent;

    config.entityTitle = (asset) => asset ?
      this.utils.customTranslation(asset.name, asset.name) : '';

    config.deleteEntityTitle = asset => this.translate.instant('asset.delete-asset-title', { assetName: asset.name });
    config.deleteEntityContent = () => this.translate.instant('asset.delete-asset-text');
    config.deleteEntitiesTitle = count => this.translate.instant('asset.delete-assets-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('asset.delete-assets-text');

    config.loadEntity = id => this.assetService.getAsset(id.id);
    config.saveEntity = asset => {
      return this.assetService.saveAsset(asset).pipe(
        tap(() => {
          this.broadcast.broadcast('assetSaved');
        }));
    };
    config.deleteEntity = id => this.assetService.deleteAsset(id.id);

    config.onEntityAction = action => this.onAssetAction(action);

    if (this.userPermissionsService.hasGroupEntityPermission(Operation.CREATE, config.entityGroup)) {
      config.headerActionDescriptors.push(
        {
          name: this.translate.instant('asset.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importAssets($event, config)
        }
      );
    }
    return of(this.groupConfigTableConfigService.prepareConfiguration(params, config));
  }

  importAssets($event: Event, config: GroupEntityTableConfig<Asset>) {
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    let customerId: CustomerId = null;
    if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
      customerId = entityGroup.ownerId as CustomerId;
    }
    this.homeDialogs.importEntities(customerId, EntityType.ASSET, entityGroupId).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('assetSaved');
        config.table.updateData();
      }
    });
  }

  onAssetAction(action: EntityAction<Asset>): boolean {
    return false;
  }

}
