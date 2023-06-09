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

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminSettings, SmsProviderConfiguration } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { MatDialog } from '@angular/material/dialog';
import { SendTestSmsDialogComponent, SendTestSmsDialogData } from '@home/pages/admin/send-test-sms-dialog.component';
import { NotificationSettings } from '@shared/models/notification.models';
import { deepTrim, isDefined, isEmptyStr } from '@core/utils';
import { NotificationService } from '@core/http/notification.service';
import { Authority } from '@shared/models/authority.enum';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
  selector: 'tb-sms-provider',
  templateUrl: './sms-provider.component.html',
  styleUrls: ['./sms-provider.component.scss', './settings-card.scss']
})
export class SmsProviderComponent extends PageComponent implements HasConfirmForm {

  smsProvider: FormGroup;
  private adminSettings: AdminSettings<SmsProviderConfiguration>;

  slackSettingsForm: FormGroup;
  private notificationSettings: NotificationSettings;

  private readonly authUser = getCurrentAuthUser(this.store);
  readonly = this.isTenantAdmin() && !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              private notificationService: NotificationService,
              private dialog: MatDialog,
              private userPermissionsService: UserPermissionsService,
              public fb: FormBuilder) {
    super(store);
    this.buildSmsProviderForm();
    this.buildGeneralServerSettingsForm();
    this.notificationService.getNotificationSettings().subscribe(
      (settings) => {
        this.notificationSettings = settings;
        this.slackSettingsForm.reset(this.notificationSettings);
      }
    );
    this.adminService.getAdminSettings<SmsProviderConfiguration>('sms', false, {ignoreErrors: true}).subscribe({
      next: adminSettings => {
        this.adminSettings = adminSettings;
        this.setSmsProviderSettings(this.adminSettings.jsonValue);
      },
      error: () => {
        this.adminSettings = {
          key: 'sms',
          jsonValue: null
        };
        this.setSmsProviderSettings(this.adminSettings.jsonValue);
      }
    });
  }

  private setSmsProviderSettings(smsProviderConfiguration: SmsProviderConfiguration) {
    const useSystemSmsSettings = this.isTenantAdmin()
      ? (smsProviderConfiguration && isDefined(smsProviderConfiguration.useSystemSmsSettings) ?
         smsProviderConfiguration.useSystemSmsSettings : true)
      : false;
    if (smsProviderConfiguration) {
      delete smsProviderConfiguration.useSystemSmsSettings;
    }
    this.smsProvider.reset({configuration: useSystemSmsSettings ? null : smsProviderConfiguration});
    if (this.isTenantAdmin()) {
      this.smsProvider.get('useSystemSmsSettings').setValue(
        useSystemSmsSettings, {emitEvent: false}
      );
    }
    this.updateValidators();
  }

  public isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  private buildSmsProviderForm() {
    this.smsProvider = this.fb.group({
      useSystemSmsSettings: [false],
      configuration: [null, [Validators.required]]
    });
    if (this.readonly) {
      this.smsProvider.get('configuration').disable({emitEvent: false});
      if (this.isTenantAdmin()) {
        this.smsProvider.get('useSystemSmsSettings').disable({emitEvent: false});
      }
    } else {
      this.registerDisableOnLoadFormControl(this.smsProvider.get('configuration'));
      if (this.isTenantAdmin()) {
        this.registerDisableOnLoadFormControl(this.smsProvider.get('useSystemSmsSettings'));
      }
    }
    if (this.isTenantAdmin()) {
      this.smsProvider.get('useSystemSmsSettings').valueChanges.subscribe(
        () => {
          this.updateValidators();
        }
      );
    }
  }

  private updateValidators() {
    const useSystemSmsSettings: boolean = this.smsProvider.get('useSystemSmsSettings').value;
    if (this.isTenantAdmin() && useSystemSmsSettings) {
      this.smsProvider.get('configuration').setValidators([]);
    } else {
      this.smsProvider.get('configuration').setValidators([Validators.required]);
    }
    this.smsProvider.get('configuration').updateValueAndValidity({emitEvent: false});
  }

  sendTestSms(): void {
    this.dialog.open<SendTestSmsDialogComponent, SendTestSmsDialogData>(SendTestSmsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        smsProviderConfiguration: this.smsProvider.value.configuration
      }
    });
  }

  save(): void {
    if (this.isTenantAdmin()) {
      if (this.smsProvider.value.useSystemSmsSettings) {
        this.adminSettings.jsonValue = {
          useSystemSmsSettings: true
        } as SmsProviderConfiguration;
      } else {
        this.adminSettings.jsonValue = {...this.smsProvider.value.configuration, useSystemSmsSettings: false};
      }
    } else {
      this.adminSettings.jsonValue = this.smsProvider.value.configuration;
    }
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.setSmsProviderSettings(this.adminSettings.jsonValue);
      }
    );
  }

  confirmForm(): FormGroup {
    return this.smsProvider.dirty ? this.smsProvider : this.slackSettingsForm;
  }

  private buildGeneralServerSettingsForm() {
    this.slackSettingsForm = this.fb.group({
      deliveryMethodsConfigs: this.fb.group({
        SLACK: this.fb.group({
          botToken: ['']
        })
      })
    });
    if(this.readonly) {
      this.slackSettingsForm.disable(({emitEvent: false}));
    } else {
      this.registerDisableOnLoadFormControl(this.slackSettingsForm.get('deliveryMethodsConfigs'));
    }
  }

  saveNotification(): void {
    this.notificationSettings = deepTrim({
      ...this.notificationSettings,
      ...this.slackSettingsForm.value
    });
    // eslint-disable-next-line guard-for-in
    for (const method in this.notificationSettings.deliveryMethodsConfigs) {
      const keys = Object.keys(this.notificationSettings.deliveryMethodsConfigs[method]);
      if (keys.some(item => isEmptyStr(this.notificationSettings.deliveryMethodsConfigs[method][item]))) {
        delete this.notificationSettings.deliveryMethodsConfigs[method];
      } else {
        this.notificationSettings.deliveryMethodsConfigs[method].method = method;
      }
    }
    this.notificationService.saveNotificationSettings(this.notificationSettings).subscribe(setting => {
      this.notificationSettings = setting;
      this.slackSettingsForm.reset(this.notificationSettings);
    });
  }
}
