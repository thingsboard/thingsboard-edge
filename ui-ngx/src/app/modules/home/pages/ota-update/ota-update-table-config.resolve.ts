///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  ChecksumAlgorithmTranslationMap,
  OtaPackage,
  OtaPackageInfo,
  OtaUpdateTypeTranslationMap
} from '@shared/models/ota-package.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { OtaPackageService } from '@core/http/ota-package.service';
import { PageLink } from '@shared/models/page/page-link';
import { OtaUpdateComponent } from '@home/pages/ota-update/ota-update.component';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { FileSizePipe } from '@shared/pipe/file-size.pipe';

@Injectable()
export class OtaUpdateTableConfigResolve implements Resolve<EntityTableConfig<OtaPackage, PageLink, OtaPackageInfo>> {

  private readonly config: EntityTableConfig<OtaPackage, PageLink, OtaPackageInfo> =
    new EntityTableConfig<OtaPackage, PageLink, OtaPackageInfo>();

  constructor(private translate: TranslateService,
              private datePipe: DatePipe,
              private otaPackageService: OtaPackageService,
              private fileSize: FileSizePipe) {
    this.config.entityType = EntityType.OTA_PACKAGE;
    this.config.entityComponent = OtaUpdateComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.OTA_PACKAGE);
    this.config.entityResources = entityTypeResources.get(EntityType.OTA_PACKAGE);

    this.config.entityTitle = (otaPackage) => otaPackage ? otaPackage.title : '';

    this.config.columns.push(
      new DateEntityTableColumn<OtaPackageInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<OtaPackageInfo>('title', 'ota-update.title', '25%'),
      new EntityTableColumn<OtaPackageInfo>('version', 'ota-update.version', '25%'),
      new EntityTableColumn<OtaPackageInfo>('type', 'ota-update.package-type', '25%', entity => {
        return this.translate.instant(OtaUpdateTypeTranslationMap.get(entity.type));
      }),
      new EntityTableColumn<OtaPackageInfo>('fileName', 'ota-update.file-name', '25%'),
      new EntityTableColumn<OtaPackageInfo>('dataSize', 'ota-update.file-size', '70px', entity => {
        return this.fileSize.transform(entity.dataSize || 0);
      }),
      new EntityTableColumn<OtaPackageInfo>('checksum', 'ota-update.checksum', '540px', entity => {
        return `${ChecksumAlgorithmTranslationMap.get(entity.checksumAlgorithm)}: ${entity.checksum}`;
      }, () => ({}), false)
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('ota-update.download'),
        icon: 'file_download',
        isEnabled: (otaPackage) => otaPackage.hasData,
        onAction: ($event, entity) => this.exportPackage($event, entity)
      }
    );

    this.config.deleteEntityTitle = otaPackage => this.translate.instant('ota-update.delete-ota-update-title',
      { title: otaPackage.title });
    this.config.deleteEntityContent = () => this.translate.instant('ota-update.delete-ota-update-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('ota-update.delete-ota-updates-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('ota-update.delete-ota-updates-text');

    this.config.entitiesFetchFunction = pageLink => this.otaPackageService.getOtaPackages(pageLink);
    this.config.loadEntity = id => this.otaPackageService.getOtaPackageInfo(id.id);
    this.config.saveEntity = otaPackage => this.otaPackageService.saveOtaPackage(otaPackage);
    this.config.deleteEntity = id => this.otaPackageService.deleteOtaPackage(id.id);

    this.config.onEntityAction = action => this.onPackageAction(action);
  }

  resolve(): EntityTableConfig<OtaPackage, PageLink, OtaPackageInfo> {
    this.config.tableTitle = this.translate.instant('ota-update.packages-repository');
    return this.config;
  }

  exportPackage($event: Event, otaPackageInfo: OtaPackageInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.otaPackageService.downloadOtaPackage(otaPackageInfo.id.id).subscribe();
  }

  onPackageAction(action: EntityAction<OtaPackageInfo>): boolean {
    switch (action.action) {
      case 'uploadPackage':
        this.exportPackage(action.event, action.entity);
        return true;
    }
    return false;
  }

}
