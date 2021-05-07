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
  Firmware,
  FirmwareInfo,
  FirmwareTypeTranslationMap
} from '@shared/models/firmware.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { FirmwareService } from '@core/http/firmware.service';
import { PageLink } from '@shared/models/page/page-link';
import { FirmwaresComponent } from '@home/pages/firmware/firmwares.component';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { FileSizePipe } from '@shared/pipe/file-size.pipe';

@Injectable()
export class FirmwareTableConfigResolve implements Resolve<EntityTableConfig<Firmware, PageLink, FirmwareInfo>> {

  private readonly config: EntityTableConfig<Firmware, PageLink, FirmwareInfo> = new EntityTableConfig<Firmware, PageLink, FirmwareInfo>();

  constructor(private translate: TranslateService,
              private datePipe: DatePipe,
              private firmwareService: FirmwareService,
              private fileSize: FileSizePipe) {
    this.config.entityType = EntityType.FIRMWARE;
    this.config.entityComponent = FirmwaresComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.FIRMWARE);
    this.config.entityResources = entityTypeResources.get(EntityType.FIRMWARE);

    this.config.entityTitle = (firmware) => firmware ? firmware.title : '';

    this.config.columns.push(
      new DateEntityTableColumn<FirmwareInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<FirmwareInfo>('title', 'firmware.title', '25%'),
      new EntityTableColumn<FirmwareInfo>('version', 'firmware.version', '25%'),
      new EntityTableColumn<FirmwareInfo>('type', 'firmware.type', '25%', entity => {
        return this.translate.instant(FirmwareTypeTranslationMap.get(entity.type));
      }),
      new EntityTableColumn<FirmwareInfo>('fileName', 'firmware.file-name', '25%'),
      new EntityTableColumn<FirmwareInfo>('dataSize', 'firmware.file-size', '70px', entity => {
        return this.fileSize.transform(entity.dataSize || 0);
      }),
      new EntityTableColumn<FirmwareInfo>('checksum', 'firmware.checksum', '540px', entity => {
        return `${ChecksumAlgorithmTranslationMap.get(entity.checksumAlgorithm)}: ${entity.checksum}`;
      }, () => ({}), false)
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('firmware.download'),
        icon: 'file_download',
        isEnabled: (firmware) => firmware.hasData,
        onAction: ($event, entity) => this.exportFirmware($event, entity)
      }
    );

    this.config.deleteEntityTitle = firmware => this.translate.instant('firmware.delete-firmware-title',
      { firmwareTitle: firmware.title });
    this.config.deleteEntityContent = () => this.translate.instant('firmware.delete-firmware-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('firmware.delete-firmwares-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('firmware.delete-firmwares-text');

    this.config.entitiesFetchFunction = pageLink => this.firmwareService.getFirmwares(pageLink);
    this.config.loadEntity = id => this.firmwareService.getFirmwareInfo(id.id);
    this.config.saveEntity = firmware => this.firmwareService.saveFirmware(firmware);
    this.config.deleteEntity = id => this.firmwareService.deleteFirmware(id.id);

    this.config.onEntityAction = action => this.onFirmwareAction(action);
  }

  resolve(): EntityTableConfig<Firmware, PageLink, FirmwareInfo> {
    this.config.tableTitle = this.translate.instant('firmware.firmware');
    return this.config;
  }

  exportFirmware($event: Event, firmware: FirmwareInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.firmwareService.downloadFirmware(firmware.id.id).subscribe();
  }

  onFirmwareAction(action: EntityAction<FirmwareInfo>): boolean {
    switch (action.action) {
      case 'uploadFirmware':
        this.exportFirmware(action.event, action.entity);
        return true;
    }
    return false;
  }

}
