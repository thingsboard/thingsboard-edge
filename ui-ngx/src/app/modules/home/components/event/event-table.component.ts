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
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EventTableConfig } from './event-table-config';
import { EventService } from '@core/http/event.service';
import { DialogService } from '@core/services/dialog.service';
import { DebugEventType, EventType } from '@shared/models/event.models';
import { Overlay } from '@angular/cdk/overlay';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-event-table',
  templateUrl: './event-table.component.html',
  styleUrls: ['./event-table.component.scss']
})
export class EventTableComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input()
  tenantId: string;

  @Input()
  defaultEventType: EventType | DebugEventType;

  @Input()
  disabledEventTypes: Array<EventType | DebugEventType>;

  @Input()
  debugEventTypes: Array<DebugEventType>;

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        this.entitiesTable.updateData();
      }
    }
  }

  @Input()
  set entityId(entityId: EntityId) {
    this.entityIdValue = entityId;
    if (this.eventTableConfig && this.eventTableConfig.entityId !== entityId) {
      this.eventTableConfig.eventType = this.defaultEventType;
      this.eventTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  eventTableConfig: EventTableConfig;

  private isEmptyData$: Subscription;

  constructor(private eventService: EventService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.dirtyValue = !this.activeValue;
    this.eventTableConfig = new EventTableConfig(
      this.eventService,
      this.dialogService,
      this.translate,
      this.datePipe,
      this.dialog,
      this.entityIdValue,
      this.tenantId,
      this.defaultEventType,
      this.disabledEventTypes,
      this.debugEventTypes,
      this.overlay,
      this.viewContainerRef,
      this.cd
    );
  }

  ngAfterViewInit() {
    this.isEmptyData$ = this.entitiesTable.dataSource.isEmpty().subscribe(value => this.eventTableConfig.hideClearEventAction = value);
  }

  ngOnDestroy() {
    if (this.isEmptyData$) {
      this.isEmptyData$.unsubscribe();
    }
  }

}
