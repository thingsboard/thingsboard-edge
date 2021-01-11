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

import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { SelfRegistrationService } from '@core/http/self-register.service';
import { SelfRegistrationParams } from '@shared/models/self-register.models';
import { deepClone } from '@core/utils';
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
