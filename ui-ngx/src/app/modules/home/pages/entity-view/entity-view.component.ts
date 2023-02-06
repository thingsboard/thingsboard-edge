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
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@app/shared/models/id/entity-id';
import { EntityView } from '@shared/models/entity-view.models';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';

@Component({
  selector: 'tb-entity-view',
  templateUrl: './entity-view.component.html',
  styleUrls: ['./entity-view.component.scss']
})
export class EntityViewComponent extends GroupEntityComponent<EntityView> {

  entityType = EntityType;

  dataKeyType = DataKeyType;

  // entityViewScope: 'tenant' | 'customer' | 'customer_user';

  allowedEntityTypes = [EntityType.DEVICE, EntityType.ASSET];

  maxStartTimeMs: Observable<number | null>;
  minEndTimeMs: Observable<number | null>;

  selectedEntityId: Observable<EntityId | null>;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: EntityView,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: GroupEntityTableConfig<EntityView>,
              protected fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    // this.entityViewScope = this.entitiesTableConfig.componentsData.entityViewScope;
    super.ngOnInit();
    this.maxStartTimeMs = this.entityForm.get('endTimeMs').valueChanges;
    this.minEndTimeMs = this.entityForm.get('startTimeMs').valueChanges;
    this.selectedEntityId = this.entityForm.get('entityId').valueChanges;
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  /* isAssignedToCustomer(entity: EntityView): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  } */

  buildForm(entity: EntityView): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        type: [entity ? entity.type : null, Validators.required],
        entityId: [entity ? entity.entityId : null, [Validators.required]],
        startTimeMs: [entity ? entity.startTimeMs : null],
        endTimeMs: [entity ? entity.endTimeMs : null],
        keys: this.fb.group(
          {
            attributes: this.fb.group(
              {
                cs: [entity && entity.keys && entity.keys.attributes ? entity.keys.attributes.cs : null],
                sh: [entity && entity.keys && entity.keys.attributes ? entity.keys.attributes.sh : null],
                ss: [entity && entity.keys && entity.keys.attributes ? entity.keys.attributes.ss : null],
              }
            ),
            timeseries: [entity && entity.keys && entity.keys.timeseries ? entity.keys.timeseries : null]
          }
        ),
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
  }

  updateForm(entity: EntityView) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type});
    this.entityForm.patchValue({entityId: entity.entityId});
    this.entityForm.patchValue({startTimeMs: entity.startTimeMs});
    this.entityForm.patchValue({endTimeMs: entity.endTimeMs});
    this.entityForm.patchValue({
      keys:
        {
          attributes: {
            cs: entity.keys && entity.keys.attributes ? entity.keys.attributes.cs : null,
            sh: entity.keys && entity.keys.attributes ? entity.keys.attributes.sh : null,
            ss: entity.keys && entity.keys.attributes ? entity.keys.attributes.ss : null,
          },
          timeseries: entity.keys && entity.keys.timeseries ? entity.keys.timeseries : null
        }
    });
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
  }


  onEntityViewIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('entity-view.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
