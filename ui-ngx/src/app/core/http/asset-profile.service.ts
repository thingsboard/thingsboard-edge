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
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { AssetProfile, AssetProfileInfo } from '@shared/models/asset.models';
import { DeviceProfileInfo } from '@shared/models/device.models';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';

@Injectable({
  providedIn: 'root'
})
export class AssetProfileService {

  constructor(
    private http: HttpClient
  ) {
  }

  public getAssetProfiles(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AssetProfile>> {
    return this.http.get<PageData<AssetProfile>>(`/api/assetProfiles${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getAssetProfile(assetProfileId: string, config?: RequestConfig): Observable<AssetProfile> {
    return this.http.get<AssetProfile>(`/api/assetProfile/${assetProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveAssetProfile(assetProfile: AssetProfile, config?: RequestConfig): Observable<AssetProfile> {
    return this.http.post<AssetProfile>('/api/assetProfile', assetProfile, defaultHttpOptionsFromConfig(config));
  }

  public deleteAssetProfile(assetProfileId: string, config?: RequestConfig) {
    return this.http.delete(`/api/assetProfile/${assetProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public setDefaultAssetProfile(assetProfileId: string, config?: RequestConfig): Observable<AssetProfile> {
    return this.http.post<AssetProfile>(`/api/assetProfile/${assetProfileId}/default`, defaultHttpOptionsFromConfig(config));
  }

  public getDefaultAssetProfileInfo(config?: RequestConfig): Observable<AssetProfileInfo> {
    return this.http.get<AssetProfileInfo>('/api/assetProfileInfo/default', defaultHttpOptionsFromConfig(config));
  }

  public getAssetProfileInfo(assetProfileId: string, config?: RequestConfig): Observable<AssetProfileInfo> {
    return this.http.get<AssetProfileInfo>(`/api/assetProfileInfo/${assetProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAssetProfileInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AssetProfileInfo>> {
    return this.http.get<PageData<AssetProfileInfo>>(`/api/assetProfileInfos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getAssetProfilesByIds(assetProfileIds: Array<string>, config?: RequestConfig): Observable<Array<AssetProfileInfo>> {
    return this.http.get<Array<AssetProfileInfo>>(`/api/assetProfileInfos?assetProfileIds=${assetProfileIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((assetProfiles) => sortEntitiesByIds(assetProfiles, assetProfileIds))
    );
  }

}
