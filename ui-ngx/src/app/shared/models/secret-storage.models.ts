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

import { BaseData, HasId } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { SecretStorageId } from '@shared/models/id/secret-storage-id';
import { ResourceReferences, TbResourceInfo } from '@shared/models/resource.models';
import { isNotEmptyStr } from '@core/utils';
import { WhiteLabeling } from '@shared/models/white-labeling.models';

export interface SecretStorage extends BaseData<SecretStorageId> {
  tenantId?: TenantId;
  name: string;
  type: SecretStorageType;
  description?: any;
}

export interface SecretStorageInfo extends SecretStorage {
  value: string;
}

export enum SecretStorageType {
  TEXT = 'TEXT',
  TEXT_FILE = 'TEXT_FILE'
}

export const secretStorageTypeTranslationMap = new Map<SecretStorageType, string>(
  [
    [SecretStorageType.TEXT, 'secret-storage.types.text'],
    [SecretStorageType.TEXT_FILE, 'secret-storage.types.file'],
  ]
);

export const secretStorageTypeDialogTitleTranslationMap = new Map<SecretStorageType, string>(
  [
    [SecretStorageType.TEXT, 'secret-storage.dialog-title.text'],
    [SecretStorageType.TEXT_FILE, 'secret-storage.dialog-title.file'],
  ]
);

export const secretStorageCreateTitleTranslationMap = new Map<SecretStorageType, string>(
  [
    [SecretStorageType.TEXT, 'secret-storage.create.text'],
    [SecretStorageType.TEXT_FILE, 'secret-storage.create.file'],
  ]
);

export interface SecretWithReferences extends SecretStorage {
  references: any;
}

export interface SecretDeleteResult {
  resource: SecretStorage;
  success: boolean;
  resourceIsReferencedError?: boolean;
  error?: any;
  references?: ResourceReferences;
}

export type SecretResourceInfo = TbResourceInfo<SecretStorage>;

export const toSecretDeleteResult = (resource: SecretStorage, e?: any): SecretDeleteResult => {
  if (!e) {
    return {resource, success: true};
  } else {
    const result: SecretDeleteResult = {resource, success: false, error: e};
    if (e?.status === 400 && e?.error?.success === false && e?.error?.references) {
      const entityReferences: {[entityType: string]: Array<BaseData<HasId>>} = e?.error?.references;
      const whiteLabelingList: Array<WhiteLabeling> = e?.error?.whiteLabelingList;
      const references: ResourceReferences = [];
      if (entityReferences) {
        for (const entityTypeStr of Object.keys(entityReferences)) {
          const entities = entityReferences[entityTypeStr];
          references.push.apply(references, entities);
        }
      }
      if (whiteLabelingList) {
        references.push.apply(references, whiteLabelingList);
      }
      result.resourceIsReferencedError = true;
      result.references = references;
    }
    return result;
  }
};

export const  parseSecret = (str: string) => {
  if (isNotEmptyStr(str)) {
    const regex = /^\${secret:([^;]+);type:[^}]+}$/;
    const match = str.match(regex);
    return match ? match[1] : null;
  }
  return null;
}

