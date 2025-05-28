///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import { BehaviorSubject, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityBooleanFunction } from '@home/models/entity/entities-table-config.models';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, take, tap } from 'rxjs/operators';
import { SecretStorage } from '@shared/models/secret-storage.models';
import { SecretStorageService } from '@core/http/secret-storage.service';

export class SecretsDatasource implements DataSource<SecretStorage> {
  private entitiesSubject: Subject<SecretStorage[]>;
  private readonly pageDataSubject: Subject<PageData<SecretStorage>>;

  public pageData$: Observable<PageData<SecretStorage>>;

  public selection = new SelectionModel<SecretStorage>(true, []);

  public dataLoading = true;

  constructor(private secretStorageService: SecretStorageService,
              private secrets: SecretStorage[],
              private selectionEnabledFunction: EntityBooleanFunction<SecretStorage>) {
    if (this.secrets && this.secrets.length) {
      this.entitiesSubject = new BehaviorSubject<SecretStorage[]>(this.secrets);
    } else {
      this.entitiesSubject = new BehaviorSubject<SecretStorage[]>([]);
      this.pageDataSubject = new BehaviorSubject<PageData<SecretStorage>>(emptyPageData<SecretStorage>());
      this.pageData$ = this.pageDataSubject.asObservable();
    }
  }

  connect(collectionViewer: CollectionViewer):
    Observable<SecretStorage[] | ReadonlyArray<SecretStorage>> {
    return this.entitiesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.entitiesSubject.complete();
    if (this.pageDataSubject) {
      this.pageDataSubject.complete();
    }
  }

  reset() {
    this.entitiesSubject.next([]);
    if (this.pageDataSubject) {
      this.pageDataSubject.next(emptyPageData<SecretStorage>());
    }
  }

  loadEntities(pageLink: PageLink): Observable<PageData<SecretStorage>> {
    this.dataLoading = true;
    const result = new ReplaySubject<PageData<SecretStorage>>();
    this.fetchEntities(pageLink).pipe(
      tap(() => {
        this.selection.clear();
      }),
      catchError(() => of(emptyPageData<SecretStorage>())),
    ).subscribe(
      (pageData) => {
        this.entitiesSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
        this.dataLoading = false;
      }
    );
    return result;
  }

  fetchEntities(pageLink: PageLink): Observable<PageData<SecretStorage>> {
    return this.secretStorageService.getSecrets(pageLink);
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.entitiesSubject.pipe(
      map((entities) => numSelected === entities.length)
    );
  }

  isEmpty(): Observable<boolean> {
    return this.entitiesSubject.pipe(
      map((entities) => !entities.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
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

  private selectableEntitiesCount(entities: Array<SecretStorage>): number {
    return entities.filter((entity) => this.selectionEnabledFunction(entity)).length;
  }
}
