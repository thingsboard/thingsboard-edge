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

import { Injectable } from '@angular/core';

import { Resolve } from '@angular/router';
import {
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Converter, converterTypeTranslationMap, getConverterHelpLink } from '@shared/models/converter.models';
import { ConverterService } from '@core/http/converter.service';
import { ConverterComponent } from '@home/pages/converter/converter.component';
import { ConverterTabsComponent } from '@home/pages/converter/converter-tabs.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class ConvertersTableConfigResolver implements Resolve<EntityTableConfig<Converter>> {

  private readonly config: EntityTableConfig<Converter> = new EntityTableConfig<Converter>();

  constructor(private converterService: ConverterService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private importExport: ImportExportService,
              private datePipe: DatePipe,
              private utils: UtilsService) {

    this.config.entityType = EntityType.CONVERTER;
    this.config.entityComponent = ConverterComponent;
    this.config.entityTabsComponent = ConverterTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.CONVERTER);
    this.config.entityResources = {
      helpLinkId: null,
      helpLinkIdForEntity(entity: Converter): string {
        return getConverterHelpLink(entity);
      }
    };
    this.config.addDialogStyle = {width: '600px'};

    this.config.entityTitle = (converter) => converter ?
      this.utils.customTranslation(converter.name, converter.name) : '';

    this.config.columns.push(
      new DateEntityTableColumn<Converter>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Converter>('name', 'converter.name', '33%', this.config.entityTitle),
      new EntityTableColumn<Converter>('type', 'converter.type', '33%', (converter) => {
        return this.translate.instant(converterTypeTranslationMap.get(converter.type))
      })
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('converter.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportConverter($event, entity)
      }
    );

    this.config.addActionDescriptors.push(
        {
          name: this.translate.instant('converter.create-new-converter'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.config.table.addEntity($event)
        },
        {
          name: this.translate.instant('converter.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importConverter($event)
        }
    );

    this.config.deleteEntityTitle = converter =>
      this.translate.instant('converter.delete-converter-title', { converterName: converter.name });
    this.config.deleteEntityContent = () => this.translate.instant('converter.delete-converter-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('converter.delete-converters-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('converter.delete-converters-text');
    this.config.entitiesFetchFunction = pageLink => this.converterService.getConverters(pageLink);
    this.config.loadEntity = id => this.converterService.getConverter(id.id);
    this.config.saveEntity = converter => this.converterService.saveConverter(converter);
    this.config.deleteEntity = id => this.converterService.deleteConverter(id.id);

    this.config.onEntityAction = action => this.onConverterAction(action);
  }

  resolve(): EntityTableConfig<Converter> {
    this.config.tableTitle = this.translate.instant('converter.converters');
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  exportConverter($event: Event, converter: Converter) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportConverter(converter.id.id);
  }

  importConverter($event: Event) {
    this.importExport.importConverter().subscribe(
     (converter) => {
      if (converter) {
        this.config.table.updateData();
      }
    });
  }

  onConverterAction(action: EntityAction<Converter>): boolean {
    switch (action.action) {
      case 'export':
        this.exportConverter(action.event, action.entity);
        return true;
    }
    return false;
  }

}
