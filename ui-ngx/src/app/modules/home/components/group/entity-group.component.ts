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
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { Operation, publicGroupTypes, Resource, sharableGroupTypes } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { OtaUpdateType } from '@shared/models/ota-package.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupsTableConfig } from '@home/components/group/entity-groups-table-config';

@Component({
  selector: 'tb-entity-group',
  templateUrl: './entity-group.component.html',
  styleUrls: ['./entity-group.component.scss']
})
export class EntityGroupComponent extends EntityComponent<EntityGroupInfo> {

  isPublic = false;
  shareEnabled = false;
  makePublicEnabled = false;
  makePrivateEnabled = false;
  isGroupAll = false;
  packageTypes = OtaUpdateType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              protected userPermissionsService: UserPermissionsService,
              @Inject('entity') protected entityValue: EntityGroupInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<EntityGroupInfo>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideOpen() {
    if (this.entitiesTableConfig) {
      return this.entitiesTableConfig.componentsData.isGroupEntitiesView;
    } else {
      return false;
    }
  }

  hideUnassign() {
    if (this.entitiesTableConfig) {
      return this.entitiesTableConfig.componentsData.isUnassignEnabled;
    } else {
      return false;
    }
  }

  buildForm(entity: EntityGroupInfo): UntypedFormGroup {
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
    this.updateGroupParams(entity);
    if ((this.entitiesTableConfig as EntityGroupsTableConfig).groupType === EntityType.DEVICE) {
      form.addControl('firmwareId', this.fb.control(entity ? entity.firmwareId : ''));
      form.addControl('softwareId', this.fb.control(entity ? entity.softwareId : ''));
    }
    return form;
  }

  updateForm(entity: EntityGroupInfo) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
    if (entity.type === EntityType.DEVICE) {
      this.entityForm.patchValue({
        firmwareId: entity.firmwareId,
        softwareId: entity.softwareId
      }, {emitEvent: false});
    }
    this.updateGroupParams(entity);
  }

  private updateGroupParams(entityGroup: EntityGroupInfo) {
    if (entityGroup) {
      if (entityGroup.id) {
        const isPublicGroupType = publicGroupTypes.has(entityGroup.type);
        const isSharableGroupType = sharableGroupTypes.has(entityGroup.type);
        const isPublic: boolean = entityGroup.additionalInfo?.isPublic;
        const isOwned = this.userPermissionsService.isDirectlyOwnedGroup(entityGroup);
        const isWriteAllowed = this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entityGroup);
        const isCreatePermissionAllowed = this.userPermissionsService.hasGenericPermission(Resource.GROUP_PERMISSION, Operation.CREATE);
        this.isPublic = isPublic;
        this.shareEnabled = isSharableGroupType && isCreatePermissionAllowed && isWriteAllowed;
        this.makePublicEnabled = isPublicGroupType && !isPublic && isOwned && isWriteAllowed;
        this.makePrivateEnabled = isPublicGroupType && isPublic && isOwned && isWriteAllowed;
        this.isGroupAll = entityGroup.groupAll;
      } else {
        this.isPublic = false;
        this.shareEnabled = false;
        this.makePublicEnabled = false;
        this.makePrivateEnabled = false;
        this.isGroupAll = false;
      }
    }
  }

  onEntityGroupIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('entity-group.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
