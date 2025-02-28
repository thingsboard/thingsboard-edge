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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { entityGroupTypes } from '@app/shared/models/entity-group.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-transformation-node-duplicate-to-group-by-name-config',
  templateUrl: './duplicate-to-group-by-name-config.component.html',
  styleUrls: []
})
export class DuplicateToGroupByNameConfigComponent extends RuleNodeConfigurationComponent {

  duplicateToGroupByNameConfigForm: UntypedFormGroup;

  entityGroupTypesList = entityGroupTypes;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.duplicateToGroupByNameConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.duplicateToGroupByNameConfigForm = this.fb.group({
      searchEntityGroupForTenantOnly: [configuration ? configuration.searchEntityGroupForTenantOnly : false, []],
      considerMessageOriginatorAsAGroupOwner: [configuration ? configuration.considerMessageOriginatorAsAGroupOwner : true, []],
      groupType: [configuration ? configuration.groupType : null, [Validators.required]],
      groupName: [configuration ? configuration.groupName : null, [Validators.required]]
    });

    this.duplicateToGroupByNameConfigForm.get('searchEntityGroupForTenantOnly').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      const considerMessageOriginatorAsAGroupOwner = this.duplicateToGroupByNameConfigForm.get('considerMessageOriginatorAsAGroupOwner');
      if (value) {
        considerMessageOriginatorAsAGroupOwner.setValue(false);
        considerMessageOriginatorAsAGroupOwner.disable({emitEvent: false});
      } else {
        considerMessageOriginatorAsAGroupOwner.enable({emitEvent: false});
      }
    });
  }

  protected updateValidators() {
    if (this.duplicateToGroupByNameConfigForm.get('searchEntityGroupForTenantOnly').value === true) {
      this.duplicateToGroupByNameConfigForm.get('considerMessageOriginatorAsAGroupOwner').disable({emitEvent: false});
    }
  }

}
