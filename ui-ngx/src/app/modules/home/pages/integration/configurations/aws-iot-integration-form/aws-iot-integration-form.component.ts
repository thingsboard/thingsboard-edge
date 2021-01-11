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

import { Component, Input } from '@angular/core';
import { FormArray, FormControl } from '@angular/forms';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';

@Component({
  selector: 'tb-aws-iot-integration-form',
  templateUrl: './aws-iot-integration-form.component.html',
  styleUrls: ['./aws-iot-integration-form.component.scss']
})
export class AwsIotIntegrationFormComponent extends IntegrationFormComponent {

  @Input() topicFilters: FormArray;
  @Input() downlinkTopicPattern: FormControl;

  constructor() {
    super();
  }

}
