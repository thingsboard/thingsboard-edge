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

import { Component, ViewChild } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatChipGrid, MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { AttributeScope, telemetryTypeTranslations } from '@shared/models/telemetry/telemetry.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-action-node-delete-attributes-config',
  templateUrl: './delete-attributes-config.component.html',
  styleUrls: []
})
export class DeleteAttributesConfigComponent extends RuleNodeConfigurationComponent {
  @ViewChild('attributeChipList') attributeChipList: MatChipGrid;

  deleteAttributesConfigForm: UntypedFormGroup;
  attributeScopeMap = AttributeScope;
  attributeScopes = Object.keys(AttributeScope);
  telemetryTypeTranslationsMap = telemetryTypeTranslations;
  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.deleteAttributesConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.deleteAttributesConfigForm = this.fb.group({
      scope: [configuration ? configuration.scope : null, [Validators.required]],
      keys: [configuration ? configuration.keys : null, [Validators.required]],
      sendAttributesDeletedNotification: [configuration ? configuration.sendAttributesDeletedNotification : false, []],
      notifyDevice: [configuration ? configuration.notifyDevice : false, []]
    });

    this.deleteAttributesConfigForm.get('scope').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      if (value !== AttributeScope.SHARED_SCOPE) {
        this.deleteAttributesConfigForm.get('notifyDevice').patchValue(false, {emitEvent: false});
      }
    });
  }

  removeKey(key: string): void {
    const keys: string[] = this.deleteAttributesConfigForm.get('keys').value;
    const index = keys.indexOf(key);
    if (index >= 0) {
      keys.splice(index, 1);
      this.deleteAttributesConfigForm.get('keys').patchValue(keys, {emitEvent: true});
    }
  }

  addKey(event: MatChipInputEvent): void {
    const input = event.input;
    let value = event.value;
    if ((value || '').trim()) {
      value = value.trim();
      let keys: string[] = this.deleteAttributesConfigForm.get('keys').value;
      if (!keys || keys.indexOf(value) === -1) {
        if (!keys) {
          keys = [];
        }
        keys.push(value);
        this.deleteAttributesConfigForm.get('keys').patchValue(keys, {emitEvent: true});
      }
    }
    if (input) {
      input.value = '';
    }
  }
}
