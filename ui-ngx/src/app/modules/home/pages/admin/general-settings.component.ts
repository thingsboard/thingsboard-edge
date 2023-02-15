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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminSettings, GeneralSettings } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';

@Component({
  selector: 'tb-general-settings',
  templateUrl: './general-settings.component.html',
  styleUrls: ['./general-settings.component.scss', './settings-card.scss']
})
export class GeneralSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  generalSettings: FormGroup;
  adminSettings: AdminSettings<GeneralSettings>;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildGeneralServerSettingsForm();
    this.adminService.getAdminSettings<GeneralSettings>('general').subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.generalSettings.reset(this.adminSettings.jsonValue);
      }
    );
  }

  buildGeneralServerSettingsForm() {
    this.generalSettings = this.fb.group({
      baseUrl: ['', [Validators.required]],
      prohibitDifferentUrl: ['',[]]
    });
  }

  save(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.generalSettings.value};
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.generalSettings.reset(this.adminSettings.jsonValue);
      }
    );
  }

  confirmForm(): FormGroup {
    return this.generalSettings;
  }

}
