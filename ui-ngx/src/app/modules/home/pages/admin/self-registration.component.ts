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

import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { UntypedFormBuilder, UntypedFormGroup, FormGroupDirective, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { SelfRegistrationService } from '@core/http/self-register.service';
import { SelfRegistrationParams } from '@shared/models/self-register.models';
import { deepClone, isNotEmptyStr, randomAlphanumeric } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeService } from '@core/http/attribute.service';

@Component({
  selector: 'tb-self-registration',
  templateUrl: './self-registration.component.html',
  styleUrls: ['./self-registration.component.scss', './settings-card.scss']
})
export class SelfRegistrationComponent extends PageComponent implements OnInit, HasConfirmForm {

  selfRegistrationFormGroup: UntypedFormGroup;
  selfRegistrationParams: SelfRegistrationParams;
  registerLink: string;
  deleteDisabled: boolean = true;

  entityTypes = EntityType;

  tinyMceOptions: Record<string, any> = {
    base_url: '/assets/tinymce',
    suffix: '.min',
    plugins: ['link table image imagetools code fullscreen'],
    menubar: 'edit insert tools view format table',
    toolbar: 'fontselect fontsizeselect | formatselect | bold italic  strikethrough  forecolor backcolor ' +
      '| link | table | image | alignleft aligncenter alignright alignjustify  ' +
      '| numlist bullist outdent indent  | removeformat | code | fullscreen',
    height: 380,
    autofocus: false,
    branding: false,
    resize: true
  };

  constructor(protected store: Store<AppState>,
              private router: Router,
              private selfRegistrationService: SelfRegistrationService,
              private attributeService: AttributeService,
              private translate: TranslateService,
              public fb: UntypedFormBuilder) {
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
      privacyPolicy: [null],
      showPrivacyPolicy: [true],
      termsOfUse: [null],
      showTermsOfUse: [true],
      enableMobileSelfRegistration: [false],
      pkgName: [null, [Validators.required]],
      appSecret: [null, [Validators.required, Validators.minLength(16), Validators.maxLength(2048),
        Validators.pattern(/^[A-Za-z0-9]+$/)]],
      appScheme: [null, [Validators.required]],
      appHost: [null, [Validators.required]]
    });
    this.selfRegistrationFormGroup.get('defaultDashboardId').valueChanges.subscribe(
      () => {
        this.updateDisabledState();
      }
    );
    this.selfRegistrationFormGroup.get('enableMobileSelfRegistration').valueChanges.subscribe(
      () => {
        this.updateMobileSelfRegistration();
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

  private updateMobileSelfRegistration() {
    const enableMobileSelfRegistration = this.selfRegistrationFormGroup.get('enableMobileSelfRegistration').value;
    if (enableMobileSelfRegistration) {
      this.selfRegistrationFormGroup.get('pkgName').enable();
      this.selfRegistrationFormGroup.get('appSecret').enable();
      this.selfRegistrationFormGroup.get('appScheme').enable();
      this.selfRegistrationFormGroup.get('appHost').enable();
      const appSecret = this.selfRegistrationFormGroup.get('appSecret').value;
      if (!isNotEmptyStr(appSecret)) {
        this.selfRegistrationFormGroup.get('appSecret').patchValue(randomAlphanumeric(24), {emitEvent: false});
      }
    } else {
      this.selfRegistrationFormGroup.get('pkgName').disable();
      this.selfRegistrationFormGroup.get('appSecret').disable();
      this.selfRegistrationFormGroup.get('appScheme').disable();
      this.selfRegistrationFormGroup.get('appHost').disable();
    }
    this.selfRegistrationFormGroup.updateValueAndValidity({emitEvent: false});
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

  delete(form: FormGroupDirective): void {
    this.selfRegistrationService.deleteSelfRegistrationParams(this.selfRegistrationParams.domainName).subscribe(
      () => {
        this.onSelfRegistrationParamsLoaded(null);
        form.resetForm();
      }
    );
  }

  confirmForm(): UntypedFormGroup {
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
    if (selfRegistrationFormValue.showPrivacyPolicy == null) {
      selfRegistrationFormValue.showPrivacyPolicy = true;
    }
    if (selfRegistrationFormValue.showTermsOfUse == null) {
      selfRegistrationFormValue.showTermsOfUse = false;
    }
    this.deleteDisabled = !this.selfRegistrationParams.adminSettingsId;
    (selfRegistrationFormValue as any).enableMobileSelfRegistration = isNotEmptyStr(selfRegistrationFormValue.pkgName);
    this.selfRegistrationFormGroup.reset(selfRegistrationFormValue);
    this.updateDisabledState();
    this.updateMobileSelfRegistration();
  }

  private selfRegistrationParamsFromFormValue(selfRegistrationParams: SelfRegistrationParams): SelfRegistrationParams {
    if (selfRegistrationParams.signUpTextMessage && selfRegistrationParams.signUpTextMessage.length) {
      selfRegistrationParams.signUpTextMessage = this.convertTextToHTML(selfRegistrationParams.signUpTextMessage);
    }
    if (!(selfRegistrationParams as any).enableMobileSelfRegistration) {
      selfRegistrationParams.pkgName = null;
      selfRegistrationParams.appSecret = null;
      selfRegistrationParams.appScheme = null;
      selfRegistrationParams.appHost = null;
    }
    delete (selfRegistrationParams as any).enableMobileSelfRegistration;
    return selfRegistrationParams;
  }

  private convertTextToHTML(str: string): string {
    return str.replace(/\n/g, '<br/>');
  }

  private convertHTMLToText(str: string): string {
    return str.replace(/<br\s*[/]?>/gi, '\n');
  }

}
