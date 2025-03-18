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

import { Component, DestroyRef, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormGroupDirective, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { SelfRegistrationService } from '@core/http/self-register.service';
import { CaptchaParams, SelfRegistrationType, WebSelfRegistrationParams } from '@shared/models/self-register.models';
import { deepClone } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { DomainDialogComponent } from '@home/pages/admin/oauth2/domains/domain-dialog.component';
import { Domain } from '@shared/models/oauth2.models';
import { MatDialog } from '@angular/material/dialog';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { Operation, Resource } from '@shared/models/security.models';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { NotificationTarget } from '@shared/models/notification.models';
import { EditorOptions } from 'tinymce';
import { DialogService } from '@core/services/dialog.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-self-registration',
  templateUrl: './self-registration.component.html',
  styleUrls: ['./self-registration.component.scss', './settings-card.scss']
})
export class SelfRegistrationComponent extends PageComponent implements OnInit, HasConfirmForm {

  selfRegistrationFormGroup: UntypedFormGroup;
  selfRegistrationParams: WebSelfRegistrationParams;
  registerLink: string;

  entityTypes = EntityType;

  deleteDisabled = true;

  tinyMceOptions: Partial<EditorOptions> = {
    base_url: '/assets/tinymce',
    suffix: '.min',
    plugins: ['link', 'table', 'image', 'imagetools', 'code', 'fullscreen', 'lists'],
    menubar: 'edit insert tools view format table',
    toolbar_mode: 'sliding',
    toolbar: 'fontfamily fontsize | bold italic  strikethrough  forecolor backcolor ' +
      '| link table image | alignleft aligncenter alignright alignjustify  ' +
      '| numlist bullist outdent indent | blocks | removeformat code | fullscreen',
    height: 380,
    autofocus: false,
    branding: false,
    resize: true,
    promotion: false,
    relative_urls: false,
    urlconverter_callback: (url) => url
  };

  showMainLoadingBar = false;

  readonly EntityType = EntityType;
  readonly operation = Operation;
  readonly resource = Resource;

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private selfRegistrationService: SelfRegistrationService,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super();
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
      domainId: [null, [Validators.required]],
      captcha: this.fb.group({
        version: ['v3', Validators.required],
        siteKey: ['', Validators.required],
        secretKey: ['', Validators.required],
        logActionName: ['']
      }),
      notificationRecipient: [null, Validators.required],
      title: [null, [Validators.maxLength(200)]],
      permissions: [null],
      defaultDashboard: this.fb.group({
        id: [null],
        fullscreen: [false]
      }),
      privacyPolicy: [null],
      showPrivacyPolicy: [true],
      termsOfUse: [null],
      showTermsOfUse: [true]
    });
    this.selfRegistrationFormGroup.get('defaultDashboard.id').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateDisabledState();
    });
  }

  private updateDisabledState() {
    const defaultDashboardId = this.selfRegistrationFormGroup.get('defaultDashboard.id').value;
    if (defaultDashboardId) {
      this.selfRegistrationFormGroup.get('defaultDashboard.fullscreen').enable();
    } else {
      this.selfRegistrationFormGroup.get('defaultDashboard.fullscreen').disable();
    }
  }

  save(): void {
    this.selfRegistrationParams = {...this.selfRegistrationParams,
      ...this.selfRegistrationParamsFromFormValue(this.selfRegistrationFormGroup.value)};
    this.selfRegistrationParams.type = SelfRegistrationType.WEB;
    this.selfRegistrationService.saveSelfRegistrationParams(this.selfRegistrationParams).subscribe(
      (selfRegistrationParams) => {
        this.onSelfRegistrationParamsLoaded(selfRegistrationParams);
      }
    );
  }

  delete(form: FormGroupDirective): void {
    this.dialogService.confirm(
      this.translate.instant('mobile.self-registration.reset-self-registration-title'),
      this.translate.instant('mobile.self-registration.reset-self-registration-text'),
    ).subscribe(res => {
      if (res) {
        this.selfRegistrationService.deleteSelfRegistrationParams().subscribe(() => {
          this.onSelfRegistrationParamsLoaded(null);
          this.registerLink = '';
          form.resetForm({captcha: {version: 'v3'}});
        });
      }
    })
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

  domainChange(domain: BaseData<EntityId>) {
    if (domain?.name?.length) {
      this.registerLink = this.selfRegistrationService.getRegistrationLink(domain.name);
    } else {
      this.registerLink = '';
    }
  }

  createDomain() {
    this.dialog.open<DomainDialogComponent, any, Domain>(DomainDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
    }).afterClosed()
      .subscribe((domain) => {
        if (domain) {
          this.selfRegistrationFormGroup.get('domainId').patchValue(domain.id);
          this.selfRegistrationFormGroup.get('domainId').markAsDirty();
        }
      });
  }

  createTarget() {
    this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData,
      NotificationTarget>(RecipientNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.selfRegistrationFormGroup.get('notificationRecipient').setValue(res.id);
        }
      })
  }

  private onSelfRegistrationParamsLoaded(selfRegistrationParams: WebSelfRegistrationParams) {
    this.selfRegistrationParams = selfRegistrationParams || {
      type: SelfRegistrationType.WEB,
      enabled: true
    } as WebSelfRegistrationParams;
    this.deleteDisabled = !this.selfRegistrationParams.domainId;
    const selfRegistrationFormValue = deepClone(this.selfRegistrationParams);
    if (selfRegistrationFormValue.title?.length) {
      selfRegistrationFormValue.title = this.convertHTMLToText(selfRegistrationFormValue.title);
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
    if (!selfRegistrationFormValue.captcha) {
      selfRegistrationFormValue.captcha = {} as CaptchaParams;
    }
    selfRegistrationFormValue.captcha.version = selfRegistrationFormValue.captcha.version || 'v3';
    this.selfRegistrationFormGroup.reset(selfRegistrationFormValue);
    this.updateDisabledState();
  }

  private selfRegistrationParamsFromFormValue(selfRegistrationParams: WebSelfRegistrationParams): WebSelfRegistrationParams {
    if (selfRegistrationParams.title?.length) {
      selfRegistrationParams.title = this.convertTextToHTML(selfRegistrationParams.title);
    }
    return selfRegistrationParams;
  }

  private convertTextToHTML(str: string): string {
    return str.replace(/\n/g, '<br/>');
  }

  private convertHTMLToText(str: string): string {
    return str.replace(/<br\s*\/?>/gi, '\n');
  }
}
