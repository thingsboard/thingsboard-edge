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

import { Component, OnInit, DestroyRef } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TrendzSettingsService } from '@core/http/trendz-settings.service';
import { TrendzSettings } from '@shared/models/trendz-settings.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Authority } from '@shared/models/authority.enum';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
  selector: 'tb-trendz-settings',
  templateUrl: './trendz-settings.component.html',
  styleUrls: ['./trendz-settings.component.scss', './settings-card.scss']
})
export class TrendzSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {
  readonly = !this.userPermissionsService.hasGenericPermission(Resource.ADMIN_SETTINGS, Operation.WRITE);
  trendzSettingsForm: FormGroup;

  constructor(private fb: FormBuilder,
              private trendzSettingsService: TrendzSettingsService,
              private destroyRef: DestroyRef,
              private userPermissionsService: UserPermissionsService,) {
    super();
  }

  ngOnInit() {
    this.trendzSettingsForm = this.fb.group({
      isTrendzEnabled: [false],
      trendzUrl: [null, [Validators.pattern(/^(https?:\/\/)[^\s/$.?#].[^\s]*$/i)]],
      apiKey: [null, [Validators.pattern(/^\S+$/)]]
    });

    if(this.readonly) {
      this.trendzSettingsForm.disable({emitEvent: false});
    };

    this.trendzSettingsService.getTrendzSettings().subscribe((trendzSettings) => {
      this.setTrendzSettings(trendzSettings);
    });

    this.trendzSettingsForm.get('isTrendzEnabled').valueChanges
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe((enabled: boolean) => this.toggleUrlRequired(enabled));
  }

  toggleUrlRequired(enabled: boolean) {
    const trendzUrlControl = this.trendzSettingsForm.get('trendzUrl')!;

    if (enabled) {
      trendzUrlControl.addValidators(Validators.required);
    } else {
      trendzUrlControl.removeValidators(Validators.required);
    }

    trendzUrlControl.updateValueAndValidity();
  }

  setTrendzSettings(trendzSettings: TrendzSettings) {
    this.trendzSettingsForm.reset({
      isTrendzEnabled: trendzSettings?.enabled ?? false,
      trendzUrl: trendzSettings?.baseUrl,
      apiKey: trendzSettings?.apiKey
    });

    this.toggleUrlRequired(this.trendzSettingsForm.get('isTrendzEnabled').value);
  }

  confirmForm(): FormGroup {
    return this.trendzSettingsForm;
  }

  save(): void {
    const isTrendzEnabled =   this.trendzSettingsForm.get('isTrendzEnabled').value;
    const trendzUrl = this.trendzSettingsForm.get('trendzUrl').value;
    const apiKey = this.trendzSettingsForm.get('apiKey').value.trim();

    const trendzSettings: TrendzSettings = {
      enabled: isTrendzEnabled,
      baseUrl: trendzUrl,
      apiKey: apiKey
    };

    this.trendzSettingsService.saveTrendzSettings(trendzSettings)
      .subscribe(() => {
        this.setTrendzSettings(trendzSettings);
      })
  }
}
