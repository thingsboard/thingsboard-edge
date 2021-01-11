///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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
import { EntityType } from '@shared/models/entity-type.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { Asset } from '@shared/models/asset.models';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';


@Component({
  selector: 'tb-asset',
  templateUrl: './asset.component.html',
  styleUrls: ['./asset.component.scss']
})
export class AssetComponent extends GroupEntityComponent<Asset> {

  entityType = EntityType;

  // assetScope: 'tenant' | 'customer' | 'customer_user';

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Asset,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: GroupEntityTableConfig<Asset>,
              protected fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  ngOnInit() {
    // this.assetScope = this.entitiesTableConfig.componentsData.assetScope;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideAssignmentActions() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.assignmentEnabled(this.entity);
    } else {
      return false;
    }
  }

  /* isAssignedToCustomer(entity: Asset): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  } */

  buildForm(entity: Asset): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        type: [entity ? entity.type : null, [Validators.required]],
        label: [entity ? entity.label : ''],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
  }

  updateForm(entity: Asset) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type});
    this.entityForm.patchValue({label: entity.label});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
  }


  onAssetIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('asset.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
