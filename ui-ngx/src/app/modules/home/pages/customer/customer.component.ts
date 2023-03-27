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

import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Customer } from '@shared/models/customer.model';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { isDefined, isDefinedAndNotNull } from '@core/utils';
import { GroupContactBasedComponent } from '@home/components/group/group-contact-based.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-customer',
  templateUrl: './customer.component.html',
  styleUrls: ['./customer.component.scss']
})
export class CustomerComponent extends GroupContactBasedComponent<Customer> {

  isPublic = false;

  allowCustomerWhiteLabeling = getCurrentAuthState(this.store).customerWhiteLabelingAllowed;
  whiteLabelingAllowed = getCurrentAuthState(this.store).whiteLabelingAllowed;
  edgesSupportEnabled = getCurrentAuthState(this.store).edgesSupportEnabled;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Customer,
              @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<Customer> | GroupEntityTableConfig<Customer>,
              protected fb: UntypedFormBuilder,
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

  hideManageUsers() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageUsersEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageCustomers() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageCustomersEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageAssets() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageAssetsEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageDevices() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageDevicesEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageEntityViews() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageEntityViewsEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageEdges() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageEdgesEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageDashboards() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageDashboardsEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildEntityForm(entity: Customer): UntypedFormGroup {
    return this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
            allowWhiteLabeling: [entity && entity.additionalInfo
            && isDefined(entity.additionalInfo.allowWhiteLabeling) ? entity.additionalInfo.allowWhiteLabeling : true],
            homeDashboardId: [entity && entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null],
            homeDashboardHideToolbar: [entity && entity.additionalInfo &&
            isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true]
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
        && isDefined(entity.additionalInfo.allowWhiteLabeling) ? entity.additionalInfo.allowWhiteLabeling : true,
        homeDashboardId: entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null,
        homeDashboardHideToolbar: entity.additionalInfo &&
        isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true
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
