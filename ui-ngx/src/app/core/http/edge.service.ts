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
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink, TimePageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntitySubtype } from '@app/shared/models/entity-type.models';
import { Edge, EdgeEvent, EdgeInstallInstructions, EdgeSearchQuery } from '@shared/models/edge.models';
import { EntityId } from '@shared/models/id/entity-id';
import { BulkImportRequest, BulkImportResult } from '@home/components/import-export/import-export.models';

@Injectable({
  providedIn: 'root'
})
export class EdgeService {

  constructor(
    private http: HttpClient
  ) { }

  public getEdges(edgeIds: Array<string>, config?: RequestConfig): Observable<Array<Edge>> {
    return this.http.get<Array<Edge>>(`/api/edges?edgeIds=${edgeIds.join(',')}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEdge(edgeId: string, config?: RequestConfig): Observable<Edge> {
    return this.http.get<Edge>(`/api/edge/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveEdge(edge: Edge, entityGroupId?: string, config?: RequestConfig): Observable<Edge> {
    let url = '/api/edge';
    if (entityGroupId) {
      url += `?entityGroupId=${entityGroupId}`;
    }
    return this.http.post<Edge>(url, edge, defaultHttpOptionsFromConfig(config));
  }

  public deleteEdge(edgeId: string, config?: RequestConfig) {
    return this.http.delete(`/api/edge/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeTypes(config?: RequestConfig): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/edge/types', defaultHttpOptionsFromConfig(config));
  }

  public getCustomerEdgeInfos(customerId: string, pageLink: PageLink, type: string = '',
                               config?: RequestConfig): Observable<PageData<Edge>> {
    return this.http.get<PageData<Edge>>(`/api/customer/${customerId}/edgeInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public assignEdgeToCustomer(customerId: string, edgeId: string,
                              config?: RequestConfig): Observable<Edge> {
    return this.http.post<Edge>(`/api/customer/${customerId}/edge/${edgeId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignEdgeFromCustomer(edgeId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/edge/${edgeId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public makeEdgePublic(edgeId: string, config?: RequestConfig): Observable<Edge> {
    return this.http.post<Edge>(`/api/customer/public/edge/${edgeId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantEdges(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<Edge>> {
    return this.http.get<PageData<Edge>>(`/api/tenant/edges${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantEdgeInfos(pageLink: PageLink, type: string = '',
                            config?: RequestConfig): Observable<PageData<Edge>> {
    return this.http.get<PageData<Edge>>(`/api/tenant/edgeInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getUserEdges(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<Edge>> {
    return this.http.get<PageData<Edge>>(`/api/user/edges${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByQuery(query: EdgeSearchQuery, config?: RequestConfig): Observable<Array<Edge>> {
    return this.http.post<Array<Edge>>('/api/edges', query,
      defaultHttpOptionsFromConfig(config));
  }

  public getEdgeEvents(entityId: EntityId, pageLink: TimePageLink,
                       config?: RequestConfig): Observable<PageData<EdgeEvent>> {
    return this.http.get<PageData<EdgeEvent>>(`/api/edge/${entityId.id}/events` + `${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public syncEdge(edgeId: string, config?: RequestConfig) {
    return this.http.post(`/api/edge/sync/${edgeId}`, edgeId, defaultHttpOptionsFromConfig(config));
  }

  public findMissingToRelatedRuleChains(edgeId: string, config?: RequestConfig): Observable<string> {
    return this.http.get<string>(`/api/edge/missingToRelatedRuleChains/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public findByName(edgeName: string, config?: RequestConfig): Observable<Edge> {
    return this.http.get<Edge>(`/api/tenant/edges?edgeName=${edgeName}`, defaultHttpOptionsFromConfig(config));
  }

  public bulkImportEdges(entitiesData: BulkImportRequest, config?: RequestConfig): Observable<BulkImportResult> {
    return this.http.post<BulkImportResult>('/api/edge/bulk_import', entitiesData, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeDockerInstallInstructions(edgeId: string, config?: RequestConfig): Observable<EdgeInstallInstructions> {
    return this.http.get<EdgeInstallInstructions>(`/api/edge/instructions/${edgeId}`, defaultHttpOptionsFromConfig(config));
  }

  public findAllRelatedEdgesMissingAttributes(integrationId: string, config?: RequestConfig): Observable<string> {
    const url = `/api/edge/integration/${integrationId}/allMissingAttributes`;
    return this.http.get<string>(url, defaultHttpOptionsFromConfig(config));
  }

  public findEdgeMissingAttributes(integrationIds: Array<string>, edgeId: string, config?: RequestConfig): Observable<string> {
    const url = `/api/edge/integration/${edgeId}/missingAttributes?integrationIds=${integrationIds.join(',')}`;
    return this.http.get<string>(url, defaultHttpOptionsFromConfig(config));
  }
}
