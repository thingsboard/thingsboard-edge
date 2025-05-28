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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { Job, JobStatus } from '@app/shared/models/job.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { ResourceReferences } from '@shared/models/resource.models';

@Component({
  selector: 'tb-task-info',
  templateUrl: './task-info-panel.component.html',
  styleUrls: ['./task-info-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TaskInfoPanelComponent implements OnInit {

  JobStatus = JobStatus;

  @Input()
  job: Job;

  @Output()
  reprocessTask = new EventEmitter<void>();

  @Output()
  cancelTask = new EventEmitter<void>();

  reference: ResourceReferences = [];

  constructor(private popover: TbPopoverComponent<TaskInfoPanelComponent>) {
  }

  ngOnInit() {
    if (this.job.result.results.length > 0) {
      this.job.result.results.forEach((result) => {
        if (result.failure?.entityInfo) {
          this.reference.push(result.failure.entityInfo);
        }
      })
    }
  }

  cancel() {
    this.popover.hide();
  }

  reprocess() {
    this.reprocessTask.emit();
  }

  cancelJob() {
    this.cancelTask.emit();
  }
}
