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

import { EntityType } from '@shared/models/entity-type.models';
import { AttributeData } from './telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { DeviceCredentialMQTTBasic } from '@shared/models/device.models';
import { Lwm2mSecurityConfigModels } from '@shared/models/lwm2m-security-config.models';

export interface EntityInfo {
  name?: string;
  label?: string;
  entityType?: EntityType;
  id?: string;
  entityDescription?: string;
}

export interface EntityInfoData {
  id: EntityId;
  name: string;
}

export interface ImportEntityData {
  lineNumber: number;
  name: string;
  type: string;
  label: string;
  gateway: boolean;
  description: string;
  credential: {
    accessToken?: string;
    x509?: string;
    mqtt?: DeviceCredentialMQTTBasic;
    lwm2m?: Lwm2mSecurityConfigModels;
  };
  attributes: {
    server: AttributeData[],
    shared: AttributeData[]
  };
  timeseries: AttributeData[];
}

export interface EdgeImportEntityData extends ImportEntityData {
  secret: string;
  routingKey: string;
  cloudEndpoint: string;
  edgeLicenseKey: string;
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
    errors?: string;
  };
}

export interface EntityField {
  keyName: string;
  value: string;
  name: string;
  time?: boolean;
}

export interface EntitiesKeysByQuery {
  attribute: Array<string>;
  timeseries: Array<string>;
  entityTypes: EntityType[];
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
  },
  configuration: {
    keyName: 'configuration',
    name: 'entity-field.configuration',
    value: 'configuration'
  },
  schedule: {
    keyName: 'schedule',
    name: 'entity-field.schedule',
    value: 'schedule'
  },
  originatorId: {
    keyName: 'originatorId',
    name: 'entity-field.originatorId',
    value: 'originatorId'
  },
  originatorType: {
    keyName: 'originatorType',
    name: 'entity-field.originatorType',
    value: 'originatorType'
  }
};
