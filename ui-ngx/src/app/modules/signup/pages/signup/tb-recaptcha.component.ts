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

import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { ReCaptcha2Component, ReCaptchaV3Service } from 'ngx-captcha';
import { MobileService } from '@core/services/mobile.service';
import { SelfRegistrationService } from '@app/core/http/self-register.service';
import { from } from 'rxjs';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ActivatedRoute } from '@angular/router';
import { CaptchaParams, CaptchaVersion } from '@shared/models/self-register.models';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-recaptcha',
  templateUrl: './tb-recaptcha.component.html',
  styleUrls: ['./tb-recaptcha.component.scss']
})
export class TbRecaptchaComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('recaptcha') recaptchaComponent: ReCaptcha2Component;

  captcha: CaptchaParams;

  recaptchaResponse: string;

  activeCaptcha = false;

  private isMobileApp = this.mobileService.isMobileApp();

  constructor(private selfRegistrationService: SelfRegistrationService,
              private reCaptchaV3Service: ReCaptchaV3Service,
              private mobileService: MobileService,
              private route: ActivatedRoute) {
    super();

    if (this.route.snapshot.queryParamMap.has('version') && this.route.snapshot.queryParamMap.get('siteKey')) {
      let queryVersion = this.route.snapshot.queryParamMap.get('version');
      if (queryVersion !== 'v2' && queryVersion !== 'v3') {
        queryVersion = 'v3';
      }

      this.captcha = {
        version: queryVersion as CaptchaVersion,
        siteKey: this.route.snapshot.queryParamMap.get('siteKey'),
        logActionName: this.route.snapshot.queryParamMap.get('logActionName') ?? '',
      }

      this.activeCaptcha = isDefinedAndNotNull(this.captcha?.siteKey)
    } else {
      this.captcha = this.selfRegistrationService.signUpParams.captcha;
      this.activeCaptcha = this.selfRegistrationService.signUpParams.activate;
    }
  }

  ngOnInit() {
    if (this.isMobileApp) {
      this.mobileService.registerResetRecaptchaFunction(() => {
        setTimeout(() => {
          if (this.recaptchaComponent) {
            this.recaptchaComponent.resetCaptcha();
          }
        });
      });
      setTimeout(() => {
        this.mobileService.onRecaptchaLoaded();
      });
    }
    if (this.activeCaptcha && this.captcha.version === 'v3') {
      from(this.reCaptchaV3Service.executeAsPromise(this.captcha.siteKey,
        this.captcha.logActionName, {useGlobalDomain: true})).subscribe(
        {
          next: (token) => {
            this.mobileService.handleReCaptchaResponse(token);
          },
          error: err => {
            this.store.dispatch(new ActionNotificationShow({ message: 'ReCaptcha error: ' + err,
              type: 'error' }));
          }
        }
      );
    }
  }

  ngOnDestroy() {
    if (this.isMobileApp) {
      this.mobileService.unregisterResetRecaptchaFunction();
    }
    super.ngOnDestroy();
  }

  onRecaptchaResponse() {
    this.mobileService.handleReCaptchaResponse(this.recaptchaResponse);
  }

}
