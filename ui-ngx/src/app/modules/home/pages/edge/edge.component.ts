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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { generateSecret, guid, isDefinedAndNotNull } from '@core/utils';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { Edge } from '@shared/models/edge.models';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';

@Component({
  selector: 'tb-edge',
  templateUrl: './edge.component.html',
  styleUrls: ['./edge.component.scss']
})
export class EdgeComponent extends GroupEntityComponent<Edge> {

  entityType = EntityType;

  // edgeScope: 'tenant' | 'customer' | 'customer_user';

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              @Inject('entity') protected entityValue: Edge,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: GroupEntityTableConfig<Edge>,
              public fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    // this.edgeScope = this.entitiesTableConfig.componentsData.edgeScope;
    this.entityForm.patchValue({
      cloudEndpoint: window.location.origin
    });
    super.ngOnInit();
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

  hideManageSchedulerEvents() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.manageSchedulerEventsEnabled(this.entity);
    } else {
      return false;
    }
  }

  /* isAssignedToCustomer(entity: EdgeInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  } */

  buildForm(entity: Edge): FormGroup {
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        type: [entity?.type ? entity.type : 'default', [Validators.required, Validators.maxLength(255)]],
        label: [entity ? entity.label : '', Validators.maxLength(255)],
        cloudEndpoint: [null, [Validators.required, Validators.maxLength(255)]],
        edgeLicenseKey: ['', [Validators.required, Validators.maxLength(30)]],
        routingKey: this.fb.control({value: entity ? entity.routingKey : null, disabled: true}),
        secret: this.fb.control({value: entity ? entity.secret : null, disabled: true}),
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : '']
          }
        )
      }
    );
    this.generateRoutingKeyAndSecret(entity, form);
    return form;
  }

  updateForm(entity: Edge) {
    this.entityForm.patchValue({
      name: entity.name,
      type: entity.type,
      label: entity.label,
      cloudEndpoint: entity.cloudEndpoint ? entity.cloudEndpoint : window.location.origin,
      edgeLicenseKey: entity.edgeLicenseKey,
      routingKey: entity.routingKey,
      secret: entity.secret,
      additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : ''
      }
    });
    this.generateRoutingKeyAndSecret(entity, this.entityForm);
  }

  updateFormState() {
    super.updateFormState();
    this.entityForm.get('routingKey').disable({ emitEvent: false });
    this.entityForm.get('secret').disable({ emitEvent: false });
  }

  onEdgeIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('edge.id-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  onEdgeInfoCopied(type: string) {
    const message = type === 'key' ? 'edge.edge-key-copied-message'
      : 'edge.edge-secret-copied-message';
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant(message),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  isTenantAdmin(): boolean {
    const authUser: AuthUser = getCurrentAuthUser(this.store);
    return authUser.authority === Authority.TENANT_ADMIN;
  }

  private generateRoutingKeyAndSecret(entity: Edge, form: FormGroup) {
    if (entity && (!entity.id || (entity.id && !entity.id.id))) {
      form.get('routingKey').patchValue(guid(), { emitEvent: false });
      form.get('secret').patchValue(generateSecret(20), { emitEvent: false });
    }
  }
}
