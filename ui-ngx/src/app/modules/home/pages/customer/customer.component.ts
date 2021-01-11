///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Customer } from '@shared/models/customer.model';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { GroupContactBasedComponent } from '@home/components/group/group-contact-based.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { isDefined } from '@core/utils';

@Component({
  selector: 'tb-customer',
  templateUrl: './customer.component.html'
})
export class CustomerComponent extends GroupContactBasedComponent<Customer> {

  isPublic = false;

  allowCustomerWhiteLabeling = getCurrentAuthState(this.store).customerWhiteLabelingAllowed;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Customer,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: GroupEntityTableConfig<Customer>,
              protected fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageUsers() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.manageUsersEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageCustomers() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.manageCustomersEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageAssets() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.manageAssetsEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageDevices() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.manageDevicesEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageEntityViews() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.manageEntityViewsEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageDashboards() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.manageDashboardsEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildEntityForm(entity: Customer): FormGroup {
    return this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required]],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
            allowWhiteLabeling: [entity && entity.additionalInfo
            && isDefined(entity.additionalInfo.allowWhiteLabeling) ? entity.additionalInfo.allowWhiteLabeling : true],
          }
        )
      }
    );
  }

  updateEntityForm(entity: Customer) {
    this.isPublic = entity.additionalInfo && entity.additionalInfo.isPublic;
    this.entityForm.patchValue({title: entity.title});
    this.entityForm.patchValue({additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : '',
        allowWhiteLabeling: entity.additionalInfo
        && isDefined(entity.additionalInfo.allowWhiteLabeling) ? entity.additionalInfo.allowWhiteLabeling : true
      }});
  }

  onCustomerIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('customer.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
