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

import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { isDefinedAndNotNull } from '@core/public-api';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { FetchTo, FetchToTranslation } from '@home/components/rule-node/rule-node-config.models';

@Component({
  selector: 'tb-transformation-node-delete-keys-config',
  templateUrl: './delete-keys-config.component.html',
  styleUrls: []
})

export class DeleteKeysConfigComponent extends RuleNodeConfigurationComponent {

  deleteKeysConfigForm: FormGroup;
  deleteFrom = [];
  translation = FetchToTranslation;

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    super();
    for (const key of this.translation.keys()) {
      this.deleteFrom.push({
        value: key,
        name: this.translate.instant(this.translation.get(key))
      });
    }
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.deleteKeysConfigForm = this.fb.group({
      deleteFrom: [configuration.deleteFrom, [Validators.required]],
      keys: [configuration ? configuration.keys : null, [Validators.required]]
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    let deleteFrom: FetchTo;

    if (isDefinedAndNotNull(configuration?.fromMetadata)) {
      deleteFrom = configuration.fromMetadata ? FetchTo.METADATA : FetchTo.DATA;
    } else if (isDefinedAndNotNull(configuration?.deleteFrom)) {
      deleteFrom = configuration?.deleteFrom;
    } else {
      deleteFrom = FetchTo.DATA;
    }

    return {
      keys: isDefinedAndNotNull(configuration?.keys) ? configuration.keys : null,
      deleteFrom
    };
  }

  protected configForm(): FormGroup {
    return this.deleteKeysConfigForm;
  }
}
