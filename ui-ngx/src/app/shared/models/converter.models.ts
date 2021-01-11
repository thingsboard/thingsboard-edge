///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { ConverterId } from '@shared/models/id/converter-id';
import { ContentType } from '@shared/models/constants';

export enum ConverterType {
  UPLINK = 'UPLINK',
  DOWNLINK = 'DOWNLINK'
}

export const converterTypeTranslationMap = new Map<ConverterType, string>(
  [
    [ConverterType.UPLINK, 'converter.type-uplink'],
    [ConverterType.DOWNLINK, 'converter.type-downlink'],
  ]
);

export interface Converter extends BaseData<ConverterId> {
  tenantId?: TenantId;
  name: string;
  type: ConverterType;
  debugMode: boolean;
  configuration: any;
  additionalInfo?: any;
}

export interface TestUpLinkInputParams {
  metadata: {[key: string]: string};
  payload: string;
  decoder: string;
}

export interface TestDownLinkInputParams {
  metadata: {[key: string]: string};
  msg: string;
  msgType: string;
  integrationMetadata: {[key: string]: string};
  encoder: string;
}

export type TestConverterInputParams = TestUpLinkInputParams & TestDownLinkInputParams;

export interface TestConverterResult {
  output: string;
  error: string;
}

export interface ConverterDebugInput {
  inContentType: ContentType;
  inContent: string;
  inMetadata: string;
  inMsgType: string;
  inIntegrationMetadata: string;
}

export function getConverterHelpLink (converter: Converter) {
  let link = 'converters';
  if (converter && converter.type) {
    if (converter.type === ConverterType.UPLINK) {
      link = 'uplinkConverters';
    } else {
      link = 'downlinkConverters';
    }
  }
  return link;
}
