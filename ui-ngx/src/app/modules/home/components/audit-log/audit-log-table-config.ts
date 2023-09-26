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

import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  actionStatusTranslations,
  actionTypeTranslations,
  AuditLog,
  AuditLogMode
} from '@shared/models/audit-log.models';
import { EntityTypeResource, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AuditLogService } from '@core/http/audit-log.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { Direction } from '@shared/models/page/sort-order';
import { MatDialog } from '@angular/material/dialog';
import { TimePageLink } from '@shared/models/page/page-link';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { UserId } from '@shared/models/id/user-id';
import { CustomerId } from '@shared/models/id/customer-id';
import {
  AuditLogDetailsDialogComponent,
  AuditLogDetailsDialogData
} from '@home/components/audit-log/audit-log-details-dialog.component';
import { UtilsService } from '@core/services/utils.service';

export class AuditLogTableConfig extends EntityTableConfig<AuditLog, TimePageLink> {

  constructor(private auditLogService: AuditLogService,
              private translate: TranslateService,
              private utils: UtilsService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private auditLogMode: AuditLogMode = AuditLogMode.TENANT,
              public entityId: EntityId = null,
              public userId: UserId = null,
              public customerId: string = null,
              updateOnInit = true,
              pageMode = false) {
    super({customerId});
    this.loadDataOnInit = updateOnInit;
    this.tableTitle = '';
    this.useTimePageLink = true;
    this.pageMode = pageMode;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.actionsColumnTitle = 'audit-log.details';
    this.entityTranslations = {
      noEntities: 'audit-log.no-audit-logs-prompt',
      search: 'audit-log.search'
    };
    this.entityResources = {
    } as EntityTypeResource<AuditLog>;

    this.entitiesFetchFunction = pageLink => this.fetchAuditLogs(pageLink);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.columns.push(
      new DateEntityTableColumn<AuditLog>('createdTime', 'audit-log.timestamp', this.datePipe, '150px'));

    if (this.auditLogMode !== AuditLogMode.ENTITY) {
      this.columns.push(
        new EntityTableColumn<AuditLog>('entityType', 'audit-log.entity-type', '20%',
          (entity) => translate.instant(entityTypeTranslations.get(entity.entityId.entityType).type)),
        new EntityTableColumn<AuditLog>('entityName', 'audit-log.entity-name', '20%',
          (entity => this.utils.customTranslation(entity.entityName, entity.entityName))
        ),
      );
    }

    if (this.auditLogMode !== AuditLogMode.USER) {
      this.columns.push(
        new EntityTableColumn<AuditLog>('userName', 'audit-log.user', '33%')
      );
    }

    this.columns.push(
      new EntityTableColumn<AuditLog>('actionType', 'audit-log.type', '33%',
        (entity) => translate.instant(actionTypeTranslations.get(entity.actionType))),
      new EntityTableColumn<AuditLog>('actionStatus', 'audit-log.status', '33%',
        (entity) => translate.instant(actionStatusTranslations.get(entity.actionStatus)))
    );

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('audit-log.details'),
        icon: 'more_horiz',
        isEnabled: () => true,
        onAction: ($event, entity) => this.showAuditLogDetails(entity)
      }
    );
  }

  fetchAuditLogs(pageLink: TimePageLink): Observable<PageData<AuditLog>> {
    switch (this.auditLogMode) {
      case AuditLogMode.TENANT:
        return this.auditLogService.getAuditLogs(pageLink);
      case AuditLogMode.ENTITY:
        return this.auditLogService.getAuditLogsByEntityId(this.entityId, pageLink);
      case AuditLogMode.USER:
        return this.auditLogService.getAuditLogsByUserId(this.userId.id, pageLink);
      case AuditLogMode.CUSTOMER:
        return this.auditLogService.getAuditLogsByCustomerId(this.customerId, pageLink);
    }
  }

  showAuditLogDetails(entity: AuditLog) {
    this.dialog.open<AuditLogDetailsDialogComponent, AuditLogDetailsDialogData>(AuditLogDetailsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        auditLog: entity
      }
    });
  }

}
