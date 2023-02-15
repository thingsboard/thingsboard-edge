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

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import { EntityTableColumn, EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import {
  QueueInfo,
  QueueProcessingStrategyTypesMap,
  QueueSubmitStrategyTypesMap,
  ServiceType
} from '@shared/models/queue.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BroadcastService } from '@core/services/broadcast.service';
import { CustomerService } from '@core/http/customer.service';
import { DialogService } from '@core/services/dialog.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { map, mergeMap, take } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@app/shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { QueueComponent } from './queue.component';
import { QueueService } from '@core/http/queue.service';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { EntityAction } from '@home/models/entity/entity-component.models';

@Injectable()
export class QueuesTableConfigResolver implements Resolve<EntityTableConfig<QueueInfo>> {

  readonly queueType = ServiceType.TB_RULE_ENGINE;

  private readonly config: EntityTableConfig<QueueInfo> = new EntityTableConfig<QueueInfo>();

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              private queueService: QueueService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private router: Router,
              private translate: TranslateService) {

    this.config.entityType = EntityType.QUEUE;
    this.config.entityComponent = QueueComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.QUEUE);
    this.config.entityResources = entityTypeResources.get(EntityType.QUEUE);

    this.config.deleteEntityTitle = queue => this.translate.instant('queue.delete-queue-title', {queueName: queue.name});
    this.config.deleteEntityContent = () => this.translate.instant('queue.delete-queue-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('queue.delete-queues-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('queue.delete-queues-text');

    this.config.onEntityAction = action => this.onQueueAction(action);
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<QueueInfo>> {
    this.config.componentsData = {
      queueType: this.queueType
    };

    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      map(() => {
        this.config.tableTitle = this.translate.instant('admin.queues');
        this.config.columns = this.configureColumns();
        this.configureEntityFunctions();
        return this.config;
      })
    );
  }

  configureColumns(): Array<EntityTableColumn<QueueInfo>> {
    return [
      new EntityTableColumn<QueueInfo>('name', 'admin.queue-name', '25%'),
      new EntityTableColumn<QueueInfo>('partitions', 'admin.queue-partitions', '25%'),
      new EntityTableColumn<QueueInfo>('submitStrategy', 'admin.queue-submit-strategy', '25%',
        (entity: QueueInfo) => {
          return this.translate.instant(QueueSubmitStrategyTypesMap.get(entity.submitStrategy.type).label);
        },
        () => ({}),
        false
      ),
      new EntityTableColumn<QueueInfo>('processingStrategy', 'admin.queue-processing-strategy', '25%',
        (entity: QueueInfo) => {
          return this.translate.instant(QueueProcessingStrategyTypesMap.get(entity.processingStrategy.type).label);
        },
        () => ({}),
        false
      )
    ];
  }

  configureEntityFunctions(): void {
    this.config.entitiesFetchFunction = pageLink => this.queueService.getTenantQueuesByServiceType(pageLink, this.queueType);
    this.config.loadEntity = id => this.queueService.getQueueById(id.id);
    this.config.saveEntity = queue => this.queueService.saveQueue(queue, this.queueType).pipe(
      mergeMap((savedQueue) => this.queueService.getQueueById(savedQueue.id.id)
      ));
    this.config.deleteEntity = id => this.queueService.deleteQueue(id.id);
    this.config.deleteEnabled = (queue) => queue && queue.name !== 'Main';
    this.config.entitySelectionEnabled = (queue) => queue && queue.name !== 'Main';
  }

  onQueueAction(action: EntityAction<QueueInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openQueue(action.event, action.entity);
        return true;
    }
    return false;
  }

  private openQueue($event: Event, queue) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree(['settings', 'queues', queue.id.id]);
    this.router.navigateByUrl(url);
  }
}
