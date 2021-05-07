///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import { defaultHttpOptionsFromConfig, defaultHttpUploadOptions, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { Firmware, FirmwareInfo, FirmwareType } from '@shared/models/firmware.models';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { deepClone, isDefinedAndNotNull } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class FirmwareService {
  constructor(
    private http: HttpClient
  ) {

  }

  public getFirmwares(pageLink: PageLink, config?: RequestConfig): Observable<PageData<FirmwareInfo>> {
    return this.http.get<PageData<FirmwareInfo>>(`/api/firmwares${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getFirmwaresInfoByDeviceProfileId(pageLink: PageLink, deviceProfileId: string, type: FirmwareType,
                                           hasData = true, config?: RequestConfig): Observable<PageData<FirmwareInfo>> {
    const url = `/api/firmwares/${deviceProfileId}/${type}/${hasData}${pageLink.toQuery()}`;
    return this.http.get<PageData<FirmwareInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getFirmware(firmwareId: string, config?: RequestConfig): Observable<Firmware> {
    return this.http.get<Firmware>(`/api/firmware/${firmwareId}`, defaultHttpOptionsFromConfig(config));
  }

  public getFirmwareInfo(firmwareId: string, config?: RequestConfig): Observable<FirmwareInfo> {
    return this.http.get<FirmwareInfo>(`/api/firmware/info/${firmwareId}`, defaultHttpOptionsFromConfig(config));
  }

  public downloadFirmware(firmwareId: string): Observable<any> {
    return this.http.get(`/api/firmware/${firmwareId}/download`, { responseType: 'arraybuffer', observe: 'response' }).pipe(
      map((response) => {
        const headers = response.headers;
        const filename = headers.get('x-filename');
        const contentType = headers.get('content-type');
        const linkElement = document.createElement('a');
        try {
          const blob = new Blob([response.body], { type: contentType });
          const url = URL.createObjectURL(blob);
          linkElement.setAttribute('href', url);
          linkElement.setAttribute('download', filename);
          const clickEvent = new MouseEvent('click',
            {
              view: window,
              bubbles: true,
              cancelable: false
            }
          );
          linkElement.dispatchEvent(clickEvent);
          return null;
        } catch (e) {
          throw e;
        }
      })
    );
  }

  public saveFirmware(firmware: Firmware, config?: RequestConfig): Observable<Firmware> {
    if (!firmware.file) {
      return this.saveFirmwareInfo(firmware, config);
    }
    const firmwareInfo = deepClone(firmware);
    delete firmwareInfo.file;
    delete firmwareInfo.checksum;
    delete firmwareInfo.checksumAlgorithm;
    return this.saveFirmwareInfo(firmwareInfo, config).pipe(
      mergeMap(res => {
        return this.uploadFirmwareFile(res.id.id, firmware.file, firmware.checksumAlgorithm, firmware.checksum).pipe(
          catchError(() => this.deleteFirmware(res.id.id))
        );
      })
    );
  }

  public saveFirmwareInfo(firmware: FirmwareInfo, config?: RequestConfig): Observable<Firmware> {
    return this.http.post<Firmware>('/api/firmware', firmware, defaultHttpOptionsFromConfig(config));
  }

  public uploadFirmwareFile(firmwareId: string, file: File, checksumAlgorithm?: string,
                            checksum?: string, config?: RequestConfig): Observable<any> {
    if (!config) {
      config = {};
    }
    const formData = new FormData();
    formData.append('file', file);
    let url = `/api/firmware/${firmwareId}`;
    if (checksumAlgorithm && checksum) {
      url += `?checksumAlgorithm=${checksumAlgorithm}&checksum=${checksum}`;
    }
    return this.http.post(url, formData,
      defaultHttpUploadOptions(config.ignoreLoading, config.ignoreErrors, config.resendRequest));
  }

  public deleteFirmware(firmwareId: string, config?: RequestConfig) {
    return this.http.delete(`/api/firmware/${firmwareId}`, defaultHttpOptionsFromConfig(config));
  }

}
