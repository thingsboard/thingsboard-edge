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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable, of, ReplaySubject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import {
  BaseWidgetType,
  DeprecatedFilter,
  fullWidgetTypeFqn,
  WidgetType,
  widgetType,
  WidgetTypeDetails,
  WidgetTypeInfo,
  widgetTypesData
} from '@shared/models/widget.models';
import { TranslateService } from '@ngx-translate/core';
import { toWidgetInfo, toWidgetTypeDetails, WidgetInfo } from '@app/modules/home/models/widget-component.models';
import { filter, map, mergeMap, tap } from 'rxjs/operators';
import { WidgetTypeId } from '@shared/models/id/widget-type-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { ActivationEnd, Router } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { sortEntitiesByIds } from '@shared/models/base-data';

@Injectable({
  providedIn: 'root'
})
export class WidgetService {

  private allWidgetsBundles: Array<WidgetsBundle>;
  private systemWidgetsBundles: Array<WidgetsBundle>;
  private tenantWidgetsBundles: Array<WidgetsBundle>;

  private widgetsInfoInMemoryCache = new Map<string, WidgetInfo>();

  private widgetsBundleCacheSubject: ReplaySubject<any> = null;

  constructor(
    private http: HttpClient,
    private userPermissionsService: UserPermissionsService,
    private translate: TranslateService,
    private router: Router
  ) {
    this.router.events.pipe(filter(event => event instanceof ActivationEnd)).subscribe(
      () => {
        this.invalidateWidgetsBundleCache();
      }
    );
  }

  public getWidgetScopeVariables(): string[] {
    return ['tinycolor', 'cssjs', 'moment', '$', 'jQuery'];
  }

  public getAllWidgetsBundles(config?: RequestConfig): Observable<Array<WidgetsBundle>> {
    return this.loadWidgetsBundleCache(config).pipe(
      map(() => this.allWidgetsBundles)
    );
  }

  public getSystemWidgetsBundles(config?: RequestConfig): Observable<Array<WidgetsBundle>> {
    return this.loadWidgetsBundleCache(config).pipe(
      map(() => this.systemWidgetsBundles)
    );
  }

  public getTenantWidgetsBundles(config?: RequestConfig): Observable<Array<WidgetsBundle>> {
    return this.loadWidgetsBundleCache(config).pipe(
      map(() => this.tenantWidgetsBundles)
    );
  }

  public getWidgetBundles(pageLink: PageLink, fullSearch = false, config?: RequestConfig): Observable<PageData<WidgetsBundle>> {
    return this.http.get<PageData<WidgetsBundle>>(`/api/widgetsBundles${pageLink.toQuery()}&fullSearch=${fullSearch}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getWidgetsBundle(widgetsBundleId: string,
                          config?: RequestConfig): Observable<WidgetsBundle> {
    return this.http.get<WidgetsBundle>(`/api/widgetsBundle/${widgetsBundleId}`, defaultHttpOptionsFromConfig(config));
  }

  public getWidgetsBundlesByIds(widgetsBundleIds: Array<string>, config?: RequestConfig): Observable<Array<WidgetsBundle>> {
    return this.http.get<Array<WidgetsBundle>>(`/api/widgetsBundles?widgetsBundleIds=${widgetsBundleIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((roles) => sortEntitiesByIds(roles, widgetsBundleIds))
    );
  }

  public saveWidgetsBundle(widgetsBundle: WidgetsBundle,
                           config?: RequestConfig): Observable<WidgetsBundle> {
    return this.http.post<WidgetsBundle>('/api/widgetsBundle', widgetsBundle,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap(() => {
        this.invalidateWidgetsBundleCache();
      })
    );
  }

  public updateWidgetsBundleWidgetTypes(widgetsBundleId: string, widgetTypeIds: Array<string>,
                                        config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/widgetsBundle/${widgetsBundleId}/widgetTypes`, widgetTypeIds,
      defaultHttpOptionsFromConfig(config));
  }

  public updateWidgetsBundleWidgetFqns(widgetsBundleId: string, widgetTypeFqns: Array<string>,
                                       config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/widgetsBundle/${widgetsBundleId}/widgetTypeFqns`, widgetTypeFqns,
      defaultHttpOptionsFromConfig(config));
  }

  public deleteWidgetsBundle(widgetsBundleId: string, config?: RequestConfig) {
    return this.getWidgetsBundle(widgetsBundleId, config).pipe(
      mergeMap((widgetsBundle) => this.http.delete(`/api/widgetsBundle/${widgetsBundleId}`,
          defaultHttpOptionsFromConfig(config)).pipe(
          tap(() => {
            this.invalidateWidgetsBundleCache();
          })
        )
      ));
  }

