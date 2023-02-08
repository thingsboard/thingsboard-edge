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
import { ActivatedRoute, Router } from '@angular/router';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { LoginWhiteLabelingParams, WhiteLabelingParams } from '@shared/models/white-labeling.models';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { environment as env } from '@env/environment';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Observable } from 'rxjs';
import { isDefined, isEqual } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import { CustomCssDialogComponent, CustomCssDialogData } from '@home/pages/admin/custom-css-dialog.component';
import { UiSettingsService } from '@core/http/ui-settings.service';
import { share } from 'rxjs/operators';

@Component({
  selector: 'tb-white-labeling',
  templateUrl: './white-labeling.component.html',
  styleUrls: ['./settings-card.scss']
})
export class WhiteLabelingComponent extends PageComponent implements OnInit, HasConfirmForm {

  maxFaviconSize = 262144;
  maxFaviconSizeKb = this.maxFaviconSize / 1024;
  faviconTypes = ['image/x-icon', 'image/png', 'image/gif', 'image/vnd.microsoft.icon'];

  maxLogoSize = 4194304;
  maxLogoSizeKb = this.maxLogoSize / 1024;

  wlSettings: UntypedFormGroup;
  whiteLabelingParams: WhiteLabelingParams & LoginWhiteLabelingParams;

  isSysAdmin = getCurrentAuthUser(this.store).authority === Authority.SYS_ADMIN;

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

  constructor(protected store: Store<AppState>,
              private router: Router,
              private route: ActivatedRoute,
              private userPermissionsService: UserPermissionsService,
              private whiteLabelingService: WhiteLabelingService,
              private uiSettingsService: UiSettingsService,
              private translate: TranslateService,
              private dialog: MatDialog,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildWhiteLabelingSettingsForm();
    this.loadWhiteLabelingParams();
  }

  private loadWhiteLabelingParams() {
    (this.isLoginWl ? this.whiteLabelingService.getCurrentLoginWhiteLabelParams()
      : this.whiteLabelingService.getCurrentWhiteLabelParams()).subscribe((whiteLabelingParams) => {
      this.whiteLabelingParams = whiteLabelingParams;
      if (!this.whiteLabelingParams.paletteSettings) {
        this.whiteLabelingParams.paletteSettings = {};
      }
      if(this.whiteLabelingParams.platformName === null){
        this.whiteLabelingParams.platformName = 'ThingsBoard';
      }
      if(this.whiteLabelingParams.platformVersion === null){
        this.whiteLabelingParams.platformVersion = env.tbVersion;
      }
      this.wlSettings.reset(this.whiteLabelingParams);
      if (!this.readonly) {
        this.updateValidators();
      }
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
        this.fb.control('', [Validators.required])
      );
      this.wlSettings.addControl('prohibitDifferentUrl',
        this.fb.control('', [])
      );
    }

    if (this.isLoginWl && !this.isSysAdmin) {
      this.wlSettings.addControl('domainName',
        this.fb.control('', [Validators.required, Validators.pattern('((?![:/]).)*$')])
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
    if (this.readonly) {
      this.wlSettings.disable();
    } else {
      this.wlSettings.get('showNameVersion').valueChanges.subscribe(() => {
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

  onFaviconTypeError() {
    this.showError(this.translate.instant('white-labeling.favicon-type-error'));
  }

  onFaviconSizeError() {
    this.showError(this.translate.instant('white-labeling.favicon-size-error', {kbSize: this.maxFaviconSizeKb}));
  }

  onFaviconCleared() {
    this.wlSettings.get('favicon').get('type').setValue(null);
    this.wlSettings.get('faviconChecksum').setValue(null);
  }

  onLogoTypeError() {
    this.showError(this.translate.instant('white-labeling.logo-type-error'));
  }

  onLogoSizeError() {
    this.showError(this.translate.instant('white-labeling.logo-size-error', {kbSize: this.maxLogoSizeKb}));
  }

  onLogoCleared() {
    this.wlSettings.get('logoImageChecksum').setValue(null);
  }

  private showError(error: string) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: error,
        type: 'error'
      }));
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
    this.whiteLabelingParams = {...this.whiteLabelingParams, ...this.wlSettings.value};
    if (this.whiteLabelingParams.platformName === 'ThingsBoard') {
      this.whiteLabelingParams.platformName = null;
    }
    if (this.whiteLabelingParams.platformVersion === env.tbVersion) {
      this.whiteLabelingParams.platformVersion = null;
    }
    (this.isLoginWl ? this.whiteLabelingService.saveLoginWhiteLabelParams(this.whiteLabelingParams) :
        this.whiteLabelingService.saveWhiteLabelParams(this.whiteLabelingParams)).subscribe(() => {
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

}
