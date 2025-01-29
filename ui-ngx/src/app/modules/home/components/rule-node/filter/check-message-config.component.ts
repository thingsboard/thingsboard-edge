///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
  selector: 'tb-filter-node-check-message-config',
  templateUrl: './check-message-config.component.html',
  styleUrls: []
})
export class CheckMessageConfigComponent extends RuleNodeConfigurationComponent {

  checkMessageConfigForm: FormGroup;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.checkMessageConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      messageNames: isDefinedAndNotNull(configuration?.messageNames) ? configuration.messageNames : [],
      metadataNames: isDefinedAndNotNull(configuration?.metadataNames) ? configuration.metadataNames : [],
      checkAllKeys: isDefinedAndNotNull(configuration?.checkAllKeys) ? configuration.checkAllKeys : false
    };
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      messageNames: isDefinedAndNotNull(configuration?.messageNames) ? configuration.messageNames : [],
      metadataNames: isDefinedAndNotNull(configuration?.metadataNames) ? configuration.metadataNames : [],
      checkAllKeys: configuration.checkAllKeys
    };
  }


  private atLeastOne(validator: ValidatorFn, controls: string[] = null) {
    return (group: FormGroup): ValidationErrors | null => {
      if (!controls) {
        controls = Object.keys(group.controls);
      }
      const hasAtLeastOne = group?.controls && controls.some(k => !validator(group.controls[k]));

      return hasAtLeastOne ? null : {atLeastOne: true};
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.checkMessageConfigForm = this.fb.group({
      messageNames: [configuration.messageNames, []],
      metadataNames: [configuration.metadataNames, []],
      checkAllKeys: [configuration.checkAllKeys, []]
    }, {validators: this.atLeastOne(Validators.required, ['messageNames', 'metadataNames'])});
  }

  get touchedValidationControl(): boolean {
    return ['messageNames', 'metadataNames'].some(name => this.checkMessageConfigForm.get(name).touched);
  }
}
