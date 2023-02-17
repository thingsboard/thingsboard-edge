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

import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount, take, tap } from 'rxjs/operators';
import { EntityId } from '@app/shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import {
  AttributeData,
  AttributeScope,
  isClientSideTelemetryType,
  TelemetrySubscriber,
  TelemetryType
} from '@shared/models/telemetry/telemetry.models';
import { AttributeService } from '@core/http/attribute.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { NgZone } from '@angular/core';

export class AttributeDatasource implements DataSource<AttributeData> {

  private attributesSubject = new BehaviorSubject<AttributeData[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<AttributeData>>(emptyPageData<AttributeData>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<AttributeData>(true, []);

  private allAttributes: Observable<Array<AttributeData>>;
  private telemetrySubscriber: TelemetrySubscriber;

  constructor(private attributeService: AttributeService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private translate: TranslateService) {}

  connect(collectionViewer: CollectionViewer): Observable<AttributeData[] | ReadonlyArray<AttributeData>> {
    return this.attributesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.attributesSubject.complete();
    this.pageDataSubject.complete();
    if (this.telemetrySubscriber) {
      this.telemetrySubscriber.unsubscribe();
      this.telemetrySubscriber = null;
    }
  }

  loadAttributes(entityId: EntityId, attributesScope: TelemetryType,
                 pageLink: PageLink, reload: boolean = false): Observable<PageData<AttributeData>> {
    if (reload) {
      this.allAttributes = null;
      if (this.telemetrySubscriber) {
        this.telemetrySubscriber.unsubscribe();
        this.telemetrySubscriber = null;
      }
    }
    this.selection.clear();
    const result = new ReplaySubject<PageData<AttributeData>>();
    this.fetchAttributes(entityId, attributesScope, pageLink).pipe(
      catchError(() => of(emptyPageData<AttributeData>())),
    ).subscribe(
      (pageData) => {
        this.attributesSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
      }
    );
    return result;
  }

  fetchAttributes(entityId: EntityId, attributesScope: TelemetryType,
                  pageLink: PageLink): Observable<PageData<AttributeData>> {
    return this.getAllAttributes(entityId, attributesScope).pipe(
      map((data) => {
        const filteredData = data.filter(attrData => attrData.lastUpdateTs !== 0 && attrData.value !== '');
        return pageLink.filterData(filteredData);
      })
    );
  }

  getAllAttributes(entityId: EntityId, attributesScope: TelemetryType): Observable<Array<AttributeData>> {
    if (!this.allAttributes) {
      let attributesObservable: Observable<Array<AttributeData>>;
      if (isClientSideTelemetryType.get(attributesScope)) {
        this.telemetrySubscriber = TelemetrySubscriber.createEntityAttributesSubscription(
          this.telemetryWsService, entityId, attributesScope, this.zone);
        this.telemetrySubscriber.subscribe();
        attributesObservable = this.telemetrySubscriber.attributeData$();
      } else {
        attributesObservable = this.attributeService.getEntityAttributes(entityId, attributesScope as AttributeScope);
      }
      this.allAttributes = attributesObservable.pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.allAttributes;
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.attributesSubject.pipe(
      map((attributes) => numSelected === attributes.length)
    );
  }

  isEmpty(): Observable<boolean> {
    return this.attributesSubject.pipe(
      map((attributes) => !attributes.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

  masterToggle() {
    this.attributesSubject.pipe(
      tap((attributes) => {
        const numSelected = this.selection.selected.length;
        if (numSelected === attributes.length) {
          this.selection.clear();
        } else {
          attributes.forEach(row => {
            this.selection.select(row);
          });
        }
      }),
      take(1)
    ).subscribe();
  }

}
