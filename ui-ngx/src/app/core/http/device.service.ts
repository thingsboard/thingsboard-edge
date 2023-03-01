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
import { Observable, ReplaySubject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  ClaimRequest,
  ClaimResult,
  Device,
  DeviceCredentials,
  DeviceSearchQuery
} from '@app/shared/models/device.models';
import { EntitySubtype } from '@app/shared/models/entity-type.models';
import { AuthService } from '@core/auth/auth.service';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { BulkImportRequest, BulkImportResult } from '@home/components/import-export/import-export.models';
import { PersistentRpc, RpcStatus } from '@shared/models/rpc.models';

@Injectable({
  providedIn: 'root'
})
export class DeviceService {

  constructor(
    private http: HttpClient
  ) { }

/*  public getTenantDeviceInfos(pageLink: PageLink, type: string = '',
                              config?: RequestConfig): Observable<PageData<DeviceInfo>> {
    return this.http.get<PageData<DeviceInfo>>(`/api/tenant/deviceInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantDeviceInfosByDeviceProfileId(pageLink: PageLink, deviceProfileId: string = '',
                                               config?: RequestConfig): Observable<PageData<DeviceInfo>> {
    return this.http.get<PageData<DeviceInfo>>(`/api/tenant/deviceInfos${pageLink.toQuery()}&deviceProfileId=${deviceProfileId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerDeviceInfos(customerId: string, pageLink: PageLink, type: string = '',
                                config?: RequestConfig): Observable<PageData<DeviceInfo>> {
    return this.http.get<PageData<DeviceInfo>>(`/api/customer/${customerId}/deviceInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }*/

