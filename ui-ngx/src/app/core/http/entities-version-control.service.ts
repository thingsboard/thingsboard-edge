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
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable, of, timer } from 'rxjs';
import {
  BranchInfo,
  EntityDataDiff,
  EntityDataInfo,
  EntityLoadError,
  entityLoadErrorTranslationMap,
  EntityLoadErrorType,
  EntityVersion,
  VersionCreateRequest,
  VersionCreationResult,
  VersionLoadRequest,
  VersionLoadResult
} from '@shared/models/vc.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectIsUserLoaded } from '@core/auth/auth.selectors';
import { catchError, finalize, switchMap, takeWhile, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActionLoadFinish, ActionLoadStart } from '@core/interceptors/load.actions';

@Injectable({
  providedIn: 'root'
})
export class EntitiesVersionControlService {

  branchList: Array<BranchInfo> = null;

  constructor(
    private http: HttpClient,
    private translate: TranslateService,
    private sanitizer: DomSanitizer,
    private store: Store<AppState>
  ) {

    this.store.pipe(select(selectIsUserLoaded)).subscribe(
      () => {
        this.branchList = null;
      }
    );
  }

  public clearBranchList(): void {
    this.branchList = null;
  }

  public listBranches(): Observable<Array<BranchInfo>> {
    if (!this.branchList) {
      return this.http.get<Array<BranchInfo>>('/api/entities/vc/branches',
        defaultHttpOptionsFromConfig({ignoreErrors: true, ignoreLoading: false})).pipe(
        catchError(() => of([] as Array<BranchInfo>)),
        tap((list) => {
          this.branchList = list;
        })
      );
    } else {
      return of(this.branchList);
    }
  }

  public getEntityDataInfo(externalEntityId: EntityId,
                           internalEntityId: EntityId,
                           versionId: string,
                           config?: RequestConfig): Observable<EntityDataInfo> {
    return this.http.get<EntityDataInfo>(`/api/entities/vc/info/${versionId}/${externalEntityId.entityType}/${externalEntityId.id}?internalEntityId=${internalEntityId.id}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveEntitiesVersion(request: VersionCreateRequest, config?: RequestConfig): Observable<VersionCreationResult> {
    this.store.dispatch(new ActionLoadStart());
    return this.http.post<string>('/api/entities/vc/version', request,
      defaultHttpOptionsFromConfig({...config, ...{ignoreLoading: true}})).pipe(
      switchMap((requestId) => {
        return timer(0, 2000).pipe(
          switchMap(() => this.getVersionCreateRequestStatus(requestId, config)),
          takeWhile((res) => !res.done, true)
        );
      }),
      finalize(() => {
        const branch = request.branch;
        if (this.branchList && !this.branchList.find(b => b.name === branch)) {
          this.branchList = null;
        }
        this.store.dispatch(new ActionLoadFinish());
      }),
    );
  }

  private getVersionCreateRequestStatus(requestId: string, config?: RequestConfig): Observable<VersionCreationResult> {
    return this.http.get<VersionCreationResult>(`/api/entities/vc/version/${requestId}/status`,
      defaultHttpOptionsFromConfig({...config, ...{ignoreLoading: true}}));
  }

  public listEntityVersions(pageLink: PageLink, branch: string,
                            externalEntityId: EntityId,
                            internalEntityId: EntityId,
                            config?: RequestConfig): Observable<PageData<EntityVersion>> {
    const encodedBranch = encodeURIComponent(branch);
    return this.http.get<PageData<EntityVersion>>(`/api/entities/vc/version/${externalEntityId.entityType}/${externalEntityId.id}${pageLink.toQuery()}&branch=${encodedBranch}&internalEntityId=${internalEntityId.id}`,
      defaultHttpOptionsFromConfig(config));
  }

  public listEntityTypeVersions(pageLink: PageLink, branch: string,
                                entityType: EntityType,
                                config?: RequestConfig): Observable<PageData<EntityVersion>> {
    const encodedBranch = encodeURIComponent(branch);
    return this.http.get<PageData<EntityVersion>>(`/api/entities/vc/version/${entityType}${pageLink.toQuery()}&branch=${encodedBranch}`,
      defaultHttpOptionsFromConfig(config));
  }

  public listVersions(pageLink: PageLink, branch: string,
                      config?: RequestConfig): Observable<PageData<EntityVersion>> {
    const encodedBranch = encodeURIComponent(branch);
    return this.http.get<PageData<EntityVersion>>(`/api/entities/vc/version${pageLink.toQuery()}&branch=${encodedBranch}`,
      defaultHttpOptionsFromConfig(config));
  }

  public loadEntitiesVersion(request: VersionLoadRequest, config?: RequestConfig): Observable<VersionLoadResult> {
    this.store.dispatch(new ActionLoadStart());
    return this.http.post<string>('/api/entities/vc/entity', request,
      defaultHttpOptionsFromConfig({...config, ...{ignoreLoading: true}})).pipe(
      switchMap((requestId) => {
        return timer(0, 2000).pipe(
          switchMap(() => this.getVersionLoadRequestStatus(requestId, config)),
          takeWhile((res) => !res.done, true),
        );
      }),
      finalize(() => {
        this.store.dispatch(new ActionLoadFinish());
      }),
    );
  }

  private getVersionLoadRequestStatus(requestId: string, config?: RequestConfig): Observable<VersionLoadResult> {
    return this.http.get<VersionLoadResult>(`/api/entities/vc/entity/${requestId}/status`,
      defaultHttpOptionsFromConfig({...config, ...{ignoreLoading: true}}));
  }

  public compareEntityDataToVersion(entityId: EntityId,
                                    versionId: string,
                                    config?: RequestConfig): Observable<EntityDataDiff> {
    return this.http.get<EntityDataDiff>(`/api/entities/vc/diff/${entityId.entityType}/${entityId.id}?versionId=${versionId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public entityLoadErrorToMessage(entityLoadError: EntityLoadError): SafeHtml {
    const type = entityLoadError.type;
    const messageId = entityLoadErrorTranslationMap.get(type);
    const messageArgs = {} as any;
    switch (type) {
      case EntityLoadErrorType.DEVICE_CREDENTIALS_CONFLICT:
      case EntityLoadErrorType.INTEGRATION_ROUTING_KEY_CONFLICT:
        messageArgs.entityId = entityLoadError.source.id;
        break;
      case EntityLoadErrorType.MISSING_REFERENCED_ENTITY:
        messageArgs.sourceEntityTypeName =
          (this.translate.instant(entityTypeTranslations.get(entityLoadError.source.entityType).type) as string).toLowerCase();
        messageArgs.sourceEntityId = entityLoadError.source.id;
        messageArgs.targetEntityTypeName =
          (this.translate.instant(entityTypeTranslations.get(entityLoadError.target.entityType).type) as string).toLowerCase();
        messageArgs.targetEntityId = entityLoadError.target.id;
        break;
      case EntityLoadErrorType.RUNTIME:
        messageArgs.message = entityLoadError.message;
        break;
    }
    return this.sanitizer.bypassSecurityTrustHtml(this.translate.instant(messageId, messageArgs));
  }
}
