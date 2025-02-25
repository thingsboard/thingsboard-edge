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
import { defaultHttpOptionsFromConfig, defaultHttpUploadOptions, RequestConfig } from '@core/http/http-utils';
import { Observable, of, ReplaySubject } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import {
  ImageExportData,
  ImageResourceInfo,
  ImageResourceType,
  imageResourceType,
  IMAGES_URL_PREFIX,
  isImageResourceUrl,
  NO_IMAGE_DATA_URI,
  removeTbImagePrefix,
  ResourceSubType
} from '@shared/models/resource.models';
import { catchError, map, switchMap } from 'rxjs/operators';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { blobToBase64, blobToText } from '@core/utils';
import { ResourcesService } from '@core/services/resources.service';

@Injectable({
  providedIn: 'root'
})
export class ImageService {

  private imagesLoading: { [url: string]: ReplaySubject<Blob> } = {};

  constructor(
    private http: HttpClient,
    private sanitizer: DomSanitizer,
    private resourcesService: ResourcesService
  ) {
  }

  public uploadImage(file: File, title: string, imageSubType: ResourceSubType = ResourceSubType.IMAGE,
                     config?: RequestConfig): Observable<ImageResourceInfo> {
    if (!config) {
      config = {};
    }
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    formData.append('imageSubType', imageSubType);
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

  public updateImagePublicStatus(imageInfo: ImageResourceInfo, isPublic: boolean, config?: RequestConfig): Observable<ImageResourceInfo> {
    const type = imageResourceType(imageInfo);
    const key = encodeURIComponent(imageInfo.resourceKey);
    return this.http.put<ImageResourceInfo>(`${IMAGES_URL_PREFIX}/${type}/${key}/public/${isPublic}`,
      imageInfo, defaultHttpOptionsFromConfig(config));
  }

  public getImages(pageLink: PageLink, includeSystemImages = false,
                   imageSubType: ResourceSubType = ResourceSubType.IMAGE, config?: RequestConfig): Observable<PageData<ImageResourceInfo>> {
    return this.http.get<PageData<ImageResourceInfo>>(
      `${IMAGES_URL_PREFIX}${pageLink.toQuery()}&imageSubType=${imageSubType}&includeSystemImages=${includeSystemImages}`,
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
    return this.loadImageDataUrl(imageLink, asString, emptyUrl);
  }

  public getLoginImageDataUrl(imageUrl: string, faviconElseLogo: boolean,
                              asString = false, emptyUrl = NO_IMAGE_DATA_URI): Observable<SafeUrl | string> {
    const parts = imageUrl.split('/');
    const type = parts[parts.length - 2];
    const key = encodeURIComponent(parts[parts.length - 1]);
    const imageLink = faviconElseLogo
      ? `/api/noauth/whiteLabel/loginFavicon/${type}/${key}`
      : `/api/noauth/whiteLabel/loginLogo/${type}/${key}`;
    return this.loadImageDataUrl(imageLink, asString, emptyUrl);
  }

  private loadImageDataUrl(imageLink: string, asString = false, emptyUrl = NO_IMAGE_DATA_URI): Observable<SafeUrl | string> {
    let request: ReplaySubject<Blob>;
    if (this.imagesLoading[imageLink]) {
      request = this.imagesLoading[imageLink];
    } else {
      request = new ReplaySubject<Blob>(1);
      this.imagesLoading[imageLink] = request;
      const options = defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true});
      this.http.get(imageLink, {...options, ...{ responseType: 'blob' } }).subscribe({
        next: (value) => {
          request.next(value);
          request.complete();
        },
        error: err => {
          request.error(err);
        },
        complete: () => {
          delete this.imagesLoading[imageLink];
        }
      });
    }
    return request.pipe(
      switchMap(val => blobToBase64(val).pipe(
        map((dataUrl) => asString ? dataUrl : this.sanitizer.bypassSecurityTrustUrl(dataUrl))
      )),
      catchError(() => of(asString ? emptyUrl : this.sanitizer.bypassSecurityTrustUrl(emptyUrl)))
    );
  }

  public getImageString(imageUrl: string): Observable<string> {
    imageUrl = removeTbImagePrefix(imageUrl);
    let request: ReplaySubject<Blob>;
    if (this.imagesLoading[imageUrl]) {
      request = this.imagesLoading[imageUrl];
    } else {
      request = new ReplaySubject<Blob>(1);
      this.imagesLoading[imageUrl] = request;
      const options = defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true});
      this.http.get(imageUrl, {...options, ...{ responseType: 'blob' } }).subscribe({
        next: (value) => {
          request.next(value);
          request.complete();
        },
        error: err => {
          request.error(err);
        },
        complete: () => {
          delete this.imagesLoading[imageUrl];
        }
      });
    }
    return request.pipe(
      switchMap(val => blobToText(val))
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

  public resolveLoginImageUrl(imageUrl: string, faviconElseLogo: boolean,
                              asString = false, emptyUrl = NO_IMAGE_DATA_URI): Observable<SafeUrl | string> {
    imageUrl = removeTbImagePrefix(imageUrl);
    if (isImageResourceUrl(imageUrl)) {
      return this.getLoginImageDataUrl(imageUrl, faviconElseLogo, asString, emptyUrl);
    } else {
      return of(asString ? imageUrl : this.sanitizer.bypassSecurityTrustUrl(imageUrl));
    }
  }

  public downloadImage(type: ImageResourceType, key: string): Observable<any> {
    return this.resourcesService.downloadResource(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}`);
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
