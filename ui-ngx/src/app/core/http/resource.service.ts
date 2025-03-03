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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { forkJoin, Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { Resource, ResourceInfo, ResourceSubType, ResourceType, TBResourceScope } from '@shared/models/resource.models';
import { catchError, mergeMap } from 'rxjs/operators';
import { isNotEmptyStr } from '@core/utils';
import { ResourcesService } from '@core/services/resources.service';

@Injectable({
  providedIn: 'root'
})
export class ResourceService {
  constructor(
    private http: HttpClient,
    private resourcesService: ResourcesService
  ) {

  }

  public getResources(pageLink: PageLink, resourceType?: ResourceType, resourceSubType?: ResourceSubType, config?: RequestConfig): Observable<PageData<ResourceInfo>> {
    let url = `/api/resource${pageLink.toQuery()}`;
    if (isNotEmptyStr(resourceType)) {
      url += `&resourceType=${resourceType}`;
    }
    if (isNotEmptyStr(resourceSubType)) {
      url += `&resourceSubType=${resourceSubType}`;
    }
    return this.http.get<PageData<ResourceInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getTenantResources(pageLink: PageLink, config?: RequestConfig): Observable<PageData<ResourceInfo>> {
    return this.http.get<PageData<ResourceInfo>>(`/api/resource/tenant${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getResource(resourceId: string, config?: RequestConfig): Observable<Resource> {
    return this.http.get<Resource>(`/api/resource/${resourceId}`, defaultHttpOptionsFromConfig(config));
  }

  public getResourceInfoById(resourceId: string, config?: RequestConfig): Observable<ResourceInfo> {
    return this.http.get<Resource>(`/api/resource/info/${resourceId}`, defaultHttpOptionsFromConfig(config));
  }

  public getResourceInfo(type: ResourceType, scope: TBResourceScope, key: string, config?: RequestConfig): Observable<ResourceInfo> {
    return this.http.get<Resource>(`/api/resource/${type}/${scope}/${key}/info`, defaultHttpOptionsFromConfig(config));
  }

  public downloadResource(resourceId: string): Observable<any> {
    return this.resourcesService.downloadResource(`/api/resource/${resourceId}/download`);
  }

  public saveResources(resources: Resource[], config?: RequestConfig): Observable<Resource[]> {
    let partSize = 100;
    partSize = resources.length > partSize ? partSize : resources.length;
    const resourceObservables: Observable<Resource>[] = [];
    for (let i = 0; i < partSize; i++) {
      resourceObservables.push(this.saveResource(resources[i], config).pipe(catchError(() => of({} as Resource))));
    }
    return forkJoin(resourceObservables).pipe(
      mergeMap((resource) => {
        resources.splice(0, partSize);
        if (resources.length) {
          return this.saveResources(resources, config);
        } else {
          return of(resource);
        }
      })
    );
  }

  public saveResource(resource: Resource, config?: RequestConfig): Observable<Resource> {
    return this.http.post<Resource>('/api/resource', resource, defaultHttpOptionsFromConfig(config));
  }

  public deleteResource(resourceId: string, force = false, config?: RequestConfig) {
    return this.http.delete(`/api/resource/${resourceId}?force=${force}`, defaultHttpOptionsFromConfig(config));
  }

}
