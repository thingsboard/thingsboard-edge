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

import { Injectable } from '@angular/core';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  ProgressBarEntityTableColumn
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import {
  Job,
  JobQuery,
  JobResult,
  JobStatus,
  jobStatusTranslations,
  JobType,
  jobTypeTranslations,
  processTask,
  TaskManagerConfig,
  workingTask
} from '@shared/models/job.models';
import { JobService } from '@core/http/job.service';
import { PageQueryParam, TimePageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { forAllTimeInterval } from '@shared/models/time/time.models';
import { TaskManagerHeaderComponent } from '@home/pages/task-manager/task-manager-header.component';
import { TbPopoverService } from '@shared/components/popover.service';
import { TaskInfoPanelComponent } from '@home/pages/task-manager/task-info-panel.component';
import { TaskParametersPanelComponent } from '@home/pages/task-manager/task-parameters-panel.component';
import { DialogService } from '@core/services/dialog.service';
import { CancelTaskDialogComponent, CancelTaskDialogData } from '@home/components/task/cancel-task-dialog.component';
import { ActivatedRoute, Router } from '@angular/router';
import { deepClone, getEntityDetailsPageURL } from '@core/utils';

interface TaskManagerPageQueryParams extends PageQueryParam {
  entityId?: string;
}

@Injectable()
export class TaskManagerTableConfigResolver {

  private readonly config: TaskManagerConfig = new EntityTableConfig<Job, TimePageLink>();

  constructor(private jobService: JobService,
              private popoverService: TbPopoverService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialogService: DialogService,
              private router: Router) {

    this.config.entityType = EntityType.JOB;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.JOB);
    this.config.entityResources = entityTypeResources.get(EntityType.JOB);
    this.config.headerComponent = TaskManagerHeaderComponent;

    this.config.addEnabled = false;
    this.config.detailsPanelEnabled = false;
    this.config.useTimePageLink = true;
    this.config.forAllTimeEnabled = true;
    this.config.entitiesDeleteEnabled = false;

    this.config.defaultTimewindowInterval = forAllTimeInterval();

    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.entitiesFetchFunction = pageLink => this.fetchJobs(pageLink);

    this.config.columns.push(
      new DateEntityTableColumn<Job>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Job>('type', 'task.type', '25%', (job) => {
        return this.translate.instant(jobTypeTranslations.get(job.type));
      }),
      new EntityLinkTableColumn<Job>('entityName', 'task.entity', '25%', (job) => {
        return job.entityName;
      }, (job: Job) => (job ? getEntityDetailsPageURL(job.entityId.id, job.entityId.entityType as EntityType) : ''), false),
      new EntityTableColumn<Job>('status', 'task.status', '20%',
        (job) => this.taskStatus(job.status),
        (job) => this.taskStatusStyle(job.status),
      ),
      new ProgressBarEntityTableColumn<Job>('progress', 'task.progress', '30%',
        (job) => this.progressBar(job.result),
        (job) => this.progressBarCellStyle(job.status),
        (job) => this.progressBarStyle(job.status)
      )
    );

    this.config.cellActionDescriptors = this.configureCellActions();
    this.config.onLoadAction = (activatedRoute) => this.onLoadAction(this.config, activatedRoute);
  }

  resolve(): EntityTableConfig<Job> {
    this.config.componentsData = {
      filter: {}
    }
    this.config.tableTitle = this.translate.instant('task.task-manager');
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  private fetchJobs(pageLink: TimePageLink): Observable<PageData<Job>> {
    const query = new JobQuery(pageLink, this.config.componentsData.filter);
    return this.jobService.getJobs(query);
  }

  private configureCellActions(): Array<CellActionDescriptor<Job>> {
    return [
      {
        name: this.translate.instant('task.task-parameters'),
        icon: 'mdi:file-document-outline',
        isEnabled: () => true,
        onAction: ($event, job) => this.openTaskParameters($event, job),
      },
      {
        name: this.translate.instant('task.task-info'),
        icon: 'info_outline',
        isEnabled: (entity) => entity.status !== JobStatus.QUEUED && entity.status !== JobStatus.PENDING,
        onAction: ($event, job) => this.openTaskInfo($event, job)
      },
      {
        name: this.translate.instant('task.delete-task'),
        nameFunction: (entity) => workingTask.includes(entity.status)
          ? this.translate.instant('task.cancel-task')
          : this.translate.instant('task.delete-task'),
        iconFunction: (entity) => workingTask.includes(entity.status) ? 'close' : 'delete',
        isEnabled: () => true,
        onAction: ($event, entity) => this.cancelOrDeleteTask($event, entity)
      }
    ];
  }

  private taskStatus(jobStatus: JobStatus): string {
    let backgroundColor: string;
    switch (jobStatus) {
      case JobStatus.CANCELLED:
        backgroundColor = 'rgba(0, 0, 0, 0.06)';
        break;
      case JobStatus.QUEUED:
        backgroundColor = 'rgba(162, 158, 42, 0.06)';
        break;
      case JobStatus.PENDING:
        backgroundColor = 'rgba(250, 164, 5, 0.06)';
        break;
      case JobStatus.RUNNING:
        backgroundColor = 'rgba(11, 99, 195, 0.06)';
        break;
      case JobStatus.FAILED:
        backgroundColor = 'rgba(209, 39, 48, 0.06)';
        break;
      case JobStatus.COMPLETED:
        backgroundColor = 'rgba(25, 128, 56, 0.06)';
        break;
    }
    return `<div class="status" style="border-radius: 16px; height: 32px; line-height: 32px; padding: 0 12px; width: fit-content; background-color: ${backgroundColor}">
                ${this.translate.instant(jobStatusTranslations.get(jobStatus))}
            </div>`;
  }

  private taskStatusStyle(jobStatus: JobStatus): object {
    const styleObj = {
      fontSize: '14px',
      cursor: 'pointer',
      color: '#000'
    };
    switch (jobStatus) {
      case JobStatus.CANCELLED:
        styleObj.color = 'rgba(0, 0, 0, 0.54)';
        break;
      case JobStatus.QUEUED:
        styleObj.color = '#A29E2A';
        break;
      case JobStatus.PENDING:
        styleObj.color = '#FAA405';
        break;
      case JobStatus.RUNNING:
        styleObj.color = '#0B63C3';
        break;
      case JobStatus.FAILED:
        styleObj.color = '#D12730';
        break;
      case JobStatus.COMPLETED:
        styleObj.color = '#198038';
        break;
    }
    return styleObj;
  }

  private progressBarCellStyle(jobStatus: JobStatus): object {
    const style: Record<string, any> = {
      fontSize: '14px',
      fontWeight: 500,
      letterSpacing: '0.25px',
      textAlign: 'center'
    };
    if (jobStatus === JobStatus.PENDING) {
      style.visibility = 'hidden';
    }
    return style;
  }

  private progressBarStyle(jobStatus: JobStatus): object {
    const style = {
      borderRadius: '6px',
      '--mdc-linear-progress-active-indicator-height': '12px',
      '--mdc-linear-progress-track-height': '12px',
      '--mdc-linear-progress-track-color': 'var(--tb-primary-50)',
      '--mdc-linear-progress-active-indicator-color': 'rgba(from var(--tb-primary-500) r g b / 0.5)'
    };
    if (jobStatus === JobStatus.CANCELLED) {
      style['--mdc-linear-progress-active-indicator-color'] = 'rgba(0, 0, 0, 0.12)';
    } else if (jobStatus === JobStatus.FAILED) {
      style['--mdc-linear-progress-active-indicator-color'] = 'rgba(209, 39, 48, 0.40)';
    }
    return style;
  }

  private progressBar(result: JobResult): number {
    const progress = processTask(result)
    return progress > 0 ? Math.round(progress / result.totalCount * 100) : (result.totalCount > 0 ? 0 : 100);
  }

  private openTaskInfo($event: Event, job: Job) {
    $event?.stopPropagation();
    const trigger = $event.target as HTMLElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const taskInfoPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.config.getTable().renderer,
        componentType: TaskInfoPanelComponent,
        hostView: this.config.getTable().viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          job
        },
        showCloseButton: true,
        overlayStyle: {maxHeight: '80vh', height: '100%', padding: '10px'}
      });
      taskInfoPanelPopover.tbComponentRef.instance.cancelTask.subscribe(() => {
        taskInfoPanelPopover.hide();
        this.cancelTaskDialog(null, job);
      });
      taskInfoPanelPopover.tbComponentRef.instance.reprocessTask.subscribe(() => {
        taskInfoPanelPopover.hide();
        this.jobService.reprocessJob(job.id.id, {ignoreErrors: true}).subscribe(() => {
          this.config.getTable().updateData();
        });
      })
    }
  }

  private openTaskParameters($event: Event, job: Job) {
    $event?.stopPropagation();
    const trigger = $event.target as HTMLElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      this.popoverService.displayPopover({
        trigger,
        renderer: this.config.getTable().renderer,
        componentType: TaskParametersPanelComponent,
        hostView: this.config.getTable().viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          job
        },
        showCloseButton: true,
        overlayStyle: {maxHeight: '80vh', height: '100%', padding: '10px'},
      });
    }
  }

  private cancelOrDeleteTask($event: Event, job: Job) {
    $event?.stopPropagation();
    if (workingTask.includes(job.status)) {
      this.cancelTaskDialog($event, job);
    } else {
      this.dialogService.confirm(
        this.translate.instant('task.delete-task-title'),
        this.translate.instant('task.delete-task-text'),
        this.translate.instant('action.no'),
        this.translate.instant('action.yes'),
        true
      ).subscribe((result) => {
        if (result) {
          this.jobService.deleteJob(job.id.id).subscribe(() => {
            this.config.getTable().updateData();
            this.config.entitiesDeleted([job.id]);
          });
        }
      });
    }
  }

  private cancelTaskDialog($event: Event, job: Job) {
    $event?.stopPropagation();
    let title = '';
    let message = '';
    switch (job.type) {
      case JobType.CF_REPROCESSING:
        title = this.translate.instant('task.cancel-task-calculated-field-reprocessing-title');
        message = this.translate.instant('task.cancel-task-calculated-field-reprocessing-text');
        break;
    }
    this.dialogService.dialog.open<CancelTaskDialogComponent, CancelTaskDialogData, boolean>(CancelTaskDialogComponent, {
      disableClose: true,
      data: {
        title,
        message
      },
      panelClass: ['tb-fullscreen-dialog']
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.jobService.cancelJob(job.id.id).subscribe(() => {
          this.config.getTable().updateData();
        });
      }
    });
  }

  onLoadAction(config: TaskManagerConfig, route: ActivatedRoute): void {
    const routerQueryParams: TaskManagerPageQueryParams = route.snapshot.queryParams;
    if (routerQueryParams) {
      const queryParams = deepClone(routerQueryParams);
      let replaceUrl = false;
      if (routerQueryParams?.entityId) {
        config.componentsData.filter.entities = [JSON.parse(decodeURIComponent(routerQueryParams.entityId))];
        delete queryParams.entityId;
        replaceUrl = true;
      }
      if (replaceUrl) {
        this.router.navigate([], {
          relativeTo: route,
          queryParams,
          queryParamsHandling: '',
          replaceUrl: true
        }).then(() => {});
      }
    }
  }
}
