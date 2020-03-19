///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, ActivatedRouteSnapshot } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AuthService } from '@core/auth/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-iframe-view',
  templateUrl: './iframe-view.component.html'
})
export class IFrameViewComponent implements OnInit, OnDestroy {

  @HostBinding('style.width') public width = '100%';
  @HostBinding('style.height') public height = '100%';

  safeIframeUrl: SafeResourceUrl;

  private sub: Subscription;

  constructor(private sanitizer: DomSanitizer,
              private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.sub = this.route.queryParams.subscribe((queryParams) => {
      let iframeUrl: string;
      let setAccessToken: string;
      if (queryParams.childIframeUrl) {
        iframeUrl = queryParams.childIframeUrl;
        setAccessToken = queryParams.childSetAccessToken;
      } else {
        iframeUrl = queryParams.iframeUrl;
        setAccessToken = queryParams.setAccessToken;
      }
      if (setAccessToken === 'true') {
        const accessToken = AuthService.getJwtToken();
        if (iframeUrl.indexOf('?') > -1) {
          iframeUrl += '&';
        } else {
          iframeUrl += '?';
        }
        iframeUrl += `accessToken=${accessToken}`;
      }
      this.safeIframeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(iframeUrl);
    });
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

}
