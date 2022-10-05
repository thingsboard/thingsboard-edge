///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, Input } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';
import { IntegrationType } from '@shared/models/integration.models';


@Component({
  selector: 'tb-ttn-integration-form',
  templateUrl: './ttn-integration-form.component.html',
  styleUrls: ['./ttn-integration-form.component.scss']
})
export class TtnIntegrationFormComponent extends IntegrationFormComponent {

  @Input() topicFilters: FormArray;
  @Input() downlinkTopicPattern: FormControl;
  @Input() integrationType: IntegrationType;

  private downlinkPattern = '';

  hostTypes = ['Region', 'Custom'];
  hostRegion: FormControl;
  hostCustom: FormControl;
  apiVersion: FormControl;
  currentHostType: FormControl;
  hostRegionSuffix: string;

  V3_DOWNLINK_TOPIC_PATTERN = 'v3/${applicationId}/devices/${devId}/down/push';
  V2_DOWNLINK_TOPIC_PATTERN = '${applicationId}/devices/${devId}/down';

  V3_UPLINK_TOPIC = {
    filter: 'v3/+/devices/+/up',
    qos: 0
  };
  V2_UPLINK_TOPIC = {
    filter: '+/devices/+/up',
    qos: 0
  };

  constructor(private fb: FormBuilder) {
    super();
    this.hostRegion = this.fb.control('');
    this.hostCustom = this.fb.control('');
    this.currentHostType = this.fb.control('');
    this.apiVersion = this.fb.control(false);
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
    this.apiVersion.valueChanges.subscribe((value: boolean) => {
      this.form.get('apiVersion').patchValue(value);
      this.updateTopicsState(value);
    });
  }

  onIntegrationFormSet() {
    this.hostRegionSuffix = this.integrationType === 'TTN' ? '.cloud.thethings.network' : '.cloud.thethings.industries';
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
    this.form.get('credentials.username').valueChanges.subscribe(name => {
      this.updateDownlinkPattern(name);
    });
    const apiVersion = this.form.get('apiVersion') ? this.form.get('apiVersion').value : true;
    this.updateHostParams(hostType);
    this.updateTopicsState(apiVersion);
    this.updateControlsState();
  }

  updateFormState(disabled: boolean) {
    this.updateControlsState();
  }

  updateTopicsState(apiVersion: boolean) {
    this.downlinkPattern = apiVersion ? this.V3_DOWNLINK_TOPIC_PATTERN : this.V2_DOWNLINK_TOPIC_PATTERN;
    const name = this.form.get('credentials').get('username').value;
    this.topicFilters.patchValue([apiVersion ? this.V3_UPLINK_TOPIC : this.V2_UPLINK_TOPIC]);
    this.apiVersion.patchValue(apiVersion, {emitEvent: false});
    this.updateDownlinkPattern(name);
  }

  updateDownlinkPattern(name: string) {
    const finalPattern = this.downlinkPattern.replace('${applicationId}', name);
    this.downlinkTopicPattern.patchValue(finalPattern);
    this.form.markAsDirty();
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
