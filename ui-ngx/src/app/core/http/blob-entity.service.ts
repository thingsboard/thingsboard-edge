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

import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { BlobEntityInfo, BlobEntityWithCustomerInfo } from '@shared/models/blob-entity.models';
import { TimePageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { map } from 'rxjs/operators';
import { DOCUMENT } from '@angular/common';
import { WINDOW } from '@core/services/window.service';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { isDefinedAndNotNull } from '@core/utils';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class BlobEntityService {

  constructor(
    @Inject(WINDOW) private window: Window,
    @Inject(DOCUMENT) private document: Document,
    private http: HttpClient,
  ) {
  }

  public getBlobEntityInfo(blobEntityId: string, config?: RequestConfig): Observable<BlobEntityWithCustomerInfo> {
    return this.http.get<BlobEntityWithCustomerInfo>(`/api/blobEntity/info/${blobEntityId}`, defaultHttpOptionsFromConfig(config));
  }

  public getBlobEntities(pageLink: TimePageLink, type: string = '',
                         config?: RequestConfig): Observable<PageData<BlobEntityWithCustomerInfo>> {
    let url = `/api/blobEntities${pageLink.toQuery()}`;
    if (isDefinedAndNotNull(type)) {
      url += `&type=${type}`;
    }
    return this.http.get<PageData<BlobEntityWithCustomerInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getBlobEntitiesByIds(blobEntityIds: Array<string>, config?: RequestConfig): Observable<Array<BlobEntityInfo>> {
    return this.http.get<Array<BlobEntityInfo>>(`/api/blobEntities?blobEntityIds=${blobEntityIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
        map((blobEntities) => sortEntitiesByIds(blobEntities, blobEntityIds))
    );
  }

  public deleteBlobEntity(blobEntityId: string, config?: RequestConfig) {
    return this.http.delete(`/api/blobEntity/${blobEntityId}`, defaultHttpOptionsFromConfig(config));
  }

  public downloadBlobEntity(blobEntityId: string): Observable<any> {
    return this.http.get(`/api/blobEntity/${blobEntityId}/download`, { responseType: 'arraybuffer', observe: 'response' }).pipe(
      map((response) => {
        const headers = response.headers;
        const filename = headers.get('x-filename');
        const contentType = headers.get('content-type');
        const linkElement = this.document.createElement('a');
        try {
          const blob = new Blob([response.body], { type: contentType });
          const url = URL.createObjectURL(blob);
          linkElement.setAttribute('href', url);
          linkElement.setAttribute('download', filename);
          const clickEvent = new MouseEvent('click',
            {
              view: this.window,
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

}
