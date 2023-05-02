///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import {
  DateEntityTableColumn,
  EntityActionTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { Direction } from '@shared/models/page/sort-order';
import { MatDialog } from '@angular/material/dialog';
import { TimePageLink } from '@shared/models/page/page-link';
import { EntityId } from '@shared/models/id/entity-id';
import { UtilsService } from '@core/services/utils.service';
import { EdgeService } from "@core/http/edge.service";
import {
  CloudEvent,
  CloudEventActionType,
  cloudEventActionTypeTranslations,
  CloudEventType,
  cloudEventTypeTranslations,
  EdgeEventStatus,
  edgeEventStatusColor
} from "@shared/models/edge.models";
import { getCurrentAuthUser } from "@core/auth/auth.selectors";
import { AttributeScope } from "@shared/models/telemetry/telemetry.models";
import { Store } from "@ngrx/store";
import { AppState } from "@core/core.state";
import { AttributeService } from "@core/http/attribute.service";
import {
  CloudEventDetailsDialogComponent,
  CloudEventDetailsDialogData
} from "@home/components/cloud-event/cloud-event-details-dialog.component";
import { EntityService } from '@core/http/entity.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Observable } from 'rxjs';
import { ContentType } from "@shared/models/constants";
import { map, mergeMap } from "rxjs/operators";
import { PageData } from '@shared/models/page/page-data';

export class CloudEventTableConfig extends EntityTableConfig<CloudEvent, TimePageLink> {

  queueStartTs: number;

  constructor(private translate: TranslateService,
              private utils: UtilsService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private edgeService: EdgeService,
              private store: Store<AppState>,
              private attributeService: AttributeService,
              private entityService: EntityService) {
    super();

    this.tableTitle = '';
    this.useTimePageLink = true;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = false;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.defaultSortOrder = { property: 'createdTime', direction: Direction.DESC };
    this.entityTranslations = { noEntities: 'cloud-event.no-cloud-events-prompt' };

    this.entitiesFetchFunction = pageLink => this.fetchCloudEvents(pageLink);

    this.updateColumns();
  }

  private updateColumns() {
    this.columns = [];
    this.columns.push(
      new DateEntityTableColumn<CloudEvent>('createdTime', 'cloud-event.created-time', this.datePipe, '150px'),
      new EntityTableColumn<CloudEvent>('action', 'cloud-event.action', '25%',
        entity => this.translate.instant(cloudEventActionTypeTranslations.get(entity.action)), entity => ({}), false),
      new EntityTableColumn<CloudEvent>('type', 'cloud-event.entity-type', '25%',
        entity => this.translate.instant(cloudEventTypeTranslations.get(entity.type)), entity => ({}), false),
      new EntityTableColumn<CloudEvent>('entityId', 'cloud-event.entity-id', '30%'),
      new EntityTableColumn<CloudEvent>('status', 'event.status', '10%',
        (entity) => this.updateCloudEventStatus(entity.createdTime),
        entity => ({
          color: this.isPending(entity.createdTime) ? edgeEventStatusColor.get(EdgeEventStatus.PENDING) : edgeEventStatusColor.get(EdgeEventStatus.DEPLOYED)
        }), false),
      new EntityActionTableColumn<CloudEvent>('data', 'event.data', {
          name: this.translate.instant('cloud-event.details'),
          icon: 'more_horiz',
          isEnabled: (entity) => this.isCloudEventHasData(entity),
          onAction: ($event, entity) => {
            this.prepareCloudEventContent(entity).subscribe(
              (content) => this.showCloudEventDetails($event, content, 'event.data'),
              () => this.showEntityNotFoundError()
            )
          }
        },
        '10%')
    );
  }

  private fetchCloudEvents(pageLink: TimePageLink): Observable<PageData<CloudEvent>> {
    const authUser = getCurrentAuthUser(this.store);
    const currentTenant: EntityId = {
      id: authUser.tenantId,
      entityType: EntityType.TENANT
    };
    return this.attributeService.getEntityAttributes(currentTenant, AttributeScope.SERVER_SCOPE, ['queueStartTs']).pipe(
      map((attributes) => {
        const queueStartTs = attributes[0];
        this.queueStartTs = queueStartTs && queueStartTs.value ? queueStartTs.value : 0;
      }),
      mergeMap(() => this.edgeService.getCloudEvents(pageLink))
    );
  }

  prepareCloudEventContent(entity: CloudEvent): Observable<string> {
    return this.entityService.getCloudEventByType(entity).pipe(
      map((result) => JSON.stringify(result))
    );
  }

  showCloudEventDetails($event: MouseEvent, content: string, title: string): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<CloudEventDetailsDialogComponent, CloudEventDetailsDialogData>(CloudEventDetailsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        content,
        title,
        contentType: ContentType.JSON
      }
    });
  }

  isPending(createdTime: number): boolean {
    return createdTime > this.queueStartTs;
  }

  updateCloudEventStatus(createdTime: number): string {
    if (this.queueStartTs && createdTime <= this.queueStartTs) {
      return this.translate.instant('edge.deployed');
    } else {
      return this.translate.instant('edge.pending');
    }
  }

  isCloudEventHasData(entity: CloudEvent): boolean {
    return !(entity.type === CloudEventType.EDGE ||
             entity.action === CloudEventActionType.DELETED ||
             entity.action === CloudEventActionType.ADDED)
  }

  showEntityNotFoundError(): void {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('edge.load-entity-error'),
        type: 'error',
        verticalPosition: 'top',
        horizontalPosition: 'left'
      }));
  }
}
