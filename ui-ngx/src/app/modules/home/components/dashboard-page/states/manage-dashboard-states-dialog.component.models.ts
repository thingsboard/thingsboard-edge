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

import { DashboardState } from '@shared/models/dashboard.models';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount } from 'rxjs/operators';

export interface DashboardStateInfo extends DashboardState {
  id: string;
}

export class DashboardStatesDatasource implements DataSource<DashboardStateInfo> {

  private statesSubject = new BehaviorSubject<DashboardStateInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<DashboardStateInfo>>(emptyPageData<DashboardStateInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  private allStates: Observable<Array<DashboardStateInfo>>;

  constructor(private states: {[id: string]: DashboardState }) {
  }

  connect(collectionViewer: CollectionViewer): Observable<DashboardStateInfo[] | ReadonlyArray<DashboardStateInfo>> {
    return this.statesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.statesSubject.complete();
    this.pageDataSubject.complete();
  }

  loadStates(pageLink: PageLink, reload: boolean = false): Observable<PageData<DashboardStateInfo>> {
    if (reload) {
      this.allStates = null;
    }
    const result = new ReplaySubject<PageData<DashboardStateInfo>>();
    this.fetchStates(pageLink).pipe(
      catchError(() => of(emptyPageData<DashboardStateInfo>())),
    ).subscribe(
      (pageData) => {
        this.statesSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
      }
    );
    return result;
  }

  fetchStates(pageLink: PageLink): Observable<PageData<DashboardStateInfo>> {
    return this.getAllStates().pipe(
      map((data) => pageLink.filterData(data))
    );
  }

  getAllStates(): Observable<Array<DashboardStateInfo>> {
    if (!this.allStates) {
      const states: DashboardStateInfo[] = [];
      for (const id of Object.keys(this.states)) {
        const state = this.states[id];
        states.push({id, ...state});
      }
      this.allStates = of(states).pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.allStates;
  }

  isEmpty(): Observable<boolean> {
    return this.statesSubject.pipe(
      map((states) => !states.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

}
