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


import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
import { EntityComponent } from '@home/components/entity/entity.component';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { SecretStorage, SecretStorageInfo, SecretStorageType } from '@shared/models/secret-storage.models';

@Component({
  selector: 'tb-secret-storage',
  templateUrl: './secret-storage.component.html',
  styleUrls: []
})
export class SecretStorageComponent extends EntityComponent<SecretStorage> {

  @Input()
  hideType = false;

  @Input()
  fileName : string;

  entityType = EntityType;
  SecretStorageType = SecretStorageType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: SecretStorage,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<SecretStorage>,
              protected cd: ChangeDetectorRef,
              public fb: UntypedFormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  buildForm(): UntypedFormGroup {
    return this.fb.group({
      type: [SecretStorageType.TEXT, []],
      name: ['', [Validators.required]],
      description: ['', []],
      value: ['', [Validators.required]]
    });
  }

  updateForm(entity: SecretStorageInfo) {
    this.entityForm.patchValue({
      type: entity.type,
      name: entity.name,
      description: entity.description,
      value: entity.value
    });
  }

}
