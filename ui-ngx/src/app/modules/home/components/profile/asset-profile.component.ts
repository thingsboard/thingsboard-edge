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

import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityComponent } from '../entity/entity.component';
import { EntityType } from '@shared/models/entity-type.models';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { ServiceType } from '@shared/models/queue.models';
import { EntityId } from '@shared/models/id/entity-id';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { AssetProfile, TB_SERVICE_QUEUE } from '@shared/models/asset.models';
import { RuleChainType } from '@shared/models/rule-chain.models';

@Component({
  selector: 'tb-asset-profile',
  templateUrl: './asset-profile.component.html',
  styleUrls: []
})
export class AssetProfileComponent extends EntityComponent<AssetProfile> {

  @Input()
  standalone = false;

  entityType = EntityType;

  serviceType = ServiceType.TB_RULE_ENGINE;

  edgeRuleChainType = RuleChainType.EDGE;

  TB_SERVICE_QUEUE = TB_SERVICE_QUEUE;

  assetProfileId: EntityId;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: AssetProfile,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AssetProfile>,
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

  buildForm(entity: AssetProfile): UntypedFormGroup {
    this.assetProfileId = entity?.id ? entity.id : null;
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        image: [entity ? entity.image : null],
        defaultRuleChainId: [entity && entity.defaultRuleChainId ? entity.defaultRuleChainId.id : null, []],
        defaultDashboardId: [entity && entity.defaultDashboardId ? entity.defaultDashboardId.id : null, []],
        defaultQueueName: [entity ? entity.defaultQueueName : null, []],
        defaultEdgeRuleChainId: [entity && entity.defaultEdgeRuleChainId ? entity.defaultEdgeRuleChainId.id : null, []],
        description: [entity ? entity.description : '', []],
      }
    );
    return form;
  }

  updateForm(entity: AssetProfile) {
    this.assetProfileId = entity.id;
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({image: entity.image}, {emitEvent: false});
    this.entityForm.patchValue({defaultRuleChainId: entity.defaultRuleChainId ? entity.defaultRuleChainId.id : null}, {emitEvent: false});
    this.entityForm.patchValue({defaultDashboardId: entity.defaultDashboardId ? entity.defaultDashboardId.id : null}, {emitEvent: false});
    this.entityForm.patchValue({defaultQueueName: entity.defaultQueueName}, {emitEvent: false});
    this.entityForm.patchValue({defaultEdgeRuleChainId: entity.defaultEdgeRuleChainId ? entity.defaultEdgeRuleChainId.id : null}, {emitEvent: false});
    this.entityForm.patchValue({description: entity.description}, {emitEvent: false});
  }

  prepareFormValue(formValue: any): any {
    if (formValue.defaultRuleChainId) {
      formValue.defaultRuleChainId = new RuleChainId(formValue.defaultRuleChainId);
    }
    if (formValue.defaultDashboardId) {
      formValue.defaultDashboardId = new DashboardId(formValue.defaultDashboardId);
    }
    if (formValue.defaultEdgeRuleChainId) {
      formValue.defaultEdgeRuleChainId = new RuleChainId(formValue.defaultEdgeRuleChainId);
    }
    return super.prepareFormValue(formValue);
  }

  onAssetProfileIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('asset-profile.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