  public getBundleWidgetTypes(widgetsBundleId: string,
                              config?: RequestConfig): Observable<Array<WidgetType>> {
    return this.http.get<Array<WidgetType>>(`/api/widgetTypes?widgetsBundleId=${widgetsBundleId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getBundleWidgetTypesDetails(widgetsBundleId: string,
                                     config?: RequestConfig): Observable<Array<WidgetTypeDetails>> {
    return this.http.get<Array<WidgetTypeDetails>>(`/api/widgetTypesDetails?widgetsBundleId=${widgetsBundleId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getBundleWidgetTypeFqns(widgetsBundleId: string,
                                 config?: RequestConfig): Observable<Array<string>> {
    return this.http.get<Array<string>>(`/api/widgetTypeFqns?widgetsBundleId=${widgetsBundleId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getBundleWidgetTypeInfosList(widgetsBundleId: string,
                                      config?: RequestConfig): Observable<Array<WidgetTypeInfo>> {
    return this.getBundleWidgetTypeInfos(new PageLink(1024), widgetsBundleId, false, DeprecatedFilter.ALL, null, config).pipe(
      map((data) => data.data)
    );
  }

  public getBundleWidgetTypeInfos(pageLink: PageLink,
                                  widgetsBundleId: string,
                                  fullSearch = false,
                                  deprecatedFilter = DeprecatedFilter.ALL,
                                  widgetTypes: Array<widgetType> = null,
                                  config?: RequestConfig): Observable<PageData<WidgetTypeInfo>> {

    let url =
      `/api/widgetTypesInfos${pageLink.toQuery()}&widgetsBundleId=${widgetsBundleId}` +
      `&fullSearch=${fullSearch}&deprecatedFilter=${deprecatedFilter}`;
    if (widgetTypes && widgetTypes.length) {
      url += `&widgetTypeList=${widgetTypes.join(',')}`;
    }
    return this.http.get<PageData<WidgetTypeInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getWidgetType(fullFqn: string, config?: RequestConfig): Observable<WidgetType> {
    return this.http.get<WidgetType>(`/api/widgetType?fqn=${fullFqn}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveWidgetTypeDetails(widgetInfo: WidgetInfo,
                               id: WidgetTypeId,
                               createdTime: number,
                               config?: RequestConfig): Observable<WidgetTypeDetails> {
    const widgetTypeDetails = toWidgetTypeDetails(widgetInfo, id, undefined, createdTime);
    return this.http.post<WidgetTypeDetails>('/api/widgetType', widgetTypeDetails,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap((savedWidgetType) => {
        this.widgetTypeUpdated(savedWidgetType);
      }));
  }

  public saveImportedWidgetTypeDetails(widgetTypeDetails: WidgetTypeDetails,
                                       config?: RequestConfig): Observable<WidgetTypeDetails> {
    return this.http.post<WidgetTypeDetails>('/api/widgetType?updateExistingByFqn=true', widgetTypeDetails,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap((savedWidgetType) => {
        this.widgetTypeUpdated(savedWidgetType);
      }));
  }

  public getWidgetTypeById(widgetTypeId: string,
                           config?: RequestConfig): Observable<WidgetTypeDetails> {
    return this.http.get<WidgetTypeDetails>(`/api/widgetType/${widgetTypeId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getWidgetTypeInfoById(widgetTypeId: string,
                               config?: RequestConfig): Observable<WidgetTypeInfo> {
    return this.http.get<WidgetTypeInfo>(`/api/widgetTypeInfo/${widgetTypeId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveWidgetType(widgetTypeDetails: WidgetTypeDetails,
                        config?: RequestConfig): Observable<WidgetTypeDetails> {
    return this.http.post<WidgetTypeDetails>(`/api/widgetType`, widgetTypeDetails,
      defaultHttpOptionsFromConfig(config));
  }

  public deleteWidgetType(widgetTypeId: string,
                          config?: RequestConfig) {
    return this.getWidgetTypeById(widgetTypeId, config).pipe(
      mergeMap((widgetTypeDetails) =>
        this.http.delete(`/api/widgetType/${widgetTypeId}`, defaultHttpOptionsFromConfig(config)).pipe(
          tap(() => {
            this.widgetTypeUpdated(widgetTypeDetails);
          })
        )
    ));
  }

  public getWidgetTypes(pageLink: PageLink, tenantOnly = false,
                        fullSearch = false, deprecatedFilter = DeprecatedFilter.ALL, widgetTypes: Array<widgetType> = null,
                        config?: RequestConfig): Observable<PageData<WidgetTypeInfo>> {
    let url =
      `/api/widgetTypes${pageLink.toQuery()}&tenantOnly=${tenantOnly}&fullSearch=${fullSearch}&deprecatedFilter=${deprecatedFilter}`;
    if (widgetTypes && widgetTypes.length) {
      url += `&widgetTypeList=${widgetTypes.join(',')}`;
    }
    return this.http.get<PageData<WidgetTypeInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public addWidgetFqnToWidgetBundle(widgetsBundleId: string, fqn: string, config?: RequestConfig) {
    return this.getBundleWidgetTypeFqns(widgetsBundleId, config).pipe(
      mergeMap(widgetsBundleFqn => {
        widgetsBundleFqn.push(fqn);
        return this.updateWidgetsBundleWidgetFqns(widgetsBundleId, widgetsBundleFqn, config);
      })
    );
  }

  public getWidgetTemplate(widgetTypeParam: widgetType,
                           config?: RequestConfig): Observable<WidgetInfo> {
    const templateWidgetType = widgetTypesData.get(widgetTypeParam);
    return this.getWidgetType(templateWidgetType.template.fullFqn,
      config).pipe(
        map((result) => {
          const widgetInfo = toWidgetInfo(result);
          widgetInfo.fullFqn = undefined;
          return widgetInfo;
        })
      );
  }

  public clearWidgetInfoInMemoryCache() {
    this.widgetsInfoInMemoryCache.clear();
  }

  public getWidgetInfoFromCache(fullFqn: string): WidgetInfo | undefined {
    return this.widgetsInfoInMemoryCache.get(fullFqn);
  }

  public putWidgetInfoToCache(widgetInfo: WidgetInfo) {
    this.widgetsInfoInMemoryCache.set(widgetInfo.fullFqn, widgetInfo);
  }

  private widgetTypeUpdated(updatedWidgetType: BaseWidgetType): void {
    this.deleteWidgetInfoFromCache(fullWidgetTypeFqn(updatedWidgetType));
  }

  public deleteWidgetInfoFromCache(fullFqn: string) {
    this.widgetsInfoInMemoryCache.delete(fullFqn);
  }

  private loadWidgetsBundleCache(config?: RequestConfig): Observable<any> {
    if (!this.allWidgetsBundles) {
      if (this.widgetsBundleCacheSubject) {
        return this.widgetsBundleCacheSubject.asObservable();
      } else {
        const loadWidgetsBundleCacheSubject = new ReplaySubject<void>();
        this.widgetsBundleCacheSubject = loadWidgetsBundleCacheSubject;
        if (this.userPermissionsService.hasGenericPermission(Resource.WIDGETS_BUNDLE, Operation.READ)) {
          this.http.get<Array<WidgetsBundle>>('/api/widgetsBundles',
            defaultHttpOptionsFromConfig(config)).subscribe(
            (allWidgetsBundles) => {
              this.allWidgetsBundles = allWidgetsBundles;
              this.systemWidgetsBundles = new Array<WidgetsBundle>();
              this.tenantWidgetsBundles = new Array<WidgetsBundle>();
              this.allWidgetsBundles = this.allWidgetsBundles.sort((wb1, wb2) => {
                let res = wb1.title.localeCompare(wb2.title);
                if (res === 0) {
                  res = wb2.createdTime - wb1.createdTime;
                }
                return res;
              });
              this.allWidgetsBundles.forEach((widgetsBundle) => {
                if (widgetsBundle.tenantId.id === NULL_UUID) {
                  this.systemWidgetsBundles.push(widgetsBundle);
                } else {
                  this.tenantWidgetsBundles.push(widgetsBundle);
                }
              });
              loadWidgetsBundleCacheSubject.next();
              loadWidgetsBundleCacheSubject.complete();
              this.widgetsBundleCacheSubject = null;
            },
            () => {
              loadWidgetsBundleCacheSubject.error(null);
              this.widgetsBundleCacheSubject = null;
            });
        } else {
          this.allWidgetsBundles = [];
          this.systemWidgetsBundles = [];
          this.tenantWidgetsBundles = [];
          loadWidgetsBundleCacheSubject.next();
          loadWidgetsBundleCacheSubject.complete();
          this.widgetsBundleCacheSubject = null;
        }
        return loadWidgetsBundleCacheSubject.asObservable();
      }
    } else {
      return of(null);
    }
  }

  private invalidateWidgetsBundleCache() {
    this.allWidgetsBundles = undefined;
    this.systemWidgetsBundles = undefined;
    this.tenantWidgetsBundles = undefined;
    this.widgetsBundleCacheSubject = undefined;
  }
}
