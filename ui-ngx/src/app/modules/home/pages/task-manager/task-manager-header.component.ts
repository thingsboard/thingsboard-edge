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

import { Component } from '@angular/core';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Job, JobFilter, TaskManagerConfig } from '@shared/models/job.models';
import { TimePageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-task-manager-table-header',
  templateUrl: './task-manager-header.component.html',
  styleUrls: ['./task-manager-header.component.scss']
})
export class TaskManagerHeaderComponent extends EntityTableHeaderComponent<Job, TimePageLink> {

  get taskManagerTableConfig(): TaskManagerConfig {
    return this.entitiesTableConfig as TaskManagerConfig;
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  taskManagerChanged(jobFilter: JobFilter) {
    this.taskManagerTableConfig.filter = jobFilter;
    this.taskManagerTableConfig.getTable().resetSortAndFilter(true, true);
  }
}
