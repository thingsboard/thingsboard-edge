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

import {
  booleanAttribute,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { JobFilter, JobStatus, jobStatusTranslations, JobType, jobTypeTranslations } from '@shared/models/job.models';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { TemplatePortal } from '@angular/cdk/portal';
import { fromEvent, Subscription } from 'rxjs';
import { StringItemsOption } from '@shared/components/string-items-list.component';
import { EntityType } from '@shared/models/entity-type.models';
import { Operation, resourceByEntityType } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-task-filter-config',
  templateUrl: './task-filter-config.component.html',
  styleUrls: ['./task-filter-config.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TaskFilterConfigComponent),
      multi: true
    }
  ]
})
export class TaskFilterConfigComponent implements ControlValueAccessor, OnDestroy {

  @ViewChild('taskFilterPanel', {static: true})
  taskFilterPanel: TemplateRef<any>;

  @Input({transform: booleanAttribute})
  disabled: boolean;

  jobStatuses: JobStatus[] = Object.values(JobStatus);
  jobStatusTranslations = jobStatusTranslations;

  jobTypes: StringItemsOption[] = Object.values(JobType).map(type => ({
    name: this.translate.instant(jobTypeTranslations.get(type)),
    value: type
  }))

  buttonDisplayValue = this.translate.instant('task.tasks-filter');

  filteredEntityType = [EntityType.DEVICE, EntityType.ASSET, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE];

  tasksFilterConfigForm = this.fb.group({
    statuses: [],
    types: [],
    entities: []
  });

  private taskFilterConfig: JobFilter;
  private propagateChange = (_: any) => {};

  private tasksFilterOverlayRef: OverlayRef;
  private resizeWindows: Subscription;

  constructor(private translate: TranslateService,
              private fb: FormBuilder,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private userPermissionsService: UserPermissionsService ) {
    this.filteredEntityType = this.filteredEntityType.filter(entityType => {
      const resource = resourceByEntityType.get(entityType);
      return resource ? this.userPermissionsService.hasGenericPermission(resource, Operation.READ) : false;
    })
  }

  ngOnDestroy(): void {
    this.resizeWindows?.unsubscribe();
    this.tasksFilterOverlayRef?.dispose();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.tasksFilterConfigForm.disable({emitEvent: false});
    } else {
      this.tasksFilterConfigForm.enable({emitEvent: false});
    }
  }

  writeValue(jobFilter: JobFilter): void {
    this.taskFilterConfig = deepClone(jobFilter) ?? {};
    this.updateButtonDisplayValue();
    this.tasksFilterConfigForm.patchValue(jobFilter, {emitEvent: false});
  }

  toggleJobFilterPanel($event: Event) {
    $event.stopPropagation();
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '80vh',
      height: 'min-content',
      minWidth: ''
    });
    config.hasBackdrop = true;
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(this.nativeElement)
      .withPositions([POSITION_MAP.bottomLeft]);

    this.tasksFilterOverlayRef = this.overlay.create(config);
    this.tasksFilterOverlayRef.backdropClick().subscribe(() => {
      this.resizeWindows?.unsubscribe();
      this.tasksFilterOverlayRef.dispose();
    });
    this.tasksFilterOverlayRef.attach(new TemplatePortal(this.taskFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.tasksFilterOverlayRef.updatePosition();
    });
  }

  update(): void {
    this.tasksFilterConfigForm.markAsPristine();
    this.taskFilterConfig = deepClone(this.tasksFilterConfigForm.value);
    this.updateButtonDisplayValue();
    this.propagateChange(this.tasksFilterConfigForm.value);
    this.resizeWindows?.unsubscribe();
    this.tasksFilterOverlayRef.dispose();
  }

  cancel(): void {
    this.tasksFilterConfigForm.reset(this.taskFilterConfig);
    this.tasksFilterConfigForm.markAsPristine();
    this.resizeWindows?.unsubscribe();
    this.tasksFilterOverlayRef.dispose();
  }

  reset(): void {
    this.tasksFilterConfigForm.reset();
    this.tasksFilterConfigForm.markAsDirty()
  }

  private updateButtonDisplayValue() {
    const filterTextParts: string[] = [];
    if (this.taskFilterConfig.statuses?.length) {
      filterTextParts.push(this.taskFilterConfig.statuses.map(s =>
        this.translate.instant(jobStatusTranslations.get(s))).join(', '));
    }
    if (this.taskFilterConfig.types?.length) {
      filterTextParts.push(this.taskFilterConfig.types.map(s =>
        this.translate.instant(jobTypeTranslations.get(s))).join(', '));
    }
    this.buttonDisplayValue = filterTextParts.length
      ? this.translate.instant('task.tasks-filter-params', { filterParams: filterTextParts.join(', ') })
      : this.translate.instant('task.tasks-filter');
  }
}
