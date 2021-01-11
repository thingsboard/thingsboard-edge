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

import { Directive } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormBuilder } from '@angular/forms';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { PageLink } from '@shared/models/page/page-link';
import { ShortEntityView } from '@shared/models/entity-group.models';
import { BaseData, HasId } from '@shared/models/base-data';

// @dynamic
@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class GroupEntityComponent<T extends BaseData<HasId>>
  extends EntityComponent<T, PageLink, ShortEntityView, GroupEntityTableConfig<T>> {

  entityGroup = this.entitiesTableConfig?.entityGroup;

  constructor(protected store: Store<AppState>,
              protected fb: FormBuilder,
              protected entityValue: T,
              protected entitiesTableConfigValue: GroupEntityTableConfig<T>) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  protected setEntitiesTableConfig(entitiesTableConfig: GroupEntityTableConfig<T>) {
    super.setEntitiesTableConfig(entitiesTableConfig);
    if (entitiesTableConfig) {
      this.entityGroup = entitiesTableConfig.entityGroup;
    }
  }

}
