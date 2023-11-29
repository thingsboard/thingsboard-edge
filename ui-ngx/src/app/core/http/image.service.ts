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
import { Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import {
  NO_IMAGE_DATA_URI,
  ImageResourceInfo,
  imageResourceType,
  ImageResourceType,
  IMAGES_URL_PREFIX, isImageResourceUrl, ImageExportData, removeTbImagePrefix
} from '@shared/models/resource.models';
import { catchError, map, switchMap } from 'rxjs/operators';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { blobToBase64 } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class ImageService {
  constructor(
    private http: HttpClient,
    private sanitizer: DomSanitizer
  ) {
  }

  public uploadImage(file: File, title: string, config?: RequestConfig): Observable<ImageResourceInfo> {
    if (!config) {
      config = {};
    }
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    return this.http.post<ImageResourceInfo>('/api/image', formData,
      defaultHttpUploadOptions(config.ignoreLoading, config.ignoreErrors, config.resendRequest));
  }

  public updateImage(type: ImageResourceType, key: string, file: File, config?: RequestConfig): Observable<ImageResourceInfo> {
    if (!config) {
      config = {};
    }
    const formData = new FormData();
    formData.append('file', file);
    return this.http.put<ImageResourceInfo>(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}`, formData,
      defaultHttpUploadOptions(config.ignoreLoading, config.ignoreErrors, config.resendRequest));
  }

  public updateImageInfo(imageInfo: ImageResourceInfo, config?: RequestConfig): Observable<ImageResourceInfo> {
    const type = imageResourceType(imageInfo);
    const key = encodeURIComponent(imageInfo.resourceKey);
    return this.http.put<ImageResourceInfo>(`${IMAGES_URL_PREFIX}/${type}/${key}/info`,
      imageInfo, defaultHttpOptionsFromConfig(config));
  }

  public getImages(pageLink: PageLink, includeSystemImages = false, config?: RequestConfig): Observable<PageData<ImageResourceInfo>> {
    return this.http.get<PageData<ImageResourceInfo>>(
      `${IMAGES_URL_PREFIX}${pageLink.toQuery()}&includeSystemImages=${includeSystemImages}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getImageInfo(type: ImageResourceType, key: string, config?: RequestConfig): Observable<ImageResourceInfo> {
    return this.http.get<ImageResourceInfo>(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}/info`,
      defaultHttpOptionsFromConfig(config));
  }

  public getImageDataUrl(imageUrl: string, preview = false, asString = false, emptyUrl = NO_IMAGE_DATA_URI): Observable<SafeUrl | string> {
    const parts = imageUrl.split('/');
    const key = parts[parts.length - 1];
    parts[parts.length - 1] = encodeURIComponent(key);
    const encodedUrl = parts.join('/');
    const imageLink = preview ? (encodedUrl + '/preview') : encodedUrl;
    const options = defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true});
    return this.http
    .get(imageLink, {...options, ...{ responseType: 'blob' } }).pipe(
      switchMap(val => blobToBase64(val).pipe(
          map((dataUrl) => asString ? dataUrl : this.sanitizer.bypassSecurityTrustUrl(dataUrl))
        )),
      catchError(() => of(asString ? emptyUrl : this.sanitizer.bypassSecurityTrustUrl(emptyUrl)))
    );
  }

  public resolveImageUrl(imageUrl: string, preview = false, asString = false, emptyUrl = NO_IMAGE_DATA_URI): Observable<SafeUrl | string> {
    imageUrl = removeTbImagePrefix(imageUrl);
    if (isImageResourceUrl(imageUrl)) {
      return this.getImageDataUrl(imageUrl, preview, asString, emptyUrl);
    } else {
      return of(asString ? imageUrl : this.sanitizer.bypassSecurityTrustUrl(imageUrl));
    }
  }

  public downloadImage(type: ImageResourceType, key: string): Observable<any> {
    return this.http.get(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}`, {
      responseType: 'arraybuffer',
      observe: 'response'
    }).pipe(
      map((response) => {
        const headers = response.headers;
        const filename = headers.get('x-filename');
        const contentType = headers.get('content-type');
        const linkElement = document.createElement('a');
        try {
          const blob = new Blob([response.body], {type: contentType});
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

  public deleteImage(type: ImageResourceType, key: string, force = false, config?: RequestConfig) {
    return this.http.delete(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}?force=${force}`, defaultHttpOptionsFromConfig(config));
  }

  public exportImage(type: ImageResourceType, key: string, config?: RequestConfig): Observable<ImageExportData> {
    return this.http.get<ImageExportData>(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}/export`,
      defaultHttpOptionsFromConfig(config));
  }

  public importImage(imageData: ImageExportData, config?: RequestConfig): Observable<ImageResourceInfo> {
    return this.http.put<ImageResourceInfo>('/api/image/import',
      imageData, defaultHttpOptionsFromConfig(config));
  }

}
