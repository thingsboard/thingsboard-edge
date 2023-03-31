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
import { defaultHttpOptionsFromConfig, defaultHttpUploadOptions, RequestConfig } from '@core/http/http-utils';
import { forkJoin, Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import {
  ChecksumAlgorithm,
  DeviceGroupOtaPackage,
  OtaPackage,
  OtaPackageInfo,
  OtaPagesIds,
  OtaUpdateType
} from '@shared/models/ota-package.models';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { deepClone } from '@core/utils';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { EntityType } from '@shared/models/entity-type.models';

@Injectable({
  providedIn: 'root'
})
export class OtaPackageService {
  constructor(
    private http: HttpClient,
    private translate: TranslateService,
    private dialogService: DialogService
  ) {

  }

  public getOtaPackages(pageLink: PageLink, config?: RequestConfig): Observable<PageData<OtaPackageInfo>> {
    return this.http.get<PageData<OtaPackageInfo>>(`/api/otaPackages${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getOtaPackagesInfoByDeviceProfileId(pageLink: PageLink, deviceProfileId: string, type: OtaUpdateType,
                                             config?: RequestConfig): Observable<PageData<OtaPackageInfo>> {
    const url = `/api/otaPackages/${deviceProfileId}/${type}${pageLink.toQuery()}`;
    return this.http.get<PageData<OtaPackageInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getOtaPackage(otaPackageId: string, config?: RequestConfig): Observable<OtaPackage> {
    return this.http.get<OtaPackage>(`/api/otaPackages/${otaPackageId}`, defaultHttpOptionsFromConfig(config));
  }

  public getOtaPackageInfo(otaPackageId: string, config?: RequestConfig): Observable<OtaPackageInfo> {
    return this.http.get<OtaPackageInfo>(`/api/otaPackage/info/${otaPackageId}`, defaultHttpOptionsFromConfig(config));
  }

  public downloadOtaPackage(otaPackageId: string): Observable<any> {
    return this.http.get(`/api/otaPackage/${otaPackageId}/download`, { responseType: 'arraybuffer', observe: 'response' }).pipe(
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

  public saveOtaPackage(otaPackage: OtaPackage, config?: RequestConfig): Observable<OtaPackage> {
    if (!otaPackage.file) {
      return this.saveOtaPackageInfo(otaPackage, config);
    }
    const otaPackageInfo = deepClone(otaPackage);
    delete otaPackageInfo.file;
    delete otaPackageInfo.checksum;
    delete otaPackageInfo.checksumAlgorithm;
    return this.saveOtaPackageInfo(otaPackageInfo, config).pipe(
      mergeMap(res => {
        return this.uploadOtaPackageFile(res.id.id, otaPackage.file, otaPackage.checksumAlgorithm, otaPackage.checksum).pipe(
          catchError(() => this.deleteOtaPackage(res.id.id))
        );
      })
    );
  }

  public saveOtaPackageInfo(otaPackageInfo: OtaPackageInfo, config?: RequestConfig): Observable<OtaPackage> {
    return this.http.post<OtaPackage>('/api/otaPackage', otaPackageInfo, defaultHttpOptionsFromConfig(config));
  }

  public uploadOtaPackageFile(otaPackageId: string, file: File, checksumAlgorithm: ChecksumAlgorithm,
                              checksum?: string, config?: RequestConfig): Observable<any> {
    if (!config) {
      config = {};
    }
    const formData = new FormData();
    formData.append('file', file);
    let url = `/api/otaPackage/${otaPackageId}?checksumAlgorithm=${checksumAlgorithm}`;
    if (checksum) {
      url += `&checksum=${checksum}`;
    }
    return this.http.post(url, formData,
      defaultHttpUploadOptions(config.ignoreLoading, config.ignoreErrors, config.resendRequest));
  }

  public deleteOtaPackage(otaPackageId: string, config?: RequestConfig) {
    return this.http.delete(`/api/otaPackage/${otaPackageId}`, defaultHttpOptionsFromConfig(config));
  }

  public getOtaPackageInfoByDeviceGroupId(deviceGroupId: string, type: OtaUpdateType,
                                          config?: RequestConfig): Observable<DeviceGroupOtaPackage> {
    const url = `/api/deviceGroupOtaPackage/${deviceGroupId}/${type}`;
    return this.http.get<DeviceGroupOtaPackage>(url, defaultHttpOptionsFromConfig(config));
  }

  public getOtaPackagesInfoByDeviceGroupId(pageLink: PageLink, deviceGroupId: string, type: OtaUpdateType,
                                           config?: RequestConfig): Observable<PageData<OtaPackageInfo>> {
    const url = `/api/otaPackages/group/${deviceGroupId}/${type}${pageLink.toQuery()}`;
    return this.http.get<PageData<OtaPackageInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public countUpdateDeviceAfterChangePackage(type: OtaUpdateType, entityId: EntityId,
                                             packageId?: string, config?: RequestConfig): Observable<number> {
    let url;
    if (entityId.entityType === EntityType.ENTITY_GROUP) {
      url = `/api/devices/count/${type}/${packageId}/${entityId.id}`;
    } else {
      url = `/api/devices/count/${type}/${entityId.id}`;
    }
    return this.http.get<number>(url, defaultHttpOptionsFromConfig(config));
  }

  public confirmDialogUpdatePackage(entity: BaseData<EntityId>&OtaPagesIds,
                                    originEntity?: BaseData<EntityId>&OtaPagesIds): Observable<boolean> {
    const tasks: Observable<number>[] = [];
    if (originEntity?.id?.id && originEntity.firmwareId?.id !== entity.firmwareId?.id) {
      const packageId = entity.firmwareId?.id || originEntity.firmwareId?.id;
      tasks.push(this.countUpdateDeviceAfterChangePackage(OtaUpdateType.FIRMWARE, entity.id, packageId));
    } else {
      tasks.push(of(0));
    }
    if (originEntity?.id?.id && originEntity.softwareId?.id !== entity.softwareId?.id) {
      const packageId = entity.softwareId?.id || originEntity.softwareId?.id;
      tasks.push(this.countUpdateDeviceAfterChangePackage(OtaUpdateType.SOFTWARE, entity.id, packageId));
    } else {
      tasks.push(of(0));
    }
    return forkJoin(tasks).pipe(
      mergeMap(([deviceFirmwareUpdate, deviceSoftwareUpdate]) => {
        let text = '';
        if (deviceFirmwareUpdate > 0) {
          text += this.translate.instant('ota-update.change-firmware', {count: deviceFirmwareUpdate});
        }
        if (deviceSoftwareUpdate > 0) {
          text += text.length ? ' ' : '';
          text += this.translate.instant('ota-update.change-software', {count: deviceSoftwareUpdate});
        }
        return text !== '' ? this.dialogService.confirm('', text, null, this.translate.instant('common.proceed')) : of(true);
      })
    );
  }
}
