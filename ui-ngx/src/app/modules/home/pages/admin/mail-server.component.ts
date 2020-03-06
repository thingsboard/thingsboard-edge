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
import { AdminSettings, MailServerSettings, smtpPortPattern } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';

@Component({
  selector: 'tb-mail-server',
  templateUrl: './mail-server.component.html',
  styleUrls: ['./mail-server.component.scss', './settings-card.scss']
})
export class MailServerComponent extends PageComponent implements OnInit, HasConfirmForm {

  mailSettings: FormGroup;
  adminSettings: AdminSettings<MailServerSettings>;
  smtpProtocols = ['smtp', 'smtps'];

  tlsVersions = ['TLSv1', 'TLSv1.1', 'TLSv1.2', 'TLSv1.3'];

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              private translate: TranslateService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildMailServerSettingsForm();
    this.adminService.getAdminSettings<MailServerSettings>('mail').subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.mailSettings.reset(this.adminSettings.jsonValue);
      }
    );
  }

  buildMailServerSettingsForm() {
    this.mailSettings = this.fb.group({
      mailFrom: ['', [Validators.required]],
      smtpProtocol: ['smtp'],
      smtpHost: ['localhost', [Validators.required]],
      smtpPort: ['25', [Validators.required,
        Validators.pattern(smtpPortPattern),
        Validators.maxLength(5)]],
      timeout: ['10000', [Validators.required,
        Validators.pattern(/^[0-9]{1,6}$/),
        Validators.maxLength(6)]],
      enableTls: ['false'],
      tlsVersion: [],
      username: [''],
      password: ['']
    });
    this.registerDisableOnLoadFormControl(this.mailSettings.get('smtpProtocol'));
    this.registerDisableOnLoadFormControl(this.mailSettings.get('enableTls'));
  }

  sendTestMail(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.mailSettings.value};
    this.adminService.sendTestMail(this.adminSettings).subscribe(
      () => {
        this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('admin.test-mail-sent'),
          type: 'success' }));
      }
    );
  }

  save(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.mailSettings.value};
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.mailSettings.reset(this.adminSettings.jsonValue);
      }
    );
  }

  confirmForm(): FormGroup {
    return this.mailSettings;
  }

}
