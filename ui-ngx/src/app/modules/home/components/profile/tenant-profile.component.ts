///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  createTenantProfileConfiguration,
  TenantProfile,
  TenantProfileData,
  TenantProfileType
} from '@shared/models/tenant.model';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityComponent } from '../entity/entity.component';

@Component({
  selector: 'tb-tenant-profile',
  templateUrl: './tenant-profile.component.html',
  styleUrls: ['./tenant-profile.component.scss']
})
export class TenantProfileComponent extends EntityComponent<TenantProfile> {

  @Input()
  standalone = false;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: TenantProfile,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<TenantProfile>,
              protected fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: TenantProfile): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        isolatedTbCore: [entity ? entity.isolatedTbCore : false, []],
        isolatedTbRuleEngine: [entity ? entity.isolatedTbRuleEngine : false, []],
        profileData: [entity && !this.isAdd ? entity.profileData : {
          configuration: createTenantProfileConfiguration(TenantProfileType.DEFAULT)
        } as TenantProfileData, []],
        description: [entity ? entity.description : '', []],
      }
    );
  }

  updateForm(entity: TenantProfile) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({isolatedTbCore: entity.isolatedTbCore});
    this.entityForm.patchValue({isolatedTbRuleEngine: entity.isolatedTbRuleEngine});
    this.entityForm.patchValue({profileData: !this.isAdd ? entity.profileData : {
        configuration: createTenantProfileConfiguration(TenantProfileType.DEFAULT)
      } as TenantProfileData});
    this.entityForm.patchValue({description: entity.description});
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
        if (!this.isAdd) {
          this.entityForm.get('isolatedTbCore').disable({emitEvent: false});
          this.entityForm.get('isolatedTbRuleEngine').disable({emitEvent: false});
        }
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }

  onTenantProfileIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('tenant-profile.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
