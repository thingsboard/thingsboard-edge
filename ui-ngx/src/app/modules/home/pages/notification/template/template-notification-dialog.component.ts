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

import { NotificationDeliveryMethod, NotificationTemplate, NotificationType } from '@shared/models/notification.models';
import { Component, Inject, OnDestroy, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { StepperOrientation, StepperSelectionEvent } from '@angular/cdk/stepper';
import { MatStepper } from '@angular/material/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { TranslateService } from '@ngx-translate/core';
import { TemplateConfiguration } from '@home/pages/notification/template/template-configuration';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';

export interface TemplateNotificationDialogData {
  template?: NotificationTemplate;
  predefinedType?: NotificationType;
  isAdd?: boolean;
  isCopy?: boolean;
  readonly?: boolean;
}

@Component({
  selector: 'tb-template-notification-dialog',
  templateUrl: './template-notification-dialog.component.html',
  styleUrls: ['./template-notification-dialog.component.scss']
})
export class TemplateNotificationDialogComponent
  extends TemplateConfiguration<TemplateNotificationDialogComponent, NotificationTemplate> implements OnDestroy {

  @ViewChild('notificationTemplateStepper', {static: true}) notificationTemplateStepper: MatStepper;

  stepperOrientation: Observable<StepperOrientation>;
  stepperLabelPosition: Observable<'bottom' | 'end'>;

  dialogTitle = 'notification.edit-notification-template';

  notificationTypes: NotificationType[];

  selectedIndex = 0;
  hideSelectType = false;

  private readonly templateNotification: NotificationTemplate;
  private authState: AuthState = getCurrentAuthState(this.store);
  private authUser: AuthUser = this.authState.authUser;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<TemplateNotificationDialogComponent, NotificationTemplate>,
              @Inject(MAT_DIALOG_DATA) public data: TemplateNotificationDialogData,
              private breakpointObserver: BreakpointObserver,
              protected fb: FormBuilder,
              private notificationService: NotificationService,
              private translate: TranslateService) {
    super(store, router, dialogRef, fb);

    this.notificationTypes = this.allowNotificationType();

    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-md'])
      .pipe(map(({matches}) => matches ? 'end' : 'bottom'));

    if (isDefinedAndNotNull(this.data?.predefinedType)) {
      this.hideSelectType = true;
      this.templateNotificationForm.get('notificationType').setValue(this.data.predefinedType, {emitEvents: false});
    }

    if (data.isAdd || data.isCopy) {
      this.dialogTitle = 'notification.add-notification-template';
    }
    this.templateNotification = deepClone(this.data.template);

    if (this.templateNotification) {
      if (this.data.isCopy) {
        this.templateNotification.name += ` (${this.translate.instant('action.copy')})`;
      }
      this.templateNotificationForm.reset({}, {emitEvent: false});
      this.templateNotificationForm.patchValue(this.templateNotification, {emitEvent: false});
      // eslint-disable-next-line guard-for-in
      for (const method in this.templateNotification.configuration.deliveryMethodsTemplates) {
        this.deliveryMethodFormsMap.get(NotificationDeliveryMethod[method])
          .patchValue(this.templateNotification.configuration.deliveryMethodsTemplates[method]);
      }
    }

    if(data?.readonly) {
      this.dialogTitle = 'notification.view-notification-template';
      this.templateNotificationForm.disable({emitEvent: false});
      Array.from(this.deliveryMethodFormsMap.values()).map(form => form.disable({emitEvent: false}));
    }
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  changeStep($event: StepperSelectionEvent) {
    this.selectedIndex = $event.selectedIndex;
  }

  backStep() {
    this.notificationTemplateStepper.previous();
  }

  nextStep() {
    if (this.selectedIndex >= this.maxStepperIndex) {
      this.add();
    } else {
      this.notificationTemplateStepper.next();
    }
  }

  nextStepLabel(): string {
    if (this.selectedIndex >= this.maxStepperIndex) {
      return (this.data.isAdd || this.data.isCopy) ? 'action.add' : 'action.save';
    }
    return 'action.next';
  }

  get maxStepperIndex(): number {
    return this.notificationTemplateStepper?._steps?.length - 1;
  }

  private add(): void {
    if (this.allValid()) {
      let template = this.getNotificationTemplateValue();
      if (this.templateNotification && !this.data.isCopy) {
        template = {...this.templateNotification, ...template};
      }
      this.notificationService.saveNotificationTemplate(template).subscribe(
        (target) => this.dialogRef.close(target)
      );
    }
  }

  private allValid(): boolean {
    return !this.notificationTemplateStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.notificationTemplateStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }

  private isSysAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
  }

  private allowNotificationType(): NotificationType[] {
    if (this.isSysAdmin()) {
      return [NotificationType.GENERAL, NotificationType.ENTITIES_LIMIT];
    }
    return Object.values(NotificationType).filter(type => type !== NotificationType.ENTITIES_LIMIT);
  }
}
