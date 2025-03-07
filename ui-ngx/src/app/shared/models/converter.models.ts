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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { ConverterId } from '@shared/models/id/converter-id';
import { ContentType } from '@shared/models/constants';
import { ActivatedRouteSnapshot } from '@angular/router';
import { IntegrationType } from '@shared/models/integration.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { HasEntityDebugSettings } from '@shared/models/entity.models';
import { EntityType } from '@shared/models/entity-type.models';

export enum ConverterType {
  UPLINK = 'UPLINK',
  DOWNLINK = 'DOWNLINK'
}

export const IntegrationTbelDefaultConvertersUrl = new Map<IntegrationType, string>([
  [IntegrationType.CHIRPSTACK, '/assets/converters/tbel-chirpstack-decoder.raw'],
  [IntegrationType.LORIOT, '/assets/converters/tbel-loriot-decoder.raw'],
  [IntegrationType.TTI,'/assets/converters/tbel-tti-decoder.raw'],
  [IntegrationType.TTN, '/assets/converters/tbel-ttn-decoder.raw'],
  [IntegrationType.SIGFOX, '/assets/converters/tbel-sigfox-decoder.raw'],
  [IntegrationType.AZURE_IOT_HUB, '/assets/converters/tbel-azure-decoder.raw'],
  [IntegrationType.AZURE_EVENT_HUB, '/assets/converters/tbel-azure-decoder.raw'],
  [IntegrationType.AZURE_SERVICE_BUS, '/assets/converters/tbel-azure-decoder.raw'],
  [IntegrationType.AWS_IOT, '/assets/converters/tbel-aws-iot-decoder.raw'],
  [IntegrationType.KPN, '/assets/converters/tbel-kpn-decoder.raw'],
  [IntegrationType.THINGPARK, '/assets/converters/tbel-thingspark-decoder.raw'],
  [IntegrationType.TPE, '/assets/converters/tbel-tpe-decoder.raw']
]);

export const jsDefaultConvertersUrl = new Map<ConverterType, string>([
  [ConverterType.UPLINK, '/assets/converters/js-decoder.raw' ],
  [ConverterType.DOWNLINK, '/assets/converters/js-encoder.raw'],
]);

export const jsDefaultConvertersV2Url = new Map<ConverterType, string>([
  [ConverterType.UPLINK, '/assets/converters/js-decoder.raw' ],
  [ConverterType.DOWNLINK, '/assets/converters/js-encoder.raw'],
]);

export const tbelDefaultConvertersUrl = new Map<ConverterType, string>([
  [ConverterType.UPLINK, '/assets/converters/tbel-decoder.raw' ],
  [ConverterType.DOWNLINK, '/assets/converters/tbel-encoder.raw'],
]);

export const tbelDefaultConvertersV2Url = new Map<ConverterType, string>([
  [ConverterType.UPLINK, '/assets/converters/tbel-decoder.raw' ],
  [ConverterType.DOWNLINK, '/assets/converters/tbel-encoder.raw'],
]);

export const DefaultUpdateOnlyKeysValue = ['manufacturer'];
export type DefaultUpdateOnlyKeys = {[key in IntegrationType]?: Array<string>};

export const converterTypeTranslationMap = new Map<ConverterType, string>(
  [
    [ConverterType.UPLINK, 'converter.type-uplink'],
    [ConverterType.DOWNLINK, 'converter.type-downlink'],
  ]
);

export type ConverterVersion = 1 | 2;

export interface Converter extends BaseData<ConverterId>, ExportableEntity<ConverterId>, HasEntityDebugSettings {
  tenantId?: TenantId;
  name: string;
  type: ConverterType;
  configuration: ConverterConfig & Partial<ConverterConfigV2>;
  additionalInfo?: any;
  edgeTemplate: boolean;
  integrationType?: IntegrationType;
  converterVersion: ConverterVersion;
}

export interface ConverterConfig {
  scriptLang: ScriptLanguage;
  decoder: string;
  tbelDecoder: string;
  encoder: string;
  tbelEncoder: string;
  updateOnlyKeys: string[];
}

export interface ConverterConfigV2 extends ConverterConfig {
  type: EntityType.DEVICE | EntityType.ASSET;
  name: string;
  profile: string;
  label: string;
  customer: string;
  group: string;
  attributes: string[];
  telemetry: string[];
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

export interface LatestConverterParameters {
  converterType?: ConverterType;
  integrationType?: IntegrationType;
  integrationName?: string;
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

export enum ConverterSourceType {
  NEW = 'new',
  EXISTING = 'existing',
  LIBRARY = 'library',
  SKIP = 'skip',
}

export interface ConverterLibraryValue {
  vendor: string;
  model: string;
  converter: Converter;
}

export interface Vendor {
  name: string;
  logo: string;
}

export interface Model {
  name: string;
  photo: string;
  info: {
    description: string;
    label: string;
    url: string;
  };
  searchText?: string;
}

export function getConverterHelpLink(converter: Converter) {
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

export interface ConverterParams {
  converterScope: string;
}

export function resolveConverterParams(route: ActivatedRouteSnapshot): ConverterParams {
  return {
    converterScope: route.data.convertersType ? route.data.convertersType : 'tenant'
  };
}

const getJsTemplateUrl = (
  converterType: ConverterType,
  converterVersion: ConverterVersion
): string => {
  const url =
    converterVersion === 2
      ? jsDefaultConvertersV2Url.get(converterType)
      : jsDefaultConvertersUrl.get(converterType);
  if (!url) {
    throw new Error(
      `JS template URL not found for converterType: ${converterType} and converterVersion: ${converterVersion}`
    );
  }
  return url;
};

const getTbelTemplateUrl = (
  converterType: ConverterType,
  converterVersion: ConverterVersion
): string => {
  const url =
    converterVersion === 2
      ? tbelDefaultConvertersV2Url.get(converterType)
      : tbelDefaultConvertersUrl.get(converterType);
  if (!url) {
    throw new Error(
      `Tbel template URL not found for converterType: ${converterType} and converterVersion: ${converterVersion}`
    );
  }
  return url;
};

export const getTargetField =
  (converterType: ConverterType, scriptLang: ScriptLanguage): string => {
    return scriptLang === ScriptLanguage.TBEL
      ? (converterType === ConverterType.UPLINK ? 'tbelDecoder' : 'tbelEncoder')
      : (converterType === ConverterType.UPLINK ? 'decoder' : 'encoder');
  }

export const getTargetTemplateUrl =
  (converterType: ConverterType, scriptLang: ScriptLanguage,
   integrationType: IntegrationType, converterVersion: ConverterVersion = 1): string => {
    if (scriptLang === ScriptLanguage.JS) {
      return getJsTemplateUrl(converterType, converterVersion);
    } else if (converterType === ConverterType.UPLINK && IntegrationTbelDefaultConvertersUrl.has(integrationType)) {
      return IntegrationTbelDefaultConvertersUrl.get(integrationType);
    }
    return getTbelTemplateUrl(converterType, converterVersion);
  }

export const getConverterFunctionHeldId =
  (converterType: ConverterType, scriptLang: ScriptLanguage): string => {
    return scriptLang === ScriptLanguage.TBEL
      ? (converterType === ConverterType.UPLINK ? 'converter/tbel/decoder_fn' : 'converter/tbel/encoder_fn')
      : (converterType === ConverterType.UPLINK ? 'converter/decoder_fn' : 'converter/encoder_fn');
  }