  public getTenantDevices(pageLink: PageLink, type: string = '',
                          config?: RequestConfig): Observable<PageData<Device>> {
    return this.http.get<PageData<Device>>(`/api/tenant/devices${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerDevices(customerId: string, pageLink: PageLink, type: string = '',
                                config?: RequestConfig): Observable<PageData<Device>> {
    return this.http.get<PageData<Device>>(`/api/customer/${customerId}/devices${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

/*  public getCustomerDeviceInfosByDeviceProfileId(customerId: string, pageLink: PageLink, deviceProfileId: string = '',
                                                 config?: RequestConfig): Observable<PageData<DeviceInfo>> {
    return this.http.get<PageData<DeviceInfo>>(`/api/customer/${customerId}/deviceInfos${pageLink.toQuery()}&deviceProfileId=${deviceProfileId}`,
      defaultHttpOptionsFromConfig(config));
  } */

  public getDevice(deviceId: string, config?: RequestConfig): Observable<Device> {
    return this.http.get<Device>(`/api/device/${deviceId}`, defaultHttpOptionsFromConfig(config));
  }

  public getDevices(deviceIds: Array<string>, config?: RequestConfig): Observable<Array<Device>> {
    return this.http.get<Array<Device>>(`/api/devices?deviceIds=${deviceIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((devices) => sortEntitiesByIds(devices, deviceIds))
    );
  }

  public getUserDevices(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<Device>> {
    return this.http.get<PageData<Device>>(`/api/user/devices${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

/*  public getDeviceInfo(deviceId: string, config?: RequestConfig): Observable<DeviceInfo> {
    return this.http.get<DeviceInfo>(`/api/device/info/${deviceId}`, defaultHttpOptionsFromConfig(config));
  }*/

  public saveDevice(device: Device, entityGroupId?: string, config?: RequestConfig): Observable<Device> {
    let url = '/api/device';
    if (entityGroupId) {
      url += `?entityGroupId=${entityGroupId}`;
    }
    return this.http.post<Device>(url, device, defaultHttpOptionsFromConfig(config));
  }

  public deleteDevice(deviceId: string, config?: RequestConfig) {
    return this.http.delete(`/api/device/${deviceId}`, defaultHttpOptionsFromConfig(config));
  }

  public getDeviceTypes(config?: RequestConfig): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/device/types', defaultHttpOptionsFromConfig(config));
  }

  public getDeviceCredentials(deviceId: string, sync: boolean = false, config?: RequestConfig): Observable<DeviceCredentials> {
    const url = `/api/device/${deviceId}/credentials`;
    if (sync) {
      const responseSubject = new ReplaySubject<DeviceCredentials>();
      const request = new XMLHttpRequest();
      request.open('GET', url, false);
      request.setRequestHeader('Accept', 'application/json, text/plain, */*');
      const jwtToken = AuthService.getJwtToken();
      if (jwtToken) {
        request.setRequestHeader('X-Authorization', 'Bearer ' + jwtToken);
      }
      request.send(null);
      if (request.status === 200) {
        const credentials = JSON.parse(request.responseText) as DeviceCredentials;
        responseSubject.next(credentials);
      } else {
        responseSubject.error(null);
      }
      return responseSubject.asObservable();
    } else {
      return this.http.get<DeviceCredentials>(url, defaultHttpOptionsFromConfig(config));
    }
  }

  public saveDeviceCredentials(deviceCredentials: DeviceCredentials, config?: RequestConfig): Observable<DeviceCredentials> {
    return this.http.post<DeviceCredentials>('/api/device/credentials', deviceCredentials, defaultHttpOptionsFromConfig(config));
  }

  /*public makeDevicePublic(deviceId: string, config?: RequestConfig): Observable<Device> {
    return this.http.post<Device>(`/api/customer/public/device/${deviceId}`, null, defaultHttpOptionsFromConfig(config));
  }

  public assignDeviceToCustomer(customerId: string, deviceId: string,
                                config?: RequestConfig): Observable<Device> {
    return this.http.post<Device>(`/api/customer/${customerId}/device/${deviceId}`, null, defaultHttpOptionsFromConfig(config));
  }

  public unassignDeviceFromCustomer(deviceId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/device/${deviceId}`, defaultHttpOptionsFromConfig(config));
  }*/

  public sendOneWayRpcCommand(deviceId: string, requestBody: any, config?: RequestConfig): Observable<any> {
    return this.http.post<Device>(`/api/rpc/oneway/${deviceId}`, requestBody, defaultHttpOptionsFromConfig(config));
  }

  public sendTwoWayRpcCommand(deviceId: string, requestBody: any, config?: RequestConfig): Observable<any> {
    return this.http.post<Device>(`/api/rpc/twoway/${deviceId}`, requestBody, defaultHttpOptionsFromConfig(config));
  }

  public getPersistedRpc(rpcId: string, fullResponse = false, config?: RequestConfig): Observable<PersistentRpc> {
    return this.http.get<PersistentRpc>(`/api/rpc/persistent/${rpcId}`, defaultHttpOptionsFromConfig(config));
  }

  public deletePersistedRpc(rpcId: string, config?: RequestConfig) {
    return this.http.delete<PersistentRpc>(`/api/rpc/persistent/${rpcId}`, defaultHttpOptionsFromConfig(config));
  }

  public getPersistedRpcRequests(deviceId: string, pageLink: PageLink,
                                 rpcStatus?: RpcStatus, config?: RequestConfig): Observable<PageData<PersistentRpc>> {
    let url = `/api/rpc/persistent/device/${deviceId}${pageLink.toQuery()}`;
    if (rpcStatus && rpcStatus.length) {
      url += `&rpcStatus=${rpcStatus}`;
    }
    return this.http.get<PageData<PersistentRpc>>(url, defaultHttpOptionsFromConfig(config));
  }

  public findByQuery(query: DeviceSearchQuery,
                     config?: RequestConfig): Observable<Array<Device>> {
    return this.http.post<Array<Device>>('/api/devices', query, defaultHttpOptionsFromConfig(config));
  }

  public findByName(deviceName: string, config?: RequestConfig): Observable<Device> {
    return this.http.get<Device>(`/api/tenant/devices?deviceName=${deviceName}`, defaultHttpOptionsFromConfig(config));
  }

  public claimDevice(deviceName: string, claimRequest: ClaimRequest,
                     config?: RequestConfig): Observable<ClaimResult> {
    return this.http.post<ClaimResult>(`/api/customer/device/${deviceName}/claim`, claimRequest, defaultHttpOptionsFromConfig(config));
  }

  public unclaimDevice(deviceName: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/device/${deviceName}/claim`, defaultHttpOptionsFromConfig(config));
  }

  public bulkImportDevices(entitiesData: BulkImportRequest, config?: RequestConfig): Observable<BulkImportResult> {
    return this.http.post<BulkImportResult>('/api/device/bulk_import', entitiesData, defaultHttpOptionsFromConfig(config));
  }
}
