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

import {
  DateEntityTableColumn,
  EntityActionTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  EdgeEvent,
  edgeEventActionTypeTranslations,
  EdgeEventStatus,
  edgeEventStatusColor,
  EdgeEventType,
  edgeEventTypeTranslations
} from '@shared/models/edge.models';
import { TimePageLink } from '@shared/models/page/page-link';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { Direction } from '@shared/models/page/sort-order';
import { DialogService } from '@core/services/dialog.service';
import { ContentType } from '@shared/models/constants';
import {
  EventContentDialogComponent,
  EventContentDialogData
} from '@home/components/event/event-content-dialog.component';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { EdgeDownlinkTableHeaderComponent } from '@home/components/edge/edge-downlink-table-header.component';
import { EdgeService } from '@core/http/edge.service';
import { EntityService } from '@core/http/entity.service';
import { map, mergeMap } from "rxjs/operators";

export class EdgeDownlinkTableConfig extends EntityTableConfig<EdgeEvent, TimePageLink> {

  queueStartTs: number;

  constructor(private attributeService: AttributeService,
              private datePipe: DatePipe,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private edgeService: EdgeService,
              private entityService: EntityService,
              private translate: TranslateService,
              public entityId: EntityId) {
    super();

    this.tableTitle = '';
    this.loadDataOnInit = false;
    this.useTimePageLink = true;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = false;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;

    this.headerComponent = EdgeDownlinkTableHeaderComponent;
    this.entityTranslations = {
      noEntities: 'edge.no-downlinks-prompt'
    };
    this.entityResources = {} as EntityTypeResource<EdgeEvent>;
    this.entitiesFetchFunction = pageLink => this.fetchEvents(pageLink);
    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.updateColumns();
  }

  fetchEvents(pageLink: TimePageLink): Observable<PageData<EdgeEvent>> {
    return this.attributeService.getEntityAttributes(this.entityId, AttributeScope.SERVER_SCOPE, ['queueStartTs']).pipe(
      map((attributes) => {
          const queueStartTs = attributes[0];
          this.queueStartTs = queueStartTs ? queueStartTs.lastUpdateTs : 0;
      }),
      mergeMap(() => this.edgeService.getEdgeEvents(this.entityId, pageLink))
    );
  }

  updateColumns(updateTableColumns: boolean = false): void {
    this.columns = [];
    this.columns.push(
      new DateEntityTableColumn<EdgeEvent>('createdTime', 'event.event-time', this.datePipe, '120px'),
      new EntityTableColumn<EdgeEvent>('type', 'event.type', '25%',
        entity => this.translate.instant(edgeEventTypeTranslations.get(entity.type)), entity => ({}), false),
      new EntityTableColumn<EdgeEvent>('action', 'edge.event-action', '25%',
        entity => this.translate.instant(edgeEventActionTypeTranslations.get(entity.action)), entity => ({}), false),
      new EntityTableColumn<EdgeEvent>('entityId', 'edge.entity-id', '40%',
        (entity) => entity.entityId, entity => ({}), false),
      new EntityTableColumn<EdgeEvent>('status', 'event.status', '10%',
        (entity) => this.updateEdgeEventStatus(entity.createdTime),
        entity => ({
          color: this.isPending(entity.createdTime) ? edgeEventStatusColor.get(EdgeEventStatus.PENDING) : edgeEventStatusColor.get(EdgeEventStatus.DEPLOYED)
        }), false),
      new EntityActionTableColumn<EdgeEvent>('data', 'event.data',
        {
          name: this.translate.instant('action.view'),
          icon: 'more_horiz',
          isEnabled: (entity) => this.isEdgeEventHasData(entity.type),
          onAction: ($event, entity) =>
            {
              this.prepareEdgeEventContent(entity).subscribe(
                (content) => {
                this.showEdgeEventContent($event, content,'event.data');
              });
            }
        },
        '40px'),
    );
    if (updateTableColumns) {
      this.table.columnsUpdated(true);
    }
  }

  updateEdgeEventStatus(createdTime): string {
    if (createdTime < this.queueStartTs) {
      return this.translate.instant('edge.deployed');
    } else {
      return this.translate.instant('edge.pending');
    }
  }

  isPending(createdTime): boolean {
    return createdTime > this.queueStartTs;
  }

  isEdgeEventHasData(edgeEventType: EdgeEventType): boolean {
    switch (edgeEventType) {
      case EdgeEventType.ROLE:
      case EdgeEventType.ADMIN_SETTINGS:
      case EdgeEventType.CUSTOM_TRANSLATION:
      case EdgeEventType.WHITE_LABELING:
      case EdgeEventType.LOGIN_WHITE_LABELING:
        return false;
      default:
        return true;
    }
  }

  prepareEdgeEventContent(entity: any): Observable<string> {
    return this.entityService.getEdgeEventContent(entity).pipe(
      map((result) => JSON.stringify(result))
    );
  }

  showEdgeEventContent($event: MouseEvent, content: string, title: string): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EventContentDialogComponent, EventContentDialogData>(EventContentDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        content,
        title,
        contentType: ContentType.JSON
      }
    });
  }
}
