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

import { BaseData, HasId } from '@shared/models/base-data';
import { PageComponent } from '@shared/components/page.component';
import { AfterViewInit, Directive, Input, OnInit, QueryList, ViewChildren } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { MatTab } from '@angular/material/tabs';
import { BehaviorSubject } from 'rxjs';
import { Authority } from '@app/shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { EntityType } from '@shared/models/entity-type.models';
import { AuditLogMode } from '@shared/models/audit-log.models';
import { DebugEventType, EventType } from '@shared/models/event.models';
import { AttributeScope, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { UntypedFormGroup } from '@angular/forms';
import { PageLink } from '@shared/models/page/page-link';

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class EntityTabsComponent<T extends BaseData<HasId>,
  P extends PageLink = PageLink,
  L extends BaseData<HasId> = T,
  C extends EntityTableConfig<T, P, L> = EntityTableConfig<T, P, L>>
  extends PageComponent implements OnInit, AfterViewInit {

  attributeScopes = AttributeScope;
  latestTelemetryTypes = LatestTelemetry;

  authorities = Authority;

  entityTypes = EntityType;

  auditLogModes = AuditLogMode;

  eventTypes = EventType;

  debugEventTypes = DebugEventType;

  authUser: AuthUser;

  nullUid = NULL_UUID;

  entityValue: T;

  entitiesTableConfigValue: C;

  @ViewChildren(MatTab) entityTabs: QueryList<MatTab>;

  isEditValue: boolean;

  @Input()
  set isEdit(isEdit: boolean) {
    this.isEditValue = isEdit;
  }

  get isEdit() {
    return this.isEditValue;
  }

  @Input()
  set entity(entity: T) {
    this.setEntity(entity);
  }

  get entity(): T {
    return this.entityValue;
  }

  @Input()
  set entitiesTableConfig(entitiesTableConfig: C) {
    this.setEntitiesTableConfig(entitiesTableConfig);
  }

  get entitiesTableConfig(): C {
    return this.entitiesTableConfigValue;
  }

  @Input()
  detailsForm: UntypedFormGroup;

  private entityTabsSubject = new BehaviorSubject<Array<MatTab>>(null);

  entityTabsChanged = this.entityTabsSubject.asObservable();

  protected constructor(protected store: Store<AppState>) {
    super(store);
    this.authUser = getCurrentAuthUser(store);
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
    this.entityTabsSubject.next(this.entityTabs.toArray());
    this.entityTabs.changes.subscribe(
      () => {
        this.entityTabsSubject.next(this.entityTabs.toArray());
      }
    );
  }

  protected setEntity(entity: T) {
    this.entityValue = entity;
  }

  protected setEntitiesTableConfig(entitiesTableConfig: C) {
    this.entitiesTableConfigValue = entitiesTableConfig;
  }

}
