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

import {
  NotificationDeliveryMethod,
  NotificationDeliveryMethodTranslateMap,
  NotificationRequest,
  NotificationRequestPreview,
  NotificationTarget,
  NotificationType
} from '@shared/models/notification.models';
import { Component, Inject, OnDestroy, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { deepTrim, guid } from '@core/utils';
import { Observable } from 'rxjs';
import { EntityType } from '@shared/models/entity-type.models';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatStepper } from '@angular/material/stepper';
import { StepperOrientation, StepperSelectionEvent } from '@angular/cdk/stepper';
import { MediaBreakpoints } from '@shared/models/constants';
import { map, takeUntil } from 'rxjs/operators';
import { getCurrentTime } from '@shared/models/time/time.models';
import {
  TargetNotificationDialogComponent,
  TargetsNotificationDialogData
} from '@home/pages/notification-center/targets-table/target-notification-dialog.componet';
import { MatButton } from '@angular/material/button';
import { TemplateConfiguration } from '@home/pages/notification-center/template-table/template-configuration';

export interface RequestNotificationDialogData {
  request?: NotificationRequest;
  isAdd?: boolean;
}

@Component({
  selector: 'tb-request-notification-dialog',
  templateUrl: './request-notification-dialog.component.html',
  styleUrls: ['./request-notification-dialog.component.scss']
})
export class RequestNotificationDialogComponent extends
  TemplateConfiguration<RequestNotificationDialogComponent, NotificationRequest> implements OnDestroy {

  @ViewChild('createNotification', {static: true}) createNotification: MatStepper;
  stepperOrientation: Observable<StepperOrientation>;
  isAdd = true;
  entityType = EntityType;
  notificationType = NotificationType;

  notificationRequestForm: FormGroup;

  notificationDeliveryMethods = Object.keys(NotificationDeliveryMethod) as NotificationDeliveryMethod[];
  notificationDeliveryMethodTranslateMap = NotificationDeliveryMethodTranslateMap;

  selectedIndex = 0;
  preview: NotificationRequestPreview = null;

  dialogTitle = 'notification.notify-again';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RequestNotificationDialogComponent, NotificationRequest>,
              @Inject(MAT_DIALOG_DATA) public data: RequestNotificationDialogData,
              private breakpointObserver: BreakpointObserver,
              protected fb: FormBuilder,
              private notificationService: NotificationService,
              private dialog: MatDialog) {
    super(store, router, dialogRef, fb);

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-xs'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.notificationRequestForm = this.fb.group({
      useTemplate: [false],
      templateId: [{value: null, disabled: true}, Validators.required],
      targets: [null, Validators.required],
      template: this.templateNotificationForm,
      additionalConfig: this.fb.group({
        enabled: [false],
        timezone: [{value: '', disabled: true}, Validators.required],
        time: [{value: 0, disabled: true}, Validators.required]
      })
    });

    this.notificationRequestForm.get('template.name').setValue(guid())

    this.notificationRequestForm.get('useTemplate').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.notificationRequestForm.get('templateId').enable({emitEvent: false});
        this.notificationRequestForm.get('template').disable({emitEvent: false});
      } else {
        this.notificationRequestForm.get('templateId').disable({emitEvent: false});
        this.notificationRequestForm.get('template').enable({emitEvent: false});
      }
    });

    this.notificationRequestForm.get('additionalConfig.enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.notificationRequestForm.get('additionalConfig.timezone').enable({emitEvent: false});
        this.notificationRequestForm.get('additionalConfig.time').enable({emitEvent: false});
      } else {
        this.notificationRequestForm.get('additionalConfig.timezone').disable({emitEvent: false});
        this.notificationRequestForm.get('additionalConfig.time').disable({emitEvent: false});
      }
    });

    if (data.isAdd) {
      this.dialogTitle = 'notification.new-notification';
    }
    if (data.request) {
      this.notificationRequestForm.patchValue(this.data.request, {emitEvent: false});
    }
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  nextStepLabel(): string {
    if (this.selectedIndex !== 0) {
      if (this.selectedIndex >= this.maxStepperIndex) {
        return 'action.send';
      } else if (this.createNotification.selected.stepControl.pristine) {
        return 'action.skip';
      }
    }
    return 'action.next';
  }

  changeStep($event: StepperSelectionEvent) {
    this.selectedIndex = $event.selectedIndex;
    if (this.selectedIndex === this.maxStepperIndex) {
      this.getPreview();
    }
  }

  backStep() {
    this.createNotification.previous();
  }

  nextStep() {
    if (this.selectedIndex >= this.maxStepperIndex) {
      this.add();
    } else {
      this.createNotification.next();
    }
  }

  private get maxStepperIndex(): number {
    return this.createNotification?._steps?.length - 1;
  }

  private add(): void {
    if (this.allValid()) {
      this.notificationService.createNotificationRequest(this.notificationFormValue).subscribe(
        (notification) => this.dialogRef.close(notification)
      );
    }
  }

  private getPreview() {
    if (this.allValid()) {
      this.preview = null;
      this.notificationService.getNotificationRequestPreview(this.notificationFormValue).pipe(
        map(data => {
          if (data.processedTemplates.PUSH?.enabled) {
            (data.processedTemplates.PUSH as any).text = data.processedTemplates.PUSH.body;
          }
          return data;
        })
      ).subscribe(
        (preview) => this.preview = preview
      );
    }
  }

  private get notificationFormValue(): NotificationRequest {
    const formValue = deepTrim(this.notificationRequestForm.value);
    if (!formValue.useTemplate) {
      formValue.template = super.getNotificationTemplateValue();
    }
    delete formValue.useTemplate;
    let delay = 0;
    if (formValue.additionalConfig.enabled) {
      delay = (this.notificationRequestForm.value.additionalConfig.time.valueOf() - this.minDate().valueOf()) / 1000;
    }
    formValue.additionalConfig = {
      sendingDelayInSec: delay > 0 ? delay : 0
    };
    return formValue;
  }

  private allValid(): boolean {
    return !this.createNotification.steps.find((item, index) => {
      if (item.stepControl?.invalid) {
        item.interacted = true;
        this.createNotification.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }

  minDate(): Date {
    return new Date(getCurrentTime(this.notificationRequestForm.get('additionalConfig.timezone').value).format('lll'));
  }

  maxDate(): Date {
    const date = this.minDate();
    date.setDate(date.getDate() + 7);
    return date;
  }

  createTarget($event: Event, button: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    button._elementRef.nativeElement.blur();
    this.dialog.open<TargetNotificationDialogComponent, TargetsNotificationDialogData,
      NotificationTarget>(TargetNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          let formValue: string[] = this.notificationRequestForm.get('targets').value;
          if (!formValue) {
            formValue = [];
          }
          formValue.push(res.id.id);
          this.notificationRequestForm.get('targets').patchValue(formValue);
        }
      });
  }
}
