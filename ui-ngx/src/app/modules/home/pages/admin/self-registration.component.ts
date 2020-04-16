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

import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SecuritySettings, smtpPortPattern } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { SelfRegistrationService } from '@core/http/self-register.service';
import { SelfRegistrationParams } from '@shared/models/self-register.models';
import { deepClone, isDefined } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-self-registration',
  templateUrl: './self-registration.component.html',
  styleUrls: ['./self-registration.component.scss', './settings-card.scss']
})
export class SelfRegistrationComponent extends PageComponent implements OnInit, HasConfirmForm {

  selfRegistrationFormGroup: FormGroup;
  selfRegistrationParams: SelfRegistrationParams;
  registerLink: string;

  entityTypes = EntityType;

  tinyMceOptions: Record<string, any> = {
    base_url: '/tinymce',
    suffix: '.min',
    plugins: ['link table image imagetools code fullscreen'],
    menubar: 'edit insert tools view format table',
    toolbar: 'fontselect fontsizeselect | formatselect | bold italic  strikethrough  forecolor backcolor ' +
      '| link | table | image | alignleft aligncenter alignright alignjustify  ' +
      '| numlist bullist outdent indent  | removeformat | code | fullscreen',
    height: 380,
    autofocus: false,
    branding: false
  };

  constructor(protected store: Store<AppState>,
              private router: Router,
              private selfRegistrationService: SelfRegistrationService,
              private translate: TranslateService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildSelfRegistrationForm();
    this.selfRegistrationService.getSelfRegistrationParams().subscribe(
      (selfRegistrationParams) => {
        this.onSelfRegistrationParamsLoaded(selfRegistrationParams);
      }
    );
  }

  buildSelfRegistrationForm() {
    this.selfRegistrationFormGroup = this.fb.group({
      domainName: [null, [Validators.required, Validators.pattern('((?![:/]).)*$')]],
      captchaSiteKey: [null, [Validators.required]],
      captchaSecretKey: [null, [Validators.required]],
      notificationEmail: [null, [Validators.required, Validators.email]],
      signUpTextMessage: [null, [Validators.maxLength(200)]],
      permissions: [null],
      defaultDashboardId: [null],
      defaultDashboardFullscreen: [false],
      privacyPolicy: [null]
    });
    this.selfRegistrationFormGroup.get('defaultDashboardId').valueChanges.subscribe(
      () => {
        this.updateDisabledState();
      }
    );
  }

  private updateDisabledState() {
    const defaultDashboardId = this.selfRegistrationFormGroup.get('defaultDashboardId').value;
    if (defaultDashboardId) {
      this.selfRegistrationFormGroup.get('defaultDashboardFullscreen').enable();
    } else {
      this.selfRegistrationFormGroup.get('defaultDashboardFullscreen').disable();
    }
  }

  save(): void {
    this.selfRegistrationParams = {...this.selfRegistrationParams,
      ...this.selfRegistrationParamsFromFormValue(this.selfRegistrationFormGroup.value)};
    this.selfRegistrationService.saveSelfRegistrationParams(this.selfRegistrationParams).subscribe(
      (selfRegistrationParams) => {
        this.onSelfRegistrationParamsLoaded(selfRegistrationParams);
      }
    );
  }

  confirmForm(): FormGroup {
    return this.selfRegistrationFormGroup;
  }

  onActivationLinkCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('user.activation-link-copied-message'),
        type: 'success',
        target: 'registrationLinkContent',
        duration: 750,
        verticalPosition: 'top',
        horizontalPosition: 'left'
      }));
  }

  private onSelfRegistrationParamsLoaded(selfRegistrationParams: SelfRegistrationParams) {
    this.selfRegistrationParams = selfRegistrationParams || {};
    if (this.selfRegistrationParams.domainName && this.selfRegistrationParams.domainName.length) {
      this.registerLink = this.selfRegistrationService.getRegistrationLink(this.selfRegistrationParams.domainName);
    } else {
      this.registerLink = '';
    }
    const selfRegistrationFormValue = deepClone(this.selfRegistrationParams);
    if (selfRegistrationFormValue.signUpTextMessage && selfRegistrationFormValue.signUpTextMessage.length) {
      selfRegistrationFormValue.signUpTextMessage = this.convertHTMLToText(selfRegistrationFormValue.signUpTextMessage);
    }
    if (!selfRegistrationFormValue.permissions) {
      selfRegistrationFormValue.permissions = [];
    }
    this.selfRegistrationFormGroup.reset(selfRegistrationFormValue);
    this.updateDisabledState();
  }

  private selfRegistrationParamsFromFormValue(selfRegistrationParams: SelfRegistrationParams): SelfRegistrationParams {
    if (selfRegistrationParams.signUpTextMessage && selfRegistrationParams.signUpTextMessage.length) {
      selfRegistrationParams.signUpTextMessage = this.convertTextToHTML(selfRegistrationParams.signUpTextMessage);
    }
    return selfRegistrationParams;
  }

  private convertTextToHTML(str: string): string {
    return str.replace(/\n/g, '<br/>');
  }

  private convertHTMLToText(str: string): string {
    return str.replace(/<br\s*[/]?>/gi, '\n');
  }

}
