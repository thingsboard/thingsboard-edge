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
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';
import { IntegrationType } from '@shared/models/integration.models';


@Component({
  selector: 'tb-ttn-integration-form',
  templateUrl: './ttn-integration-form.component.html',
  styleUrls: ['./ttn-integration-form.component.scss']
})
export class TtnIntegrationFormComponent extends IntegrationFormComponent {

  @Input() topicFilters: FormGroup;
  @Input() downlinkTopicPattern: FormControl;
  @Input() integrationType: IntegrationType;

  hostTypes = ['Region', 'Custom'];
  hostRegion: FormControl;
  hostCustom: FormControl;
  currentHostType: FormControl;
  hostRegionSuffix: string;

  constructor(private fb: FormBuilder) {
    super();
    this.hostRegion = this.fb.control('');
    this.hostCustom = this.fb.control('');
    this.currentHostType = this.fb.control('');
    this.hostRegion.valueChanges.subscribe(() => {
      this.buildHostName();
      this.form.markAsDirty();
    });
    this.hostCustom.valueChanges.subscribe(() => {
      this.buildHostName();
      this.form.markAsDirty();
    });
    this.currentHostType.valueChanges.subscribe((type: string) => {
      this.updateHostParams(type);
      this.form.markAsDirty();
    });
  }

  onIntegrationFormSet() {
    this.hostRegionSuffix = this.integrationType === 'TTN' ? '.thethings.network' : '.cloud.thethings.industries';
    const hostType: string = this.form.get('customHost').value ? 'Custom' : 'Region';
    this.currentHostType.patchValue(hostType, {emitEvent: false});
    const host: string = this.form.get('host').value;
    if (hostType === 'Custom') {
      this.hostCustom.patchValue(host, {emitEvent: false});
      this.hostRegion.patchValue('', {emitEvent: false});
    } else if (hostType === 'Region') {
      if (host && host.endsWith(this.hostRegionSuffix)) {
        this.hostRegion.patchValue(host.slice(0, -this.hostRegionSuffix.length), {emitEvent: false});
      } else {
        this.hostRegion.patchValue(host, {emitEvent: false});
      }
      this.hostCustom.patchValue('', {emitEvent: false});
    }
    this.updateHostParams(hostType);
    if (this.integrationType === 'TTN') {
      this.downlinkTopicPattern.patchValue(this.form.get('credentials').get('username').value + '/devices/${devId}/down');
      this.form.get('credentials').get('username').valueChanges.subscribe(name => {
        this.downlinkTopicPattern.patchValue(name + '/devices/${devId}/down');
      });
    } else {
      this.downlinkTopicPattern.patchValue('v3/' + this.form.get('credentials').get('username').value + '/devices/${devId}/down/push');
      this.form.get('credentials').get('username').valueChanges.subscribe(name => {
        this.downlinkTopicPattern.patchValue('v3/' + name + '/devices/${devId}/down/push');
      });
    }
    this.updateControlsState();
  }

  updateFormState(disabled: boolean) {
    this.updateControlsState();
  }

  updateControlsState() {
    if (this.form.disabled) {
      this.hostRegion.disable({emitEvent: false});
      this.hostCustom.disable({emitEvent: false});
      this.currentHostType.disable({emitEvent: false});
    } else {
      this.hostRegion.enable({emitEvent: false});
      this.hostCustom.enable({emitEvent: false});
      this.currentHostType.enable({emitEvent: false});
    }
  }

  updateHostParams(hostType: string) {
    if (hostType === 'Region') {
      this.hostCustom.patchValue('', {emitEvent: false});
    } else {
      this.hostRegion.patchValue('', {emitEvent: false});
    }
    this.buildHostName();
  }

  buildHostName() {
    let host = '';
    if (this.currentHostType.value === 'Region') {
      if (this.hostRegion.value) {
        host = this.hostRegion.value + this.hostRegionSuffix;
      }
      this.form.get('customHost').patchValue(false);
    } else {
      host = this.hostCustom.value;
      this.form.get('customHost').patchValue(true);
    }
    this.form.get('host').patchValue(host);
  }

}
