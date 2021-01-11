///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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
