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

import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup, FormGroupDirective, Validators } from '@angular/forms';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import {
  RepositorySettings,
  RepositoryAuthMethod,
  repositoryAuthMethodTranslationMap
} from '@shared/models/settings.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { isNotEmptyStr } from '@core/utils';
import { DialogService } from '@core/services/dialog.service';
import { ActionAuthUpdateHasRepository } from '@core/auth/auth.actions';
import { selectHasRepository } from '@core/auth/auth.selectors';
import { catchError, mergeMap, take } from 'rxjs/operators';
import { of } from 'rxjs';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
  selector: 'tb-repository-settings',
  templateUrl: './repository-settings.component.html',
  styleUrls: ['./repository-settings.component.scss', './../../pages/admin/settings-card.scss']
})
export class RepositorySettingsComponent extends PageComponent implements OnInit {

  @Input()
  detailsMode = false;

  @Input()
  popoverComponent: TbPopoverComponent;

  repositorySettingsForm: UntypedFormGroup;
  settings: RepositorySettings = null;

  repositoryAuthMethod = RepositoryAuthMethod;
  repositoryAuthMethods = Object.values(RepositoryAuthMethod);
  repositoryAuthMethodTranslations = repositoryAuthMethodTranslationMap;

  showChangePassword = false;
  changePassword = false;

  showChangePrivateKeyPassword = false;
  changePrivateKeyPassword = false;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.VERSION_CONTROL, Operation.WRITE);
  allowDelete = this.userPermissionsService.hasGenericPermission(Resource.VERSION_CONTROL, Operation.DELETE);

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              private cd: ChangeDetectorRef,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.repositorySettingsForm = this.fb.group({
      repositoryUri: [null, [Validators.required]],
      defaultBranch: ['main', []],
      readOnly: [false, []],
      showMergeCommits: [false, []],
      authMethod: [RepositoryAuthMethod.USERNAME_PASSWORD, [Validators.required]],
      username: [null, []],
      password: [null, []],
      privateKeyFileName: [null, [Validators.required]],
      privateKey: [null, []],
      privateKeyPassword: [null, []]
    });
    this.updateValidators(false);
    this.repositorySettingsForm.get('authMethod').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.repositorySettingsForm.get('privateKeyFileName').valueChanges.subscribe(() => {
      this.updateValidators(false);
    });
    this.store.pipe(
      select(selectHasRepository),
      take(1),
      mergeMap((hasRepository) => {
        if (hasRepository) {
          return this.adminService.getRepositorySettings({ignoreErrors: true}).pipe(
            catchError(() => of(null))
          );
        } else {
          return of(null);
        }
      })
    ).subscribe(
      (settings) => {
        this.settings = settings;
        if (this.settings != null) {
          if (this.settings.authMethod === RepositoryAuthMethod.USERNAME_PASSWORD) {
            this.showChangePassword = true;
          } else {
            this.showChangePrivateKeyPassword = true;
          }
          this.repositorySettingsForm.reset(this.settings);
          this.updateValidators(false);
        }
    });
    if (this.readonly) {
      this.repositorySettingsForm.disable({emitEvent: false});
    }
  }

  checkAccess(): void {
    const settings: RepositorySettings = this.repositorySettingsForm.value;
    this.adminService.checkRepositoryAccess(settings).subscribe(() => {
      this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('admin.check-repository-access-success'),
        type: 'success' }));
    });
  }

  save(): void {
    const settings: RepositorySettings = this.repositorySettingsForm.value;
    this.adminService.saveRepositorySettings(settings).subscribe(
      (savedSettings) => {
        this.settings = savedSettings;
        if (this.settings.authMethod === RepositoryAuthMethod.USERNAME_PASSWORD) {
          this.showChangePassword = true;
          this.changePassword = false;
        } else {
          this.showChangePrivateKeyPassword = true;
          this.changePrivateKeyPassword = false;
        }
        this.repositorySettingsForm.reset(this.settings);
        this.updateValidators(false);
        this.store.dispatch(new ActionAuthUpdateHasRepository({ hasRepository: true }));
      }
    );
  }

  delete(formDirective: FormGroupDirective): void {
    this.dialogService.confirm(
      this.translate.instant('admin.delete-repository-settings-title', ),
      this.translate.instant('admin.delete-repository-settings-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.adminService.deleteRepositorySettings().subscribe(
          () => {
            this.settings = null;
            this.showChangePassword = false;
            this.changePassword = false;
            this.showChangePrivateKeyPassword = false;
            this.changePrivateKeyPassword = false;
            formDirective.resetForm();
            this.repositorySettingsForm.reset({ defaultBranch: 'main', authMethod: RepositoryAuthMethod.USERNAME_PASSWORD });
            this.updateValidators(false);
            this.store.dispatch(new ActionAuthUpdateHasRepository({ hasRepository: false }));
          }
        );
      }
    });
  }

  changePasswordChanged() {
    if (this.changePassword) {
      this.repositorySettingsForm.get('password').patchValue('');
      this.repositorySettingsForm.get('password').markAsDirty();
    }
    this.updateValidators(false);
  }

  changePrivateKeyPasswordChanged() {
    if (this.changePrivateKeyPassword) {
      this.repositorySettingsForm.get('privateKeyPassword').patchValue('');
      this.repositorySettingsForm.get('privateKeyPassword').markAsDirty();
    }
    this.updateValidators(false);
  }

  updateValidators(emitEvent?: boolean): void {
    if (this.readonly) {
      return;
    }
    const authMethod: RepositoryAuthMethod = this.repositorySettingsForm.get('authMethod').value;
    const privateKeyFileName: string = this.repositorySettingsForm.get('privateKeyFileName').value;
    if (authMethod === RepositoryAuthMethod.USERNAME_PASSWORD) {
      this.repositorySettingsForm.get('username').enable({emitEvent});
      if (this.changePassword || !this.showChangePassword) {
        this.repositorySettingsForm.get('password').enable({emitEvent});
      } else {
        this.repositorySettingsForm.get('password').disable({emitEvent});
      }
      this.repositorySettingsForm.get('privateKeyFileName').disable({emitEvent});
      this.repositorySettingsForm.get('privateKey').disable({emitEvent});
      this.repositorySettingsForm.get('privateKeyPassword').disable({emitEvent});
    } else {
      this.repositorySettingsForm.get('username').disable({emitEvent});
      this.repositorySettingsForm.get('password').disable({emitEvent});
      this.repositorySettingsForm.get('privateKeyFileName').enable({emitEvent});
      this.repositorySettingsForm.get('privateKey').enable({emitEvent});
      if (this.changePrivateKeyPassword || !this.showChangePrivateKeyPassword) {
        this.repositorySettingsForm.get('privateKeyPassword').enable({emitEvent});
      } else {
        this.repositorySettingsForm.get('privateKeyPassword').disable({emitEvent});
      }
      if (isNotEmptyStr(privateKeyFileName)) {
        this.repositorySettingsForm.get('privateKey').clearValidators();
      } else {
        this.repositorySettingsForm.get('privateKey').setValidators([Validators.required]);
      }
    }
    this.repositorySettingsForm.get('username').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('password').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('privateKeyFileName').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('privateKey').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('privateKeyPassword').updateValueAndValidity({emitEvent: false});
  }

}
