///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
  Widget,
  WidgetType,
  widgetType,
  WidgetTypeDetails,
  WidgetTypeInfo,
  widgetTypesData
} from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { ResourcesService } from '../services/resources.service';
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

  private widgetsBundleCacheSubject: ReplaySubject<any> = null;

  private widgetTypeInfosCache = new Map<string, Array<WidgetTypeInfo>>();

  private widgetsInfoInMemoryCache = new Map<string, WidgetInfo>();

  constructor(
    private http: HttpClient,
    private utils: UtilsService,
    private userPermissionsService: UserPermissionsService,
    private resources: ResourcesService,
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

  public getWidgetBundles(pageLink: PageLink, config?: RequestConfig): Observable<PageData<WidgetsBundle>> {
    return this.http.get<PageData<WidgetsBundle>>(`/api/widgetsBundles${pageLink.toQuery()}`,
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

  public deleteWidgetsBundle(widgetsBundleId: string, config?: RequestConfig) {
    return this.getWidgetsBundle(widgetsBundleId, config).pipe(
      mergeMap((widgetsBundle) => {
        return this.http.delete(`/api/widgetsBundle/${widgetsBundleId}`,
          defaultHttpOptionsFromConfig(config)).pipe(
          tap(() => {
            this.invalidateWidgetsBundleCache();
            this.widgetsBundleDeleted(widgetsBundle);
          })
        );
      }
    ));
  }

  public getBundleWidgetTypes(bundleAlias: string, isSystem: boolean,
                              config?: RequestConfig): Observable<Array<WidgetType>> {
    return this.http.get<Array<WidgetType>>(`/api/widgetTypes?isSystem=${isSystem}&bundleAlias=${bundleAlias}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getBundleWidgetTypesDetails(bundleAlias: string, isSystem: boolean,
                                     config?: RequestConfig): Observable<Array<WidgetTypeDetails>> {
    return this.http.get<Array<WidgetTypeDetails>>(`/api/widgetTypesDetails?isSystem=${isSystem}&bundleAlias=${bundleAlias}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getBundleWidgetTypeInfos(bundleAlias: string, isSystem: boolean,
                                  config?: RequestConfig): Observable<Array<WidgetTypeInfo>> {
    const key = bundleAlias + (isSystem ? '_sys' : '');
    if (this.widgetTypeInfosCache.has(key)) {
      return of(this.widgetTypeInfosCache.get(key));
    } else {
      return this.http.get<Array<WidgetTypeInfo>>(`/api/widgetTypesInfos?isSystem=${isSystem}&bundleAlias=${bundleAlias}`,
        defaultHttpOptionsFromConfig(config)).pipe(
          tap((res) => this.widgetTypeInfosCache.set(key, res) )
      );
    }
  }

  public loadBundleLibraryWidgets(bundleAlias: string, isSystem: boolean,
                                  config?: RequestConfig): Observable<Array<Widget>> {
    return this.getBundleWidgetTypes(bundleAlias, isSystem, config).pipe(
      map((types) => {
        types = types.sort((a, b) => {
          let result = widgetType[b.descriptor.type].localeCompare(widgetType[a.descriptor.type]);
          if (result === 0) {
            result = b.createdTime - a.createdTime;
          }
          return result;
        });
        const widgetTypes = new Array<Widget>();
        let top = 0;
        const lastTop = [0, 0, 0];
        let col = 0;
        let column = 0;
        types.forEach((type) => {
          const widgetTypeInfo = toWidgetInfo(type);
          const sizeX = 8;
          const sizeY = Math.floor(widgetTypeInfo.sizeY);
          const widget: Widget = {
            typeId: type.id,
            isSystemType: isSystem,
            bundleAlias,
            typeAlias: widgetTypeInfo.alias,
            type: widgetTypeInfo.type,
            title: widgetTypeInfo.widgetName,
            sizeX,
            sizeY,
            row: top,
            col,
            config: JSON.parse(widgetTypeInfo.defaultConfig)
          };

          widget.config.title = widgetTypeInfo.widgetName;

          widgetTypes.push(widget);
          top += sizeY;
          if (top > lastTop[column] + 10) {
            lastTop[column] = top;
            column++;
            if (column > 2) {
              column = 0;
            }
            top = lastTop[column];
            col = column * 8;
          }
        });
        return widgetTypes;
      })
    );
  }

  public getWidgetType(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean,
                       config?: RequestConfig): Observable<WidgetType> {
    return this.http.get<WidgetType>(`/api/widgetType?isSystem=${isSystem}&bundleAlias=${bundleAlias}&alias=${widgetTypeAlias}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveWidgetTypeDetails(widgetInfo: WidgetInfo,
                               id: WidgetTypeId,
                               bundleAlias: string,
                               createdTime: number,
                               config?: RequestConfig): Observable<WidgetTypeDetails> {
    const widgetTypeDetails = toWidgetTypeDetails(widgetInfo, id, undefined, bundleAlias, createdTime);
    return this.http.post<WidgetTypeDetails>('/api/widgetType', widgetTypeDetails,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap((savedWidgetType) => {
        this.widgetTypeUpdated(savedWidgetType);
      }));
  }

  public saveImportedWidgetTypeDetails(widgetTypeDetails: WidgetTypeDetails,
                                       config?: RequestConfig): Observable<WidgetTypeDetails> {
    return this.http.post<WidgetTypeDetails>('/api/widgetType', widgetTypeDetails,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap((savedWidgetType) => {
        this.widgetTypeUpdated(savedWidgetType);
      }));
  }

  public deleteWidgetType(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean,
                          config?: RequestConfig) {
    return this.getWidgetType(bundleAlias, widgetTypeAlias, isSystem, config).pipe(
      mergeMap((widgetTypeInstance) => {
          return this.http.delete(`/api/widgetType/${widgetTypeInstance.id.id}`,
            defaultHttpOptionsFromConfig(config)).pipe(
            tap(() => {
              this.widgetTypeUpdated(widgetTypeInstance);
            })
          );
        }
      ));
  }

  public getWidgetTypeById(widgetTypeId: string,
                           config?: RequestConfig): Observable<WidgetTypeDetails> {
    return this.http.get<WidgetTypeDetails>(`/api/widgetType/${widgetTypeId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getWidgetTemplate(widgetTypeParam: widgetType,
                           config?: RequestConfig): Observable<WidgetInfo> {
    const templateWidgetType = widgetTypesData.get(widgetTypeParam);
    return this.getWidgetType(templateWidgetType.template.bundleAlias, templateWidgetType.template.alias, true,
      config).pipe(
        map((result) => {
          const widgetInfo = toWidgetInfo(result);
          widgetInfo.alias = undefined;
          return widgetInfo;
        })
      );
  }

  public createWidgetInfoCacheKey(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean): string {
    return `${isSystem ? 'sys_' : ''}${bundleAlias}_${widgetTypeAlias}`;
  }

  public clearWidgetInfoInMemoryCache() {
    this.widgetsInfoInMemoryCache.clear();
  }

  public getWidgetInfoFromCache(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean): WidgetInfo | undefined {
    const key = this.createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
    return this.widgetsInfoInMemoryCache.get(key);
  }

  public putWidgetInfoToCache(widgetInfo: WidgetInfo, bundleAlias: string, widgetTypeAlias: string, isSystem: boolean) {
    const key = this.createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
    this.widgetsInfoInMemoryCache.set(key, widgetInfo);
  }

  private widgetTypeUpdated(updatedWidgetType: WidgetType): void {
    this.deleteWidgetInfoFromCache(updatedWidgetType.bundleAlias, updatedWidgetType.alias, updatedWidgetType.tenantId.id === NULL_UUID);
  }

  private widgetsBundleDeleted(widgetsBundle: WidgetsBundle): void {
    this.deleteWidgetsBundleFromCache(widgetsBundle.alias, widgetsBundle.tenantId.id === NULL_UUID);
  }

  private deleteWidgetInfoFromCache(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean) {
    const key = this.createWidgetInfoCacheKey(bundleAlias, widgetTypeAlias, isSystem);
    this.widgetsInfoInMemoryCache.delete(key);
  }

  private deleteWidgetsBundleFromCache(bundleAlias: string, isSystem: boolean) {
    const key = (isSystem ? 'sys_' : '') + bundleAlias;
    this.widgetsInfoInMemoryCache.forEach((widgetInfo, cacheKey) => {
      if (cacheKey.startsWith(key)) {
        this.widgetsInfoInMemoryCache.delete(cacheKey);
      }
    });
  }

  private loadWidgetsBundleCache(config?: RequestConfig): Observable<any> {
    if (!this.allWidgetsBundles) {
      if (this.widgetsBundleCacheSubject) {
        return this.widgetsBundleCacheSubject.asObservable();
      } else {
        const loadWidgetsBundleCacheSubject = new ReplaySubject();
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
    this.widgetTypeInfosCache.clear();
  }
}
