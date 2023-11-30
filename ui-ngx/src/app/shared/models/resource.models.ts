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

import { BaseData, ExportableEntity, HasId } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { TbResourceId } from '@shared/models/id/tb-resource-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WhiteLabeling } from '@shared/models/white-labeling.models';

export enum ResourceType {
  LWM2M_MODEL = 'LWM2M_MODEL',
  PKCS_12 = 'PKCS_12',
  JKS = 'JKS',
  JS_MODULE = 'JS_MODULE',
  IMAGE = 'IMAGE'
}

export const ResourceTypeMIMETypes = new Map<ResourceType, string>(
  [
    [ResourceType.LWM2M_MODEL, 'application/xml,text/xml'],
    [ResourceType.PKCS_12, 'application/x-pkcs12'],
    [ResourceType.JKS, 'application/x-java-keystore'],
    [ResourceType.JS_MODULE, 'text/javascript,application/javascript'],
    [ResourceType.IMAGE, 'image/*']
  ]
);

export const ResourceTypeExtension = new Map<ResourceType, string>(
  [
    [ResourceType.LWM2M_MODEL, 'xml'],
    [ResourceType.PKCS_12, 'p12,pfx'],
    [ResourceType.JKS, 'jks'],
    [ResourceType.JS_MODULE, 'js']
  ]
);

export const ResourceTypeTranslationMap = new Map<ResourceType, string>(
  [
    [ResourceType.LWM2M_MODEL, 'resource.type.lwm2m-model'],
    [ResourceType.PKCS_12, 'resource.type.pkcs-12'],
    [ResourceType.JKS, 'resource.type.jks'],
    [ResourceType.JS_MODULE, 'resource.type.js-module'],
    [ResourceType.IMAGE, 'resource.type.image']
  ]
);

export interface TbResourceInfo<D> extends Omit<BaseData<TbResourceId>, 'name' | 'label'>, ExportableEntity<TbResourceId> {
  tenantId?: TenantId;
  resourceKey?: string;
  title?: string;
  resourceType: ResourceType;
  fileName: string;
  descriptor?: D;
}

export type ResourceInfo = TbResourceInfo<any>;

export interface Resource extends ResourceInfo {
  data: string;
  name?: string;
}

export interface ImageDescriptor {
  mediaType: string;
  width: number;
  height: number;
  size: number;
  etag: string;
  previewDescriptor: ImageDescriptor;
}

export interface ImageResourceInfo extends TbResourceInfo<ImageDescriptor> {
  link?: string;
}

export interface ImageExportData {
  mediaType: string;
  fileName: string;
  title: string;
  resourceKey: string;
  data: string;
}

export type ImageResourceType = 'tenant' | 'system';

export type ImageReferences = Array<BaseData<HasId> | WhiteLabeling>;

export interface ImageResourceInfoWithReferences extends ImageResourceInfo {
  references: ImageReferences;
}

export interface ImageDeleteResult {
  image: ImageResourceInfo;
  success: boolean;
  imageIsReferencedError?: boolean;
  error?: any;
  references?: ImageReferences;
}

export const toImageDeleteResult = (image: ImageResourceInfo, e?: any): ImageDeleteResult => {
  if (!e) {
    return {image, success: true};
  } else {
    const result: ImageDeleteResult = {image, success: false, error: e};
    if (e?.status === 400 && e?.error?.success === false && (e?.error?.references || e?.error?.whiteLabelingList)) {
      const entityReferences: {[entityType: string]: Array<BaseData<HasId>>} = e?.error?.references;
      const whiteLabelingList: Array<WhiteLabeling> = e?.error?.whiteLabelingList;
      const references: ImageReferences = [];
      if (entityReferences) {
        for (const entityTypeStr of Object.keys(entityReferences)) {
          const entities = entityReferences[entityTypeStr];
          references.push.apply(references, entities);
        }
      }
      if (whiteLabelingList) {
        references.push.apply(references, whiteLabelingList);
      }
      result.imageIsReferencedError = true;
      result.references = references;
    }
    return result;
  }
};

export const imageResourceType = (imageInfo: ImageResourceInfo): ImageResourceType =>
  (!imageInfo.tenantId || imageInfo.tenantId?.id === NULL_UUID) ? 'system' : 'tenant';

export const TB_IMAGE_PREFIX = 'tb-image;';

export const IMAGES_URL_REGEXP = /\/api\/images\/(tenant|system)\/(.*)/;
export const IMAGES_URL_PREFIX = '/api/images';

export const IMAGE_BASE64_URL_PREFIX = 'data:image/';

export const removeTbImagePrefix = (url: string): string => url ? url.replace(TB_IMAGE_PREFIX, '') : url;

export const removeTbImagePrefixFromUrls = (urls: string[]): string[] => urls ? urls.map(url => removeTbImagePrefix(url)) : [];

export const prependTbImagePrefix = (url: string): string => {
  if (url && !url.startsWith(TB_IMAGE_PREFIX)) {
    url = TB_IMAGE_PREFIX + url;
  }
  return url;
};

export const prependTbImagePrefixToUrls = (urls: string[]): string[] => urls ? urls.map(url => prependTbImagePrefix(url)) : [];


export const isImageResourceUrl = (url: string): boolean => url && IMAGES_URL_REGEXP.test(url);

export const extractParamsFromImageResourceUrl = (url: string): {type: ImageResourceType; key: string} => {
  const res = url.match(IMAGES_URL_REGEXP);
  return {type: res[1] as ImageResourceType, key: res[2]};
};

export const isBase64DataImageUrl = (url: string): boolean => url && url.startsWith(IMAGE_BASE64_URL_PREFIX);

export const NO_IMAGE_DATA_URI = 'data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==';
