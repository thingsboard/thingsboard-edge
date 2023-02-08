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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { AdminSettings, MailServerSettings, smtpPortPattern } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { isDefined, isDefinedAndNotNull, isString } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-mail-server',
  templateUrl: './mail-server.component.html',
  styleUrls: ['./mail-server.component.scss', './settings-card.scss']
})
export class MailServerComponent extends PageComponent implements OnInit, OnDestroy, HasConfirmForm {

  authState: AuthState = getCurrentAuthState(this.store);

  authUser: AuthUser = this.authState.authUser;

  mailSettings: UntypedFormGroup;
  adminSettings: AdminSettings<MailServerSettings>;
  smtpProtocols = ['smtp', 'smtps'];
  showChangePassword = false;

  tlsVersions = ['TLSv1', 'TLSv1.1', 'TLSv1.2', 'TLSv1.3'];

  readonly = this.isTenantAdmin() && !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              private translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildMailServerSettingsForm();
    this.adminService.getAdminSettings<MailServerSettings>('mail', false).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        if (this.adminSettings.jsonValue && isString(this.adminSettings.jsonValue.enableTls)) {
          this.adminSettings.jsonValue.enableTls = (this.adminSettings.jsonValue.enableTls as any) === 'true';
        }
        this.showChangePassword =
          isDefinedAndNotNull(this.adminSettings.jsonValue.showChangePassword) ? this.adminSettings.jsonValue.showChangePassword : true ;
        delete this.adminSettings.jsonValue.showChangePassword;
        this.mailSettings.reset(this.adminSettings.jsonValue);
        if (this.isTenantAdmin()) {
          this.mailSettings.get('useSystemMailSettings').setValue(
            isDefined(this.adminSettings.jsonValue.useSystemMailSettings) ?
              this.adminSettings.jsonValue.useSystemMailSettings : true, {emitEvent: false}
          );
          this.showChangePassword = this.showChangePassword && isDefined(this.adminSettings.jsonValue.useSystemMailSettings);
        }
        this.updateValidators();
        this.enableMailPassword(!this.showChangePassword);
      }
    );
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  public isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  buildMailServerSettingsForm() {
    this.mailSettings = this.fb.group({
      useSystemMailSettings: [false],
      mailFrom: ['', [Validators.required]],
      smtpProtocol: ['smtp'],
      smtpHost: ['localhost', [Validators.required]],
      smtpPort: ['25', [Validators.required,
        Validators.pattern(smtpPortPattern),
        Validators.maxLength(5)]],
      timeout: ['10000', [Validators.required,
        Validators.pattern(/^[0-9]{1,6}$/),
        Validators.maxLength(6)]],
      enableTls: [false],
      tlsVersion: [],
      enableProxy: [false, []],
      proxyHost: ['', [Validators.required]],
      proxyPort: ['', [Validators.required, Validators.min(1), Validators.max(65535)]],
      proxyUser: [''],
      proxyPassword: [''],
      username: [''],
      changePassword: [false],
      password: ['']
    });
    if (this.readonly) {
      this.mailSettings.get('smtpProtocol').disable({emitEvent: false});
      this.mailSettings.get('enableTls').disable({emitEvent: false});
      this.mailSettings.get('enableProxy').disable({emitEvent: false});
      this.mailSettings.get('changePassword').disable({emitEvent: false});
      if (this.isTenantAdmin()) {
        this.mailSettings.get('useSystemMailSettings').disable({emitEvent: false});
      }
    } else {
      this.registerDisableOnLoadFormControl(this.mailSettings.get('smtpProtocol'));
      this.registerDisableOnLoadFormControl(this.mailSettings.get('enableTls'));
      this.registerDisableOnLoadFormControl(this.mailSettings.get('enableProxy'));
      this.registerDisableOnLoadFormControl(this.mailSettings.get('changePassword'));
      if (this.isTenantAdmin()) {
        this.registerDisableOnLoadFormControl(this.mailSettings.get('useSystemMailSettings'));
      }
    }
    if (this.isTenantAdmin()) {
      this.mailSettings.get('useSystemMailSettings').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe(
        () => {
          this.updateValidators();
        }
      );
    }
    this.mailSettings.get('enableProxy').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      () => {
        this.updateValidators();
      }
    );
    this.mailSettings.get('changePassword').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.enableMailPassword(value);
    });
  }

  private updateValidators() {
    const useSystemMailSettings: boolean = this.mailSettings.get('useSystemMailSettings').value;
    const enableProxy: boolean = this.mailSettings.get('enableProxy').value;
    if (useSystemMailSettings) {
      this.mailSettings.get('mailFrom').setValidators([]);
      this.mailSettings.get('smtpHost').setValidators([]);
      this.mailSettings.get('smtpPort').setValidators([]);
      this.mailSettings.get('timeout').setValidators([]);
      this.mailSettings.get('proxyHost').setValidators([]);
      this.mailSettings.get('proxyPort').setValidators([]);
    } else {
      this.mailSettings.get('mailFrom').setValidators([Validators.required]);
      this.mailSettings.get('smtpHost').setValidators([Validators.required]);
      this.mailSettings.get('smtpPort').setValidators([Validators.required,
        Validators.pattern(smtpPortPattern),
        Validators.maxLength(5)]);
      this.mailSettings.get('timeout').setValidators([Validators.required,
        Validators.pattern(/^[0-9]{1,6}$/),
        Validators.maxLength(6)]);
      this.mailSettings.get('proxyHost').setValidators(enableProxy ? [Validators.required] : []);
      this.mailSettings.get('proxyPort').setValidators(enableProxy ? [Validators.required, Validators.min(1), Validators.max(65535)] : []);
    }
    this.mailSettings.get('mailFrom').updateValueAndValidity({emitEvent: false});
    this.mailSettings.get('smtpHost').updateValueAndValidity({emitEvent: false});
    this.mailSettings.get('smtpPort').updateValueAndValidity({emitEvent: false});
    this.mailSettings.get('timeout').updateValueAndValidity({emitEvent: false});
    this.mailSettings.get('proxyHost').updateValueAndValidity({emitEvent: false});
    this.mailSettings.get('proxyPort').updateValueAndValidity({emitEvent: false});
  }

  enableMailPassword(enable: boolean) {
    if (enable) {
      this.mailSettings.get('password').enable({emitEvent: false});
    } else {
      this.mailSettings.get('password').disable({emitEvent: false});
    }
  }

  sendTestMail(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.mailSettingsFormValue};
    this.adminService.sendTestMail(this.adminSettings).subscribe(
      () => {
        this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('admin.test-mail-sent'),
          type: 'success' }));
      }
    );
  }

  save(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.mailSettingsFormValue};
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.showChangePassword = true;
        this.mailSettings.reset(this.adminSettings.jsonValue);
      }
    );
  }

  confirmForm(): UntypedFormGroup {
    return this.mailSettings;
  }

  private get mailSettingsFormValue(): MailServerSettings {
    const formValue = this.mailSettings.value;
    delete formValue.changePassword;
    return formValue;
  }
}
