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

import { FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import {
  DeliveryMethodsTemplates,
  NotificationDeliveryMethod,
  NotificationDeliveryMethodInfoMap,
  NotificationTemplate,
  NotificationTemplateTypeTranslateMap,
  NotificationType
} from '@shared/models/notification.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { Directive, OnDestroy } from '@angular/core';
import { deepClone, deepTrim } from '@core/utils';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class TemplateConfiguration<T, R = any> extends DialogComponent<T, R> implements OnDestroy{

  templateNotificationForm: FormGroup;
  notificationTemplateConfigurationForm: FormGroup;

  notificationDeliveryMethods = Object.keys(NotificationDeliveryMethod) as NotificationDeliveryMethod[];
  notificationDeliveryMethodInfoMap = NotificationDeliveryMethodInfoMap;
  notificationTemplateTypeTranslateMap = NotificationTemplateTypeTranslateMap;

  deliveryConfiguration: Partial<DeliveryMethodsTemplates>;

  protected readonly destroy$ = new Subject<void>();

  protected deliveryMethodFormsMap: Map<NotificationDeliveryMethod, FormGroup>;

  protected constructor(protected store: Store<AppState>,
                        protected router: Router,
                        protected dialogRef: MatDialogRef<T, R>,
                        protected fb: FormBuilder) {
    super(store, router, dialogRef);

    this.templateNotificationForm = this.fb.group({
      name: ['', Validators.required],
      notificationType: [NotificationType.GENERAL],
      configuration: this.fb.group({
        deliveryMethodsTemplates: this.fb.group({}, {validators: this.atLeastOne()})
      })
    });

    this.templateNotificationForm.get('configuration.deliveryMethodsTemplates').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.deliveryConfiguration = value;
    });

    this.notificationTemplateConfigurationForm = this.fb.group({
      deliveryMethodsTemplates: null
    });

    this.notificationDeliveryMethods.forEach(method => {
      (this.templateNotificationForm.get('configuration.deliveryMethodsTemplates') as FormGroup)
        .addControl(method, this.fb.group({enabled: method === NotificationDeliveryMethod.WEB}), {emitEvent: false});
    });

    this.deliveryConfiguration = this.templateNotificationForm.get('configuration.deliveryMethodsTemplates').value;
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  atLeastOne() {
    return (group: FormGroup): ValidationErrors | null => {
      let hasAtLeastOne = true;
      if (group?.controls) {
        const controlsFormValue: FormGroup[] = Object.entries(group.controls).map(method => method[1]) as any;
        hasAtLeastOne = controlsFormValue.some(value => value.controls.enabled.value);
      }
      return hasAtLeastOne ? null : {atLeastOne: true};
    };
  }

  protected getNotificationTemplateValue(): NotificationTemplate {
    const template = deepClone(this.templateNotificationForm.value);
    template.configuration = deepClone(this.notificationTemplateConfigurationForm.value);
    return deepTrim(template);
  }
}
