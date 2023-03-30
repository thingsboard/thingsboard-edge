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

import { BaseData, ExportableEntity, GroupEntityInfo } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntityViewId } from '@shared/models/id/entity-view-id';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitySearchQuery } from '@shared/models/relation.models';
import { EntityInfoData } from '@shared/models/entity.models';

export interface AttributesEntityView {
  cs: Array<string>;
  ss: Array<string>;
  sh: Array<string>;
}

export interface TelemetryEntityView {
  timeseries: Array<string>;
  attributes: AttributesEntityView;
}

export interface EntityView extends BaseData<EntityViewId>, ExportableEntity<EntityViewId> {
  tenantId: TenantId;
  customerId: CustomerId;
  entityId: EntityId;
  name: string;
  type: string;
  keys: TelemetryEntityView;
  startTimeMs: number;
  endTimeMs: number;
  additionalInfo?: any;
}

export type EntityViewInfo = EntityView & GroupEntityInfo<EntityViewId>;

export interface EntityViewSearchQuery extends EntitySearchQuery {
  entityViewTypes: Array<string>;
}
