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

import { Component } from '@angular/core';
import { InitialPositionInStream } from '../../integration-forms-templates';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';
import { disableFields, enableFields } from '@home/pages/integration/integration-utils';

@Component({
  selector: 'tb-aws-kinesis-integration-form',
  templateUrl: './aws-kinesis-integration-form.component.html',
  styleUrls: ['./aws-kinesis-integration-form.component.scss']
})
export class AwsKinesisIntegrationFormComponent extends IntegrationFormComponent {

  initialPositionInStreams = Object.keys(InitialPositionInStream);

  constructor() {
    super();
  }

  protected onIntegrationFormSet() {
    this.form.get('useCredentialsFromInstanceMetadata').valueChanges.subscribe(() => {
      this.onUseCredentialsFromInstanceMetadataChange();
    });
    this.form.get('useConsumersWithEnhancedFanOut').valueChanges.subscribe(() => {
      this.onUseConsumersWithEnhancedFanOut();
    });
    this.onUseCredentialsFromInstanceMetadataChange();
    this.onUseConsumersWithEnhancedFanOut();
  }

  onUseCredentialsFromInstanceMetadataChange() {
    const fields = ['accessKeyId', 'secretAccessKey'];
    const useCredentialsFromInstanceMetadata: boolean = this.form.get('useCredentialsFromInstanceMetadata').value;
    if (useCredentialsFromInstanceMetadata) {
      disableFields(this.form, fields);
    } else {
      enableFields(this.form, fields);
    }
  }

  onUseConsumersWithEnhancedFanOut() {
    const fields = ['maxRecords', 'requestTimeout'];
    const useConsumersWithEnhancedFanOut: boolean = this.form.get('useConsumersWithEnhancedFanOut').value;
    if (useConsumersWithEnhancedFanOut) {
      disableFields(this.form, fields);
    } else {
      enableFields(this.form, fields);
    }
  }

}
