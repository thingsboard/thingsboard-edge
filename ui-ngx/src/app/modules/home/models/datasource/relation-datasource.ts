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
import { EntityRelationInfo, EntitySearchDirection } from '@shared/models/relation.models';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount, take, tap } from 'rxjs/operators';
import { EntityId } from '@app/shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';

export class RelationsDatasource implements DataSource<EntityRelationInfo> {

  private relationsSubject = new BehaviorSubject<EntityRelationInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<EntityRelationInfo>>(emptyPageData<EntityRelationInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<EntityRelationInfo>(true, []);

  private allRelations: Observable<Array<EntityRelationInfo>>;

  constructor(private entityRelationService: EntityRelationService,
              private translate: TranslateService) {}

  connect(collectionViewer: CollectionViewer): Observable<EntityRelationInfo[] | ReadonlyArray<EntityRelationInfo>> {
    return this.relationsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.relationsSubject.complete();
    this.pageDataSubject.complete();
  }

  loadRelations(direction: EntitySearchDirection, entityId: EntityId,
                pageLink: PageLink, reload: boolean = false): Observable<PageData<EntityRelationInfo>> {
    if (reload) {
      this.allRelations = null;
    }
    const result = new ReplaySubject<PageData<EntityRelationInfo>>();
    this.fetchRelations(direction, entityId, pageLink).pipe(
      tap(() => {
        this.selection.clear();
      }),
      catchError(() => of(emptyPageData<EntityRelationInfo>())),
    ).subscribe(
      (pageData) => {
        this.relationsSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
      }
    );
    return result;
  }

  fetchRelations(direction: EntitySearchDirection, entityId: EntityId,
                 pageLink: PageLink): Observable<PageData<EntityRelationInfo>> {
    return this.getAllRelations(direction, entityId).pipe(
      map((data) => pageLink.filterData(data))
    );
  }

  getAllRelations(direction: EntitySearchDirection, entityId: EntityId): Observable<Array<EntityRelationInfo>> {
    if (!this.allRelations) {
      let relationsObservable: Observable<Array<EntityRelationInfo>>;
      switch (direction) {
        case EntitySearchDirection.FROM:
          relationsObservable = this.entityRelationService.findInfoByFrom(entityId);
          break;
        case EntitySearchDirection.TO:
          relationsObservable = this.entityRelationService.findInfoByTo(entityId);
          break;
      }
      this.allRelations = relationsObservable.pipe(
        map(relations => {
          relations.forEach(relation => {
            if (direction === EntitySearchDirection.FROM) {
              relation.toEntityTypeName = this.translate.instant(entityTypeTranslations.get(relation.to.entityType).type);
              relation.entityURL = getEntityDetailsPageURL(relation.to.id, relation.to.entityType as EntityType);
            } else {
              relation.fromEntityTypeName = this.translate.instant(entityTypeTranslations.get(relation.from.entityType).type);
              relation.entityURL = getEntityDetailsPageURL(relation.from.id, relation.from.entityType as EntityType);
            }
          });
          return relations;
        }),
        publishReplay(1),
        refCount()
      );
    }
    return this.allRelations;
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.relationsSubject.pipe(
      map((relations) => numSelected === relations.length)
    );
  }

  isEmpty(): Observable<boolean> {
    return this.relationsSubject.pipe(
      map((relations) => !relations.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

  masterToggle() {
    this.relationsSubject.pipe(
      tap((relations) => {
        const numSelected = this.selection.selected.length;
        if (numSelected === relations.length) {
          this.selection.clear();
        } else {
          relations.forEach(row => {
            this.selection.select(row);
          });
        }
      }),
      take(1)
    ).subscribe();
  }
}
