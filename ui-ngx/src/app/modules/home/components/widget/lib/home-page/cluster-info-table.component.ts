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

import { AfterViewInit, Component, NgZone, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { MAX_SAFE_PAGE_SIZE, PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount, share } from 'rxjs/operators';
import { MatSort } from '@angular/material/sort';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntityDataUpdate, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityKeyType } from '@shared/models/query/query.models';
import { TranslateService } from '@ngx-translate/core';

export interface SystemInfoData {
  serviceId: string;
  serviceType: string;
  cpuUsage: number;
  cpuCount: number;
  memoryUsage: number;
  totalMemory: number;
  discUsage: number;
  totalDiscSpace: number;
}

@Component({
  selector: 'tb-cluster-info-table',
  templateUrl: './cluster-info-table.component.html',
  styleUrls: ['./cluster-info-table.component.scss']
})
export class ClusterInfoTableComponent extends PageComponent implements OnInit, AfterViewInit {

  @ViewChild(MatSort) sort: MatSort;

  displayedColumns = ['serviceId', 'serviceType', 'cpuUsage', 'memoryUsage', 'discUsage'];

  dataSource: SystemInfoDataSource;

  pageLink: PageLink;

  constructor(protected store: Store<AppState>,
              private telemetryWsService: TelemetryWebsocketService,
              private translate: TranslateService,
              private zone: NgZone) {
    super(store);
  }

  ngOnInit() {
    this.dataSource = new SystemInfoDataSource(this.telemetryWsService, this.zone);
    const sortOrder: SortOrder = {
      property: 'serviceId',
      direction: Direction.ASC
    };
    this.pageLink = new PageLink(MAX_SAFE_PAGE_SIZE, 0, null, sortOrder);
    this.dataSource.loadData(this.pageLink);
  }

  ngAfterViewInit() {
    this.sort.sortChange.subscribe(() => this.updateData());
  }

  updateData() {
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadData(this.pageLink);
  }

  statusClass(value: number): string {
    let status = '';
    if (value > 85) {
      status = 'tb-status-critical';
    } else if (value > 75) {
      status = 'tb-status-warn';
    }
    return status;
  }

  infoTooltip(serviceData: SystemInfoData, type: 'cpu' | 'ram' | 'disc'): string {
    let tooltip = '';
    let value: number;
    switch (type) {
      case 'cpu':
        value = serviceData.cpuUsage;
        tooltip += value + ' / 100 %';
        break;
      case 'ram':
        value = serviceData.memoryUsage;
        const memoryUsage = serviceData.memoryUsage;
        const totalMemory = serviceData.totalMemory / 1073741824;
        const usedMemory = (totalMemory * (memoryUsage / 100)).toFixed(0);
        tooltip += usedMemory + ' / ' + totalMemory.toFixed(0) + ' Gb';
        break;
      case 'disc':
        value = serviceData.discUsage;
        const discUsage = serviceData.discUsage;
        const totalDiscSpace = serviceData.totalDiscSpace / 1073741824;
        const usedDisc = (totalDiscSpace * (discUsage / 100)).toFixed(0);
        tooltip += usedDisc + ' / ' + totalDiscSpace.toFixed(0) + ' Gb';
        break;
    }
    if (value > 85) {
      switch (type) {
        case 'cpu':
          tooltip += '\n\n' + this.translate.instant('widgets.system-info.cpu-critical-text');
          break;
        case 'ram':
          tooltip += '\n\n' + this.translate.instant('widgets.system-info.ram-critical-text');
          break;
        case 'disc':
          tooltip += '\n\n' + this.translate.instant('widgets.system-info.disk-critical-text');
          break;
      }
    } else if (value > 75) {
      switch (type) {
        case 'cpu':
          tooltip += '\n\n' + this.translate.instant('widgets.system-info.cpu-warning-text');
          break;
        case 'ram':
          tooltip += '\n\n' + this.translate.instant('widgets.system-info.ram-warning-text');
          break;
        case 'disc':
          tooltip += '\n\n' + this.translate.instant('widgets.system-info.disk-warning-text');
          break;
      }
    }
    return tooltip;
  }
}

export class SystemInfoDataSource implements DataSource<SystemInfoData> {

  private systemDataSubject = new BehaviorSubject<SystemInfoData[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<SystemInfoData>>(emptyPageData<SystemInfoData>());

  public pageData$ = this.pageDataSubject.asObservable();

  private allSystemData: Observable<Array<SystemInfoData>>;

  private telemetrySubscriber: TelemetrySubscriber;

  constructor(private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone) {
  }

  connect(collectionViewer: CollectionViewer): Observable<SystemInfoData[] | ReadonlyArray<SystemInfoData>> {
    return this.systemDataSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.systemDataSubject.complete();
    this.pageDataSubject.complete();
    if (this.telemetrySubscriber) {
      this.telemetrySubscriber.unsubscribe();
      this.telemetrySubscriber = null;
    }
  }

  loadData(pageLink: PageLink): void {
    this.getAllSystemData().pipe(
      map((data) => pageLink.filterData(data)),
      catchError(() => of(emptyPageData<SystemInfoData>())),
    ).subscribe(
      (pageData) => {
        this.onSystemData(pageData.data);
        this.pageDataSubject.next(pageData);
      }
    );
  }

  getAllSystemData(): Observable<Array<SystemInfoData>> {
    if (!this.allSystemData) {
      this.telemetrySubscriber = TelemetrySubscriber.createEntityFilterLatestSubscription(
        this.telemetryWsService, { type: AliasFilterType.apiUsageState }, this.zone,
        [ { key: 'clusterSystemData', type: EntityKeyType.TIME_SERIES } ]);
      this.telemetrySubscriber.subscribe();
      this.allSystemData = this.telemetrySubscriber.entityData$.pipe(
        map((update) => this.toSystemInfoData(update)),
        publishReplay(1),
        refCount()
      );
    }
    return this.allSystemData;
  }

  private toSystemInfoData(entityDataUpdate: EntityDataUpdate): Array<SystemInfoData> {
    const entityData = entityDataUpdate.data?.data ? entityDataUpdate.data?.data : entityDataUpdate.update;
    if (entityData && entityData.length) {
      const latest = entityData[0].latest;
      if (latest && latest[EntityKeyType.TIME_SERIES]) {
        const latestTs = latest[EntityKeyType.TIME_SERIES];
        const tsValue = latestTs.clusterSystemData;
        if (tsValue && tsValue.value) {
          return JSON.parse(tsValue.value);
        }
      }
    }
    return [];
  }

  protected onSystemData(data: SystemInfoData[]) {
    this.systemDataSubject.next(data);
  }

  isEmpty(): Observable<boolean> {
    return this.systemDataSubject.pipe(
      map((entities) => !entities.length),
      share()
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements),
      share()
    );
  }

}
