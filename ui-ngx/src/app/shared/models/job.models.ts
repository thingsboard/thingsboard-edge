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

import { BaseData } from '@shared/models/base-data';
import { JobId } from '@shared/models/id/job-id';
import { TimePageLink } from '@shared/models/page/page-link';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityInfoData } from '@shared/models/entity.models';
import { CalculatedFieldId } from '@shared/models/id/calculated-field-id';

export enum JobType {
  CF_REPROCESSING = 'CF_REPROCESSING'
}

export const jobTypeTranslations = new Map<JobType, string>(
  [
    [JobType.CF_REPROCESSING, 'task.task-type.calculated-field-reprocessing']
  ]
)

export enum JobStatus {
  COMPLETED = 'COMPLETED',
  RUNNING = 'RUNNING',
  QUEUED = 'QUEUED',
  PENDING = 'PENDING',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

export const jobStatusTranslations = new Map<JobStatus, string>(
  [
    [JobStatus.QUEUED, 'task.task-status.queued'],
    [JobStatus.PENDING, 'task.task-status.pending'],
    [JobStatus.RUNNING, 'task.task-status.running'],
    [JobStatus.COMPLETED, 'task.task-status.completed'],
    [JobStatus.FAILED, 'task.task-status.failed'],
    [JobStatus.CANCELLED, 'task.task-status.cancelled'],
  ]
);

export interface BasicTaskResult {
  key: string;
  success: boolean;
  discarded: boolean;
  jobType: JobType;
}

export interface BasicTaskFailure {
  error: string;
}

export interface CfReprocessingTaskFailure extends BasicTaskFailure{
  entityInfo?: EntityInfoData;
}

export interface CfReprocessingTaskResult extends BasicTaskResult {
  failure: CfReprocessingTaskFailure;
}

export type TaskResult = CfReprocessingTaskResult;

export interface BasicJobConfiguration {
  tasksKey: string;
  toReprocess: TaskResult[];
  type: JobType;
}

export interface CfReprocessingJobConfiguration extends BasicJobConfiguration {
  calculatedFieldId: CalculatedFieldId;
  startTs: number;
  endTs: number;
}

export type JobConfiguration = CfReprocessingJobConfiguration;

export interface BasicJobResult {
  successfulCount: number;
  failedCount: number;
  discardedCount: number;
  totalCount?: number;
  jobType?: JobType;
  results: TaskResult[];
  generalError?: string;
  startTs?: number;
  finishTs?: number;
  cancellationTs?: number;
}

export type JobResult = BasicJobResult;

export interface Job extends Omit<BaseData<JobId>, 'label' | 'ownerId' | 'customerId' | 'name'> {
  type: JobType;
  key: string;
  status: JobStatus;
  configuration: JobConfiguration;
  result: JobResult;
}

export interface JobFilter {
  startTs?: number;
  endTs?: number;
  timeWindow?: number;
  types?: Array<JobType>;
  statuses?: Array<JobStatus>;
  entities?: Array<EntityId>;
}

export interface TaskManagerConfig extends EntityTableConfig<Job, TimePageLink> {
  filter?: JobFilter
}

export class JobQuery {

  pageLink: TimePageLink;
  types: JobType[];
  statuses: JobStatus[];
  entities: EntityId[];

  constructor(pageLink: TimePageLink,
              jobFilter: JobFilter) {
    this.pageLink = pageLink;
    this.types = jobFilter.types;
    this.statuses = jobFilter.statuses;
    this.entities = jobFilter.entities;
  }

  public toQuery(): string {
    let query = this.pageLink.toQuery();
    if (this.types?.length) {
      query += `&types=${this.types.map(type => encodeURIComponent(type)).join(',')}`;
    }
    if (this.statuses?.length) {
      query += `&statuses=${this.statuses.join(',')}`;
    }
    if (this.entities?.length) {
      query += `&entities=${this.entities.map(id => id.id).join(',')}`;
    }
    return query;
  }

}
