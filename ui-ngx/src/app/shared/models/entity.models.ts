///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { BaseData } from '@shared/models/base-data';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { AttributeData } from './telemetry/telemetry.models';

export interface EntityInfo {
  origEntity?: BaseData<EntityId>;
  name?: string;
  label?: string;
  entityType?: EntityType;
  id?: string;
  entityDescription?: string;
}

export interface ImportEntityData {
  name: string;
  type: string;
  label: string;
  gateway: boolean;
  description: string;
  accessToken: string;
  attributes: {
    server: AttributeData[],
    shared: AttributeData[]
  };
  timeseries: AttributeData[];
}

export interface ImportEntitiesResultInfo {
  create?: {
    entity: number;
  };
  update?: {
    entity: number;
  };
  error?: {
    entity: number;
  };
}

export interface EntityField {
  keyName: string;
  value: string;
  name: string;
  time?: boolean;
}

export const entityFields: {[fieldName: string]: EntityField} = {
  createdTime: {
    keyName: 'createdTime',
    name: 'entity-field.created-time',
    value: 'createdTime',
    time: true
  },
  name: {
    keyName: 'name',
    name: 'entity-field.name',
    value: 'name'
  },
  type: {
    keyName: 'type',
    name: 'entity-field.type',
    value: 'type'
  },
  firstName: {
    keyName: 'firstName',
    name: 'entity-field.first-name',
    value: 'firstName'
  },
  lastName: {
    keyName: 'lastName',
    name: 'entity-field.last-name',
    value: 'lastName'
  },
  email: {
    keyName: 'email',
    name: 'entity-field.email',
    value: 'email'
  },
  title: {
    keyName: 'title',
    name: 'entity-field.title',
    value: 'title'
  },
  country: {
    keyName: 'country',
    name: 'entity-field.country',
    value: 'country'
  },
  state: {
    keyName: 'state',
    name: 'entity-field.state',
    value: 'state'
  },
  city: {
    keyName: 'city',
    name: 'entity-field.city',
    value: 'city'
  },
  address: {
    keyName: 'address',
    name: 'entity-field.address',
    value: 'address'
  },
  address2: {
    keyName: 'address2',
    name: 'entity-field.address2',
    value: 'address2'
  },
  zip: {
    keyName: 'zip',
    name: 'entity-field.zip',
    value: 'zip'
  },
  phone: {
    keyName: 'phone',
    name: 'entity-field.phone',
    value: 'phone'
  },
  label: {
    keyName: 'label',
    name: 'entity-field.label',
    value: 'label'
  }
};
