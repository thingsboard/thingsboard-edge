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

    if (config.addEnabled) {
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
