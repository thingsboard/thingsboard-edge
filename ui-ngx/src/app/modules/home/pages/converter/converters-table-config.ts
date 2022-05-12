///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
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
import {
  Converter,
  ConverterParams,
  converterTypeTranslationMap,
  getConverterHelpLink
} from '@shared/models/converter.models';
import { ConverterService } from '@core/http/converter.service';
import { ConverterComponent } from '@home/pages/converter/converter.component';
import { ConverterTabsComponent } from '@home/pages/converter/converter-tabs.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Observable } from 'rxjs';
import { isUndefined } from '@core/utils';
import { PageData } from '@shared/models/page/page-data';

export class ConvertersTableConfig extends EntityTableConfig<Converter> {

  constructor(private converterService: ConverterService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private importExport: ImportExportService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private params: ActivatedRouteSnapshot) {

    super();

    this.entityType = EntityType.CONVERTER;
    this.entityComponent = ConverterComponent;
    this.entityTabsComponent = ConverterTabsComponent;
    this.entityTranslations = entityTypeTranslations.get(EntityType.CONVERTER);

    this.componentsData = this.setComponentsData(this.params);

    this.entityResources = {
      helpLinkId: null,
      helpLinkIdForEntity(entity: Converter): string {
        return getConverterHelpLink(entity);
      }
    };
    this.addDialogStyle = {width: '600px'};

    this.entityTitle = (converter) => converter ?
      this.utils.customTranslation(converter.name, converter.name) : '';

    this.columns.push(
      new DateEntityTableColumn<Converter>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Converter>('name', 'converter.name', '33%', this.entityTitle),
      new EntityTableColumn<Converter>('type', 'converter.type', '33%', (converter) => {
        return this.translate.instant(converterTypeTranslationMap.get(converter.type));
      })
    );

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('converter.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportConverter($event, entity)
      }
    );

    this.addActionDescriptors.push(
        {
          name: this.translate.instant('converter.create-new-converter'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.getTable().addEntity($event)
        },
        {
          name: this.translate.instant('converter.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importConverter($event)
        }
    );

    this.deleteEntityTitle = converter =>
      this.translate.instant('converter.delete-converter-title', { converterName: converter.name });
    this.deleteEntityContent = () => this.translate.instant('converter.delete-converter-text');
    this.deleteEntitiesTitle = count => this.translate.instant('converter.delete-converters-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('converter.delete-converters-text');
    this.entitiesFetchFunction = this.configureEntityFunctions(this.componentsData.converterScope);
    this.loadEntity = id => this.converterService.getConverter(id.id);
    this.saveEntity = converter => this.saveConverter(converter);
    this.deleteEntity = id => this.converterService.deleteConverter(id.id);

    this.onEntityAction = action => this.onConverterAction(action);

    this.tableTitle = this.configureTableTitle(this.componentsData.converterScope);
    defaultEntityTablePermissions(this.userPermissionsService, this);
  }

  private setComponentsData(params: any): ConverterParams {
    return {
      converterScope: params.data.convertersType ? params.data.convertersType : 'tenant'
    };
  }

  private configureEntityFunctions(converterScope: string): (pageLink) => Observable<PageData<Converter>> {
    if (converterScope === 'tenant') {
      return pageLink => this.converterService.getConverters(pageLink, false);
    } else if (converterScope === 'edges') {
      return pageLink => this.converterService.getConverters(pageLink, true);
    }
  }

  private saveConverter(converter: Converter): Observable<Converter> {
    if (isUndefined(converter.edgeTemplate)) {
      if (this.componentsData.converterScope === 'tenant') {
        converter.edgeTemplate = false;
      } else if (this.componentsData.converterScope === 'edges') {
        converter.edgeTemplate = true;
      } else {
        // safe fallback to default
        converter.edgeTemplate = false;
      }
    }
    return this.converterService.saveConverter(converter);
  }

  openConverter($event: Event, converter: Converter) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.componentsData.converterScope === 'edges') {
      this.router.navigateByUrl(`edgeManagement/converters/${converter.id.id}`);
    } else {
      this.router.navigateByUrl(`converters/${converter.id.id}`);
    }
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
        this.updateData();
      }
    });
  }

  onConverterAction(action: EntityAction<Converter>): boolean {
    switch (action.action) {
      case 'open':
        this.openConverter(action.event, action.entity);
        return true;
      case 'export':
        this.exportConverter(action.event, action.entity);
        return true;
    }
    return false;
  }

  private configureTableTitle(converterScope: string): string {
    if (converterScope === 'tenant') {
      return this.translate.instant('converter.converters');
    } else if (converterScope === 'edges') {
      return this.translate.instant('edge.converter-templates');
    }
  }

}
