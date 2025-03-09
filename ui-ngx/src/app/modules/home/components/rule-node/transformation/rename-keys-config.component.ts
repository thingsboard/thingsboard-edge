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
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { FetchTo, FetchToRenameTranslation } from '../rule-node-config.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';


@Component({
  selector: 'tb-transformation-node-rename-keys-config',
  templateUrl: './rename-keys-config.component.html',
  styleUrls: ['./rename-keys-config.component.scss']
})
export class RenameKeysConfigComponent extends RuleNodeConfigurationComponent {
  renameKeysConfigForm: FormGroup;
  renameIn = [];
  translation = FetchToRenameTranslation;

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    super();
    for (const key of this.translation.keys()) {
      this.renameIn.push({
        value: key,
        name: this.translate.instant(this.translation.get(key))
      });
    }
  }

  protected configForm(): FormGroup {
    return this.renameKeysConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.renameKeysConfigForm = this.fb.group({
      renameIn: [configuration ? configuration.renameIn : null, [Validators.required]],
      renameKeysMapping: [configuration ? configuration.renameKeysMapping : null, [Validators.required]]
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    let renameIn: FetchTo;

    if (isDefinedAndNotNull(configuration?.fromMetadata)) {
      renameIn = configuration.fromMetadata ? FetchTo.METADATA : FetchTo.DATA;
    } else if (isDefinedAndNotNull(configuration?.renameIn)) {
      renameIn = configuration?.renameIn;
    } else {
      renameIn = FetchTo.DATA;
    }

    return {
      renameKeysMapping: isDefinedAndNotNull(configuration?.renameKeysMapping) ? configuration.renameKeysMapping : null,
      renameIn
    };
  }
}
