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

import { Pipe, PipeTransform } from '@angular/core';
import { ImageService } from '@core/http/image.service';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { AsyncSubject, BehaviorSubject, Observable } from 'rxjs';
import { isDefinedAndNotNull } from '@core/utils';
import { NO_IMAGE_DATA_URI } from '@shared/models/resource.models';

const LOADING_IMAGE_DATA_URI = 'data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRG' +
                                      'LTgiPz4KPHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHZlcnNpb249IjEuMSIgdmlld0JveD0iMCAw' +
                                      'IDIwIDIwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjwvc3ZnPgo=';

export interface UrlHolder {
  url?: string;
}

@Pipe({
  name: 'image'
})
export class ImagePipe implements PipeTransform {

  constructor(private imageService: ImageService,
              private sanitizer: DomSanitizer) { }

  transform(urlData: string | UrlHolder, args?: any): Observable<SafeUrl | string> {
    const ignoreLoadingImage = !!args?.ignoreLoadingImage;
    const asString = !!args?.asString;
    const emptyUrl = args?.emptyUrl || NO_IMAGE_DATA_URI;
    const image$ = ignoreLoadingImage
      ? new AsyncSubject<SafeUrl | string>()
      : new BehaviorSubject<SafeUrl | string>(LOADING_IMAGE_DATA_URI);
    const url = (typeof urlData === 'string') ? urlData : urlData?.url;
    if (isDefinedAndNotNull(url)) {
      const preview = !!args?.preview;
      const loginLogo = !!args?.loginLogo;
      const loginFavicon = !!args?.loginFavicon;
      let imageObservable: Observable<SafeUrl | string>;
      if (loginLogo || loginFavicon) {
        const faviconElseLogo = loginFavicon;
        imageObservable = this.imageService.resolveLoginImageUrl(url, faviconElseLogo, asString, emptyUrl);
      } else {
        imageObservable = this.imageService.resolveImageUrl(url, preview, asString, emptyUrl);
      }
      imageObservable.subscribe((imageUrl) => {
        Promise.resolve().then(() => {
          image$.next(imageUrl);
          image$.complete();
        });
      });
    } else {
      Promise.resolve().then(() => {
        image$.next(asString ? emptyUrl : this.sanitizer.bypassSecurityTrustUrl(emptyUrl));
        image$.complete();
      });
    }
    return image$.asObservable();
  }

}
