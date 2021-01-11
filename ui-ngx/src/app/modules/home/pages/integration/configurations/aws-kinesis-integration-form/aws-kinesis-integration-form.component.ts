///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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
