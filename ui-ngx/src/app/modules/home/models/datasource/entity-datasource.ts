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

import { PageLink } from '@shared/models/page/page-link';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { BaseData, HasId } from '@shared/models/base-data';
import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { catchError, map, share, take, tap } from 'rxjs/operators';
import { EntityBooleanFunction } from '@home/models/entity/entities-table-config.models';

export type EntitiesFetchFunction<T extends BaseData<HasId>, P extends PageLink> = (pageLink: P) => Observable<PageData<T>>;

export class EntitiesDataSource<T extends BaseData<HasId>, P extends PageLink = PageLink> implements DataSource<T> {

  private entitiesSubject = new BehaviorSubject<T[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<T>>(emptyPageData<T>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<T>(true, []);

  public currentEntity: T = null;

  public dataLoading = true;

  constructor(private fetchFunction: EntitiesFetchFunction<T, P>,
              protected selectionEnabledFunction: EntityBooleanFunction<T>,
              protected dataLoadedFunction: (col?: number, row?: number) => void) {}

  connect(collectionViewer: CollectionViewer): Observable<T[] | ReadonlyArray<T>> {
    return this.entitiesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.entitiesSubject.complete();
    this.pageDataSubject.complete();
  }

  reset() {
    const pageData = emptyPageData<T>();
    this.onEntities(pageData.data);
    this.pageDataSubject.next(pageData);
    this.dataLoadedFunction();
  }

  loadEntities(pageLink: P): Observable<PageData<T>> {
    this.dataLoading = true;
    const result = new ReplaySubject<PageData<T>>();
    this.fetchFunction(pageLink).pipe(
      tap(() => {
        this.selection.clear();
      }),
      catchError(() => of(emptyPageData<T>())),
    ).subscribe(
      (pageData) => {
        this.onEntities(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
        this.dataLoadedFunction();
        this.dataLoading = false;
      }
    );
    return result;
  }

  protected onEntities(entities: T[]) {
    this.entitiesSubject.next(entities);
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.entitiesSubject.pipe(
      map((entities) => numSelected === this.selectableEntitiesCount(entities)),
      share()
    );
  }

  isEmpty(): Observable<boolean> {
    return this.entitiesSubject.pipe(
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

  toggleCurrentEntity(entity: T): boolean {
    if (this.currentEntity !== entity) {
      this.currentEntity = entity;
      return true;
    } else {
      return false;
    }
  }

  isCurrentEntity(entity: T): boolean {
    return (this.currentEntity && entity && this.currentEntity.id && entity.id) &&
      (this.currentEntity.id.id === entity.id.id);
  }

  masterToggle() {
    this.entitiesSubject.pipe(
      tap((entities) => {
        const numSelected = this.selection.selected.length;
        if (numSelected === this.selectableEntitiesCount(entities)) {
          this.selection.clear();
        } else {
          entities.forEach(row => {
            if (this.selectionEnabledFunction(row)) {
              this.selection.select(row);
            }
          });
        }
      }),
      take(1)
    ).subscribe();
  }

  private selectableEntitiesCount(entities: Array<T>): number {
    return entities.filter((entity) => this.selectionEnabledFunction(entity)).length;
  }
}
