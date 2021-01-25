///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';
import { AbstractControl, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-azure-event-hub-integration-form',
  templateUrl: './azure-event-hub-integration-form.component.html',
  styleUrls: ['./azure-event-hub-integration-form.component.scss']
})
export class AzureEventHubIntegrationFormComponent extends IntegrationFormComponent implements OnInit, OnDestroy {

  @Input() downlinkConverterIdControl: AbstractControl;

  iotHubNameRequired = false;

  downlinkConverterChangeSubscription: Subscription = null;

  constructor() {
    super();
  }

  ngOnInit(): void {
    this.downlinkConverterChangeSubscription = this.downlinkConverterIdControl.valueChanges.subscribe(() => {
      this.downlinkConverterIdChanged();
    });
  }

  ngOnDestroy(): void {
    if (this.downlinkConverterChangeSubscription) {
      this.downlinkConverterChangeSubscription.unsubscribe();
    }
  }

  protected onIntegrationFormSet() {
    this.downlinkConverterIdChanged();
  }

  downlinkConverterIdChanged() {
    const downlinkConverterId = this.downlinkConverterIdControl.value;
    if (this.form) {
      if (downlinkConverterId !== null) {
        this.form.get('iotHubName').setValidators(Validators.required);
        this.iotHubNameRequired = true;
      } else {
        this.form.get('iotHubName').setValidators([]);
        this.iotHubNameRequired = false;
      }
      this.form.get('iotHubName').updateValueAndValidity();
    }
  }

}
