///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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


import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { EntityComponent } from '@home/components/entity/entity.component';
import { MobileAppInfo } from '@shared/models/oauth2.models';
import { AppState } from '@core/core.state';
import { OAuth2Service } from '@core/http/oauth2.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { WINDOW } from '@core/services/window.service';
import { isDefinedAndNotNull, randomAlphanumeric } from '@core/utils';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { ClientDialogComponent } from '@home/pages/admin/oauth2/clients/client-dialog.component';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-mobile-app',
  templateUrl: './mobile-app.component.html',
  styleUrls: []
})
export class MobileAppComponent extends EntityComponent<MobileAppInfo> {

  entityType = EntityType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private oauth2Service: OAuth2Service,
              @Inject('entity') protected entityValue: MobileAppInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<MobileAppInfo>,
              protected cd: ChangeDetectorRef,
              public fb: UntypedFormBuilder,
              @Inject(WINDOW) private window: Window,
              private dialog: MatDialog) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  buildForm(entity: MobileAppInfo): UntypedFormGroup {
    return this.fb.group({
      pkgName: [entity?.pkgName ? entity.pkgName : '', [Validators.required]],
      appSecret: [entity?.appSecret ? entity.appSecret : btoa(randomAlphanumeric(64)),
        [Validators.required, this.base64Format]],
      oauth2Enabled: isDefinedAndNotNull(entity?.oauth2Enabled) ? entity.oauth2Enabled : true,
      oauth2ClientInfos: entity?.oauth2ClientInfos ? entity.oauth2ClientInfos.map(info => info.id.id) : []
    });
  }

  updateForm(entity: MobileAppInfo) {
    this.entityForm.patchValue({
      pkgName: entity.pkgName,
      appSecret: entity.appSecret,
      oauth2Enabled: entity.oauth2Enabled,
      oauth2ClientInfos: entity.oauth2ClientInfos?.map(info => info.id ? info.id.id : info)
    })
  }

  createClient($event: Event, button: MatButton) {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.dialog.open<ClientDialogComponent>(ClientDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }).afterClosed()
      .subscribe((client) => {
        if (client) {
          const formValue = this.entityForm.get('oauth2ClientInfos').value ?
            [...this.entityForm.get('oauth2ClientInfos').value] : [];
          formValue.push(client.id.id);
          this.entityForm.get('oauth2ClientInfos').patchValue(formValue);
          this.entityForm.get('oauth2ClientInfos').markAsDirty();
        }
      });
  }

  private base64Format(control: UntypedFormControl): { [key: string]: boolean } | null {
    if (control.value === '') {
      return null;
    }
    try {
      const value = atob(control.value);
      if (value.length < 64) {
        return {minLength: true};
      }
      return null;
    } catch (e) {
      return {base64: true};
    }
  }

}
