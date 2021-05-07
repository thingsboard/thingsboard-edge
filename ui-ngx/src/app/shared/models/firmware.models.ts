///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import { FirmwareId } from '@shared/models/id/firmware-id';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';

export enum ChecksumAlgorithm {
  MD5 = 'md5',
  SHA256 = 'sha256',
  CRC32 = 'crc32'
}

export const ChecksumAlgorithmTranslationMap = new Map<ChecksumAlgorithm, string>(
  [
    [ChecksumAlgorithm.MD5, 'MD5'],
    [ChecksumAlgorithm.SHA256, 'SHA-256'],
    [ChecksumAlgorithm.CRC32, 'CRC-32']
  ]
);

export enum FirmwareType {
  FIRMWARE = 'FIRMWARE',
  SOFTWARE = 'SOFTWARE'
}

export const FirmwareTypeTranslationMap = new Map<FirmwareType, string>(
  [
    [FirmwareType.FIRMWARE, 'firmware.types.firmware'],
    [FirmwareType.SOFTWARE, 'firmware.types.software']
  ]
);

export interface FirmwareInfo extends BaseData<FirmwareId> {
  tenantId?: TenantId;
  type: FirmwareType;
  deviceProfileId?: DeviceProfileId;
  title?: string;
  version?: string;
  hasData?: boolean;
  fileName: string;
  checksum?: string;
  checksumAlgorithm?: ChecksumAlgorithm;
  contentType: string;
  dataSize?: number;
  additionalInfo?: any;
}

export interface Firmware extends FirmwareInfo {
  file?: File;
  data: string;
}
