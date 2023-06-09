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

import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { ActivatedRoute } from '@angular/router';
import { ReCaptcha2Component, ReCaptchaV3Service } from 'ngx-captcha';
import { MobileService } from '@core/services/mobile.service';
import { SelfRegistrationService } from '@app/core/http/self-register.service';
import { from } from 'rxjs';
import { ActionNotificationShow } from '@core/notification/notification.actions';

@Component({
  selector: 'tb-recaptcha',
  templateUrl: './tb-recaptcha.component.html',
  styleUrls: ['./tb-recaptcha.component.scss']
})
export class TbRecaptchaComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('recaptcha') recaptchaComponent: ReCaptcha2Component;

  signupParams = this.selfRegistrationService.signUpParams;

  recaptchaResponse: string;

  isMobileApp = this.mobileService.isMobileApp();

  constructor(protected store: Store<AppState>,
              private activatedRoute: ActivatedRoute,
              private selfRegistrationService: SelfRegistrationService,
              private reCaptchaV3Service: ReCaptchaV3Service,
              private mobileService: MobileService) {
    super(store);
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
    if (this.signupParams?.activate && this.signupParams?.captchaVersion === 'v3') {
      from(this.reCaptchaV3Service.executeAsPromise(this.signupParams?.captchaSiteKey,
        this.signupParams?.captchaAction, {useGlobalDomain: true})).subscribe(
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
