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

import {
  DateEntityTableColumn,
  EntityActionTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { DebugEventType, Event, EventType } from '@shared/models/event.models';
import { TimePageLink } from '@shared/models/page/page-link';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EventService } from '@app/core/http/event.service';
import { EventTableHeaderComponent } from '@home/components/event/event-table-header.component';
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

export class EventTableConfig extends EntityTableConfig<Event, TimePageLink> {

  eventTypeValue: EventType | DebugEventType;

  set eventType(eventType: EventType | DebugEventType) {
    if (this.eventTypeValue !== eventType) {
      this.eventTypeValue = eventType;
      this.updateColumns(true);
    }
  }

  get eventType(): EventType | DebugEventType {
    return this.eventTypeValue;
  }

  eventTypes: Array<EventType | DebugEventType>;

  constructor(private eventService: EventService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              public entityId: EntityId,
              public tenantId: string,
              private defaultEventType: EventType | DebugEventType,
              private disabledEventTypes: Array<EventType | DebugEventType> = null,
              private debugEventTypes: Array<DebugEventType> = null) {
    super();
    this.loadDataOnInit = false;
    this.tableTitle = '';
    this.useTimePageLink = true;
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = false;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;

    this.headerComponent = EventTableHeaderComponent;

    this.eventTypes = Object.keys(EventType).map(type => EventType[type]);

    if (disabledEventTypes && disabledEventTypes.length) {
      this.eventTypes = this.eventTypes.filter(type => disabledEventTypes.indexOf(type) === -1);
    }

    if (debugEventTypes && debugEventTypes.length) {
      this.eventTypes = [...this.eventTypes, ...debugEventTypes];
    }

    this.eventTypeValue = defaultEventType;

    this.entityTranslations = {
      noEntities: 'event.no-events-prompt'
    };
    this.entityResources = {
    } as EntityTypeResource;
    this.entitiesFetchFunction = pageLink => this.fetchEvents(pageLink);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.updateColumns();
  }

  fetchEvents(pageLink: TimePageLink): Observable<PageData<Event>> {
    return this.eventService.getEvents(this.entityId, this.eventType, this.tenantId, pageLink);
  }

  updateColumns(updateTableColumns: boolean = false): void {
    this.columns = [];
    this.columns.push(
      new DateEntityTableColumn<Event>('createdTime', 'event.event-time', this.datePipe, '120px'),
      new EntityTableColumn<Event>('server', 'event.server', '100px',
        (entity) => entity.body.server, entity => ({}), false));
    switch (this.eventType) {
      case EventType.ERROR:
        this.columns.push(
        new EntityTableColumn<Event>('method', 'event.method', '100%',
          (entity) => entity.body.method, entity => ({}), false),
          new EntityActionTableColumn<Event>('error', 'event.error',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.error && entity.body.error.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.error, 'event.error')
            },
            '100px')
        );
        break;
      case EventType.LC_EVENT:
        this.columns.push(
          new EntityTableColumn<Event>('method', 'event.event', '100%',
            (entity) => entity.body.event, entity => ({}), false),
          new EntityTableColumn<Event>('status', 'event.status', '100%',
            (entity) =>
              this.translate.instant(entity.body.success ? 'event.success' : 'event.failed'), entity => ({}), false),
          new EntityActionTableColumn<Event>('error', 'event.error',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.error && entity.body.error.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.error, 'event.error')
            },
            '100px')
        );
        break;
      case EventType.STATS:
        this.columns.push(
          new EntityTableColumn<Event>('messagesProcessed', 'event.messages-processed', '50%',
            (entity) => entity.body.messagesProcessed + '',
            () => ({}),
            false,
            () => ({}), () => undefined, true),
          new EntityTableColumn<Event>('errorsOccurred', 'event.errors-occurred', '50%',
            (entity) => entity.body.errorsOccurred + '',
            () => ({}),
            false,
            () => ({}), () => undefined, true)
        );
        break;
      case DebugEventType.DEBUG_RULE_NODE:
      case DebugEventType.DEBUG_RULE_CHAIN:
        this.columns[0].width = '100px';
        this.columns.push(
          new EntityTableColumn<Event>('type', 'event.type', '40px',
            (entity) => entity.body.type, entity => ({
              padding: '0 12px 0 0',
            }), false, key => ({
              padding: '0 12px 0 0'
            })),
          new EntityTableColumn<Event>('entity', 'event.entity', '100px',
            (entity) => entity.body.entityName, entity => ({
              padding: '0 12px 0 0',
            }), false, key => ({
              padding: '0 12px 0 0'
            })),
          new EntityTableColumn<Event>('msgId', 'event.message-id', '100px',
            (entity) => entity.body.msgId, entity => ({
              whiteSpace: 'nowrap',
              padding: '0 12px 0 0'
            }), false, key => ({
              padding: '0 12px 0 0'
            }),
            entity => entity.body.msgId),
          new EntityTableColumn<Event>('msgType', 'event.message-type', '100px',
            (entity) => entity.body.msgType, entity => ({
              whiteSpace: 'nowrap',
              padding: '0 12px 0 0'
            }), false, key => ({
              padding: '0 12px 0 0'
            }),
            entity => entity.body.msgType),
          new EntityTableColumn<Event>('relationType', 'event.relation-type', '100px',
            (entity) => entity.body.relationType, entity => ({padding: '0 12px 0 0', }), false, key => ({
              padding: '0 12px 0 0'
            })),
          new EntityActionTableColumn<Event>('data', 'event.data',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.data ? entity.body.data.length > 0 : false,
              onAction: ($event, entity) => this.showContent($event, entity.body.data,
                'event.data', entity.body.dataType)
            },
            '40px'),
          new EntityActionTableColumn<Event>('metadata', 'event.metadata',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.metadata ? entity.body.metadata.length > 0 : false,
              onAction: ($event, entity) => this.showContent($event, entity.body.metadata,
                'event.metadata', ContentType.JSON)
            },
            '40px'),
          new EntityActionTableColumn<Event>('error', 'event.error',
            {
              name: this.translate.instant('action.view'),
              icon: 'more_horiz',
              isEnabled: (entity) => entity.body.error && entity.body.error.length > 0,
              onAction: ($event, entity) => this.showContent($event, entity.body.error,
                'event.error')
            },
            '40px')
        );
        break;
    }
    if (updateTableColumns) {
      this.table.columnsUpdated(true);
    }
  }

  showContent($event: MouseEvent, content: string, title: string, contentType: ContentType = null): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EventContentDialogComponent, EventContentDialogData>(EventContentDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        content,
        title,
        contentType
      }
    });
  }
}
