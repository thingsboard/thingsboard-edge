///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
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
