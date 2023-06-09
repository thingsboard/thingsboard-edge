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

import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { DashboardId } from '@shared/models/id/dashboard-id';

export enum SolutionTemplateLevel {
  MAKER = 'MAKER',
  PROTOTYPE = 'PROTOTYPE',
  STARTUP = 'STARTUP'
}

export const solutionTemplateLevelToPlanNameMap = new Map<SolutionTemplateLevel, string>(
  [
    [SolutionTemplateLevel.MAKER, 'Maker'],
    [SolutionTemplateLevel.PROTOTYPE, 'Prototype'],
    [SolutionTemplateLevel.STARTUP, 'Startup'],
  ]
);

export interface SolutionTemplate {
  id: string;
  title: string;
  level: SolutionTemplateLevel;
  installTimeoutMs: number;
}

export interface SolutionTemplateInfo extends SolutionTemplate {
  previewImageUrl: string;
  shortDescription: string;
  videoPreviewImageUrl: string;
  previewMp4Url: string;
  previewWebmUrl: string;
}

export interface TenantSolutionTemplateInfo extends SolutionTemplateInfo {
  installed: boolean;
}

export interface SolutionTemplateDetails extends SolutionTemplate {
  imageUrls: string[];
  highlights: string;
  description: string;
}

export interface TenantSolutionTemplateDetails extends SolutionTemplateDetails {
  installed: boolean;
}

export interface TenantSolutionTemplateInstructions {
  dashboardGroupId: EntityGroupId;
  dashboardId: DashboardId;
  details: string;
}

export interface SolutionInstallResponse extends TenantSolutionTemplateInstructions {
  success: boolean;
}
