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

import { Component, DestroyRef, Inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { ActivatedRoute } from '@angular/router';
import { FormGroupDirective, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { LoginWhiteLabelingParams, WhiteLabelingParams } from '@shared/models/white-labeling.models';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { environment as env } from '@env/environment';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { mergeMap, Observable } from 'rxjs';
import { isDefined, isEqual } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import { CustomCssDialogComponent, CustomCssDialogData } from '@home/pages/admin/custom-css-dialog.component';
import { UiSettingsService } from '@core/http/ui-settings.service';
import { share } from 'rxjs/operators';
import { WINDOW } from '@core/services/window.service';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';
import { DomainDialogComponent } from '@home/pages/admin/oauth2/domains/domain-dialog.component';
import { Domain } from '@shared/models/oauth2.models';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-white-labeling',
  templateUrl: './white-labeling.component.html',
  styleUrls: ['./settings-card.scss']
})
export class WhiteLabelingComponent extends PageComponent implements OnInit, HasConfirmForm {

  wlSettings: UntypedFormGroup;
  private whiteLabelingParams: WhiteLabelingParams & LoginWhiteLabelingParams;

  isSysAdmin = getCurrentAuthUser(this.store).authority === Authority.SYS_ADMIN;
  isTenant = getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);
  isLoginWl: boolean = this.route.snapshot.data.isLoginWl;

  uiHelpBaseUrlPlaceholder$ = this.uiSettingsService.getHelpBaseUrl().pipe(
    share()
  );

  thingsboardVersion = env.tbVersion;

  showPosition = [
    {
      name: 'white-labeling.position.under-logo',
      value: false
    },
    {
      name: 'white-labeling.position.bottom',
      value: true
    }
  ];

  readonly EntityType = EntityType;
  readonly operation = Operation;
  readonly resource = Resource;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private userPermissionsService: UserPermissionsService,
              private whiteLabelingService: WhiteLabelingService,
              private uiSettingsService: UiSettingsService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super();
  }

  ngOnInit() {
    this.buildWhiteLabelingSettingsForm();
    this.loadWhiteLabelingParams();
  }

  private loadWhiteLabelingParams() {
    (this.isLoginWl
        ? this.whiteLabelingService.getCurrentLoginWhiteLabelParams()
        : this.whiteLabelingService.getCurrentWhiteLabelParams()
    ).subscribe((whiteLabelingParams) => {
      this.setWhiteLabelingParams(whiteLabelingParams);
    });
  }

  buildWhiteLabelingSettingsForm() {
    this.wlSettings = this.fb.group({
      appTitle: ['', []],
      favicon: this.fb.group(
      {
        url: [null, []],
        type: [null, []]
      }),
      faviconChecksum: [null, []],
      logoImageUrl: [null, []],
      logoImageChecksum: [null, []],
      logoImageHeight: [null, [Validators.min(1)]],
      paletteSettings: this.fb.group(
        {
          primaryPalette: [null, []],
          accentPalette: [null, []]
        }),
      showNameVersion: [null, []],
      platformName: [null, []],
      platformVersion: [null, []]
    });

    if (this.isLoginWl) {
      this.wlSettings.addControl('baseUrl',
        this.fb.control('', [Validators.required, Validators.pattern(/^(https?:\/\/)?(localhost|([\w\-]+\.)+[\w\-]+)(:\d+)?(\/[\w\-._~:\/?#[\]@!$&'()*+,;=%]*)?$/)])
      );
      this.wlSettings.addControl('prohibitDifferentUrl',
        this.fb.control('', [])
      );
    }

    if (this.isLoginWl && !this.isSysAdmin) {
      this.wlSettings.addControl('domainId',
        this.fb.control(null, Validators.required)
      );
    } else {
      this.wlSettings.addControl('enableHelpLinks',
        this.fb.control(null, [])
      );
      this.wlSettings.addControl('helpLinkBaseUrl',
        this.fb.control(null, [])
      );
      this.wlSettings.addControl('uiHelpBaseUrl',
        this.fb.control(null, [])
      );
    }
    if (this.isLoginWl) {
      this.wlSettings.addControl('darkForeground',
        this.fb.control(null, [])
      );
      this.wlSettings.addControl('pageBackgroundColor',
        this.fb.control(null, [])
      );
      this.wlSettings.addControl('showNameBottom',
        this.fb.control(null, [])
      );
    }
    if (!this.isLoginWl && this.isTenant) {
      this.wlSettings.addControl('hideConnectivityDialog',
        this.fb.control(false, [])
      );
    }
    if (this.readonly) {
      this.wlSettings.disable();
    } else {
      this.wlSettings.get('showNameVersion').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateValidators();
      });
    }
  }

  private updateValidators() {
    const showNameVersion: boolean = this.wlSettings.get('showNameVersion').value;
    if (showNameVersion) {
      this.wlSettings.get('platformName').setValidators([Validators.required]);
      this.wlSettings.get('platformVersion').setValidators([Validators.required]);
    } else {
      this.wlSettings.get('platformName').setValidators([]);
      this.wlSettings.get('platformVersion').setValidators([]);
    }
    this.wlSettings.get('platformName').updateValueAndValidity();
    this.wlSettings.get('platformVersion').updateValueAndValidity();
  }

  editCustomCss(): void {
    this.dialog.open<CustomCssDialogComponent, CustomCssDialogData, string>(CustomCssDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          customCss: this.whiteLabelingParams.customCss,
          readonly: this.readonly
        }
      }).afterClosed().subscribe((customCss) => {
      if (isDefined(customCss)) {
        if (!isEqual(this.whiteLabelingParams.customCss, customCss)) {
          this.whiteLabelingParams.customCss = customCss;
          this.wlSettings.markAsDirty();
        }
      }
    });
  }

  preview(): void {
    this.whiteLabelingParams = {...this.whiteLabelingParams, ...this.wlSettings.value};
    this.whiteLabelingService.whiteLabelPreview(this.whiteLabelingParams).subscribe();
  }

  save(): void {
    const whiteLabelingParams: WhiteLabelingParams & LoginWhiteLabelingParams = {...this.whiteLabelingParams, ...this.wlSettings.value};
    if (whiteLabelingParams.platformName === 'ThingsBoard') {
      whiteLabelingParams.platformName = null;
    }
    if (whiteLabelingParams.platformVersion === env.tbVersion) {
      whiteLabelingParams.platformVersion = null;
    }
    (this.isLoginWl ? this.whiteLabelingService.saveLoginWhiteLabelParams(whiteLabelingParams) :
        this.whiteLabelingService.saveWhiteLabelParams(whiteLabelingParams)).subscribe(() => {
          this.whiteLabelingParams = whiteLabelingParams;
          if (this.isLoginWl) {
            this.loadWhiteLabelingParams();
          } else {
            this.wlSettings.markAsPristine();
          }
    });
  }

  confirmForm(): UntypedFormGroup {
    return this.wlSettings;
  }

  onExit(): Observable<any> {
    return this.whiteLabelingService.cancelWhiteLabelPreview();
  }

  delete(form: FormGroupDirective) {
    const title = this.isLoginWl ? 'white-labeling.reset-login-white-label-title' : 'white-labeling.reset-white-label-title';
    const text = this.isLoginWl ? 'white-labeling.reset-login-white-label-text' : 'white-labeling.reset-white-label-text';
    this.dialogService.confirm(this.translate.instant(title), this.translate.instant(text)).subscribe((res) => {
      if (res) {
        let deleteParams: Observable<LoginWhiteLabelingParams | WhiteLabelingParams>;
        if (this.isLoginWl) {
          deleteParams = this.whiteLabelingService.deleteCurrentLoginWhiteLabelParams().pipe(
            mergeMap(() => this.whiteLabelingService.getCurrentLoginWhiteLabelParams())
          );
        } else {
          deleteParams =  this.whiteLabelingService.deleteCurrentWhiteLabelParams().pipe(
            mergeMap(() => this.whiteLabelingService.getCurrentWhiteLabelParams())
          );
        }
        deleteParams.subscribe((value) => {
          this.setWhiteLabelingParams(value);
          form.resetForm(value);
        })
      }
    })
  }

  private setWhiteLabelingParams(whiteLabelingParams: WhiteLabelingParams & LoginWhiteLabelingParams) {
    this.whiteLabelingParams = whiteLabelingParams;
    this.wlSettings.reset(this.whiteLabelingParams, {emitEvent: false});
    if (!this.readonly) {
      this.updateValidators();
    }
  }

  domainChange(domain: BaseData<EntityId>) {
    const baseUrlFormControl = this.wlSettings.get('baseUrl');
    if (baseUrlFormControl.pristine && !this.whiteLabelingParams?.baseUrl) {
      baseUrlFormControl.patchValue(domain?.name ? this.window.location.protocol + '//' + domain.name : '');
    }
  }

  createDomain() {
    this.dialog.open<DomainDialogComponent, any, Domain>(DomainDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
    }).afterClosed()
      .subscribe((domain) => {
        if (domain) {
          this.wlSettings.get('domainId').patchValue(domain.id);
          this.wlSettings.get('domainId').markAsDirty();
        }
      });
  }
}
