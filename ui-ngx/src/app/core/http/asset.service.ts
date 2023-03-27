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
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntitySubtype } from '@app/shared/models/entity-type.models';
import { Asset, AssetInfo, AssetSearchQuery } from '@app/shared/models/asset.models';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { BulkImportRequest, BulkImportResult } from '@home/components/import-export/import-export.models';

@Injectable({
  providedIn: 'root'
})
export class AssetService {

  constructor(
    private http: HttpClient
  ) { }

/*  public getTenantAssetInfos(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<AssetInfo>> {
    return this.http.get<PageData<AssetInfo>>(`/api/tenant/assetInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantAssetInfosByAssetProfileId(pageLink: PageLink, assetProfileId: string = '',
                                             config?: RequestConfig): Observable<PageData<AssetInfo>> {
    return this.http.get<PageData<AssetInfo>>(`/api/tenant/assetInfos${pageLink.toQuery()}&assetProfileId=${assetProfileId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerAssetInfos(customerId: string, pageLink: PageLink, type: string = '',
                               config?: RequestConfig): Observable<PageData<AssetInfo>> {
    return this.http.get<PageData<AssetInfo>>(`/api/customer/${customerId}/assetInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }*/

  public getTenantAssets(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<Asset>> {
    return this.http.get<PageData<Asset>>(`/api/tenant/assets${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerAssets(customerId: string, pageLink: PageLink, type: string = '',
                           config?: RequestConfig): Observable<PageData<Asset>> {
    return this.http.get<PageData<Asset>>(`/api/customer/${customerId}/assets${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

/*  public getCustomerAssetInfosByAssetProfileId(customerId: string, pageLink: PageLink, assetProfileId: string = '',
                                               config?: RequestConfig): Observable<PageData<AssetInfo>> {
    return this.http.get<PageData<AssetInfo>>
    (`/api/customer/${customerId}/assetInfos${pageLink.toQuery()}&assetProfileId=${assetProfileId}`,
      defaultHttpOptionsFromConfig(config));
  } */

  public getAsset(assetId: string, config?: RequestConfig): Observable<Asset> {
    return this.http.get<Asset>(`/api/asset/${assetId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAssets(assetIds: Array<string>, config?: RequestConfig): Observable<Array<Asset>> {
    return this.http.get<Array<Asset>>(`/api/assets?assetIds=${assetIds.join(',')}`, defaultHttpOptionsFromConfig(config)).pipe(
      map((assets) => sortEntitiesByIds(assets, assetIds))
    );
  }

  public getUserAssets(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<Asset>> {
    return this.http.get<PageData<Asset>>(`/api/user/assets${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllAssetInfos(includeCustomers: boolean,
                          pageLink: PageLink, assetProfileId: string = '', config?: RequestConfig): Observable<PageData<AssetInfo>> {
    let url = `/api/assetInfos/all${pageLink.toQuery()}&assetProfileId=${assetProfileId}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<AssetInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerAssetInfos(includeCustomers: boolean, customerId: string,
                               pageLink: PageLink, assetProfileId: string = '',
                               config?: RequestConfig): Observable<PageData<AssetInfo>> {
    let url = `/api/customer/${customerId}/assetInfos${pageLink.toQuery()}&assetProfileId=${assetProfileId}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<AssetInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

/*  public getAssetInfo(assetId: string, config?: RequestConfig): Observable<AssetInfo> {
    return this.http.get<AssetInfo>(`/api/asset/info/${assetId}`, defaultHttpOptionsFromConfig(config));
  }*/

  public saveAsset(asset: Asset, entityGroupId?: string, config?: RequestConfig): Observable<Asset> {
    let url = '/api/asset';
    if (entityGroupId) {
      url += `?entityGroupId=${entityGroupId}`;
    }
    return this.http.post<Asset>(url, asset, defaultHttpOptionsFromConfig(config));
  }

  public deleteAsset(assetId: string, config?: RequestConfig) {
    return this.http.delete(`/api/asset/${assetId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAssetTypes(config?: RequestConfig): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/asset/types', defaultHttpOptionsFromConfig(config));
  }

/*  public makeAssetPublic(assetId: string, config?: RequestConfig): Observable<Asset> {
    return this.http.post<Asset>(`/api/customer/public/asset/${assetId}`, null, defaultHttpOptionsFromConfig(config));
  }

  public assignAssetToCustomer(customerId: string, assetId: string,
                               config?: RequestConfig): Observable<Asset> {
    return this.http.post<Asset>(`/api/customer/${customerId}/asset/${assetId}`, null, defaultHttpOptionsFromConfig(config));
  }

  public unassignAssetFromCustomer(assetId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/asset/${assetId}`, defaultHttpOptionsFromConfig(config));
  }*/

  public findByQuery(query: AssetSearchQuery,
                     config?: RequestConfig): Observable<Array<Asset>> {
    return this.http.post<Array<Asset>>('/api/assets', query, defaultHttpOptionsFromConfig(config));
  }

  public findByName(assetName: string, config?: RequestConfig): Observable<Asset> {
    return this.http.get<Asset>(`/api/tenant/assets?assetName=${assetName}`, defaultHttpOptionsFromConfig(config));
  }

  public bulkImportAssets(entitiesData: BulkImportRequest, config?: RequestConfig): Observable<BulkImportResult> {
    return this.http.post<BulkImportResult>('/api/asset/bulk_import', entitiesData, defaultHttpOptionsFromConfig(config));
  }
}
