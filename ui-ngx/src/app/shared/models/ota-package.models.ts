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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { OtaPackageId } from '@shared/models/id/ota-package-id';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';
import { EntityGroupId } from '@shared/models/id/entity-group-id';

export enum ChecksumAlgorithm {
  MD5 = 'MD5',
  SHA256 = 'SHA256',
  SHA384 = 'SHA384',
  SHA512 = 'SHA512',
  CRC32 = 'CRC32',
  MURMUR3_32 = 'MURMUR3_32',
  MURMUR3_128 = 'MURMUR3_128'
}

export const ChecksumAlgorithmTranslationMap = new Map<ChecksumAlgorithm, string>(
  [
    [ChecksumAlgorithm.MD5, 'MD5'],
    [ChecksumAlgorithm.SHA256, 'SHA-256'],
    [ChecksumAlgorithm.SHA384, 'SHA-384'],
    [ChecksumAlgorithm.SHA512, 'SHA-512'],
    [ChecksumAlgorithm.CRC32, 'CRC-32'],
    [ChecksumAlgorithm.MURMUR3_32, 'MURMUR3-32'],
    [ChecksumAlgorithm.MURMUR3_128, 'MURMUR3-128']
  ]
);

export enum OtaUpdateType {
  FIRMWARE = 'FIRMWARE',
  SOFTWARE = 'SOFTWARE'
}

export const OtaUpdateTypeTranslationMap = new Map<OtaUpdateType, string>(
  [
    [OtaUpdateType.FIRMWARE, 'ota-update.types.firmware'],
    [OtaUpdateType.SOFTWARE, 'ota-update.types.software']
  ]
);

export interface OtaUpdateTranslation {
  label: string;
  required: string;
  noFound: string;
  noMatching: string;
  hint: string;
}

export const OtaUpdateTranslation = new Map<OtaUpdateType, OtaUpdateTranslation>(
  [
    [OtaUpdateType.FIRMWARE, {
      label: 'ota-update.assign-firmware',
      required: 'ota-update.assign-firmware-required',
      noFound: 'ota-update.no-firmware-text',
      noMatching: 'ota-update.no-firmware-matching',
      hint: 'ota-update.chose-firmware-distributed-device'
    }],
    [OtaUpdateType.SOFTWARE, {
      label: 'ota-update.assign-software',
      required: 'ota-update.assign-software-required',
      noFound: 'ota-update.no-software-text',
      noMatching: 'ota-update.no-software-matching',
      hint: 'ota-update.chose-software-distributed-device'
    }]
  ]
);

export interface OtaPagesIds {
  firmwareId?: OtaPackageId;
  softwareId?: OtaPackageId;
}

export interface OtaPackageInfo extends BaseData<OtaPackageId> {
  tenantId?: TenantId;
  type: OtaUpdateType;
  deviceProfileId?: DeviceProfileId;
  title?: string;
  version?: string;
  tag?: string;
  hasData?: boolean;
  url?: string;
  fileName: string;
  checksum?: string;
  checksumAlgorithm?: ChecksumAlgorithm;
  contentType: string;
  dataSize?: number;
  additionalInfo?: any;
  isURL?: boolean;
}

export interface OtaPackage extends OtaPackageInfo {
  file?: File;
  data: string;
}

export interface DeviceGroupOtaPackage {
  otaPackageId: OtaPackageId;
  otaPackageType: OtaUpdateType;
  otaPackageUpdateTime?: number;
  groupId: EntityGroupId;
  id?: string;
}
