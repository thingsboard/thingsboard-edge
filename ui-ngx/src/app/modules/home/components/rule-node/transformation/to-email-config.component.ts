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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
  selector: 'tb-transformation-node-to-email-config',
  templateUrl: './to-email-config.component.html',
  styleUrls: ['./to-email-config.component.scss']
})
export class ToEmailConfigComponent extends RuleNodeConfigurationComponent {

  toEmailConfigForm: FormGroup;
  mailBodyTypes = [
    {
      name: 'rule-node-config.mail-body-types.plain-text',
      description: 'rule-node-config.mail-body-types.plain-text-description',
      value: 'false',
    },
    {
      name: 'rule-node-config.mail-body-types.html',
      description: 'rule-node-config.mail-body-types.html-text-description',
      value: 'true',
    },
    {
      name: 'rule-node-config.mail-body-types.use-body-type-template',
      description: 'rule-node-config.mail-body-types.dynamic-text-description',
      value: 'dynamic',
    }
  ];

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.toEmailConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.toEmailConfigForm = this.fb.group({
      fromTemplate: [configuration ? configuration.fromTemplate : null, [Validators.required]],
      toTemplate: [configuration ? configuration.toTemplate : null, [Validators.required]],
      ccTemplate: [configuration ? configuration.ccTemplate : null, []],
      bccTemplate: [configuration ? configuration.bccTemplate : null, []],
      subjectTemplate: [configuration ? configuration.subjectTemplate : null, [Validators.required]],
      mailBodyType: [configuration ? configuration.mailBodyType : null],
      isHtmlTemplate: [configuration ? configuration.isHtmlTemplate : null, [Validators.required]],
      bodyTemplate: [configuration ? configuration.bodyTemplate : null, [Validators.required]],
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      fromTemplate: isDefinedAndNotNull(configuration?.fromTemplate) ? configuration.fromTemplate : null,
      toTemplate: isDefinedAndNotNull(configuration?.toTemplate) ? configuration.toTemplate : null,
      ccTemplate: isDefinedAndNotNull(configuration?.ccTemplate) ? configuration.ccTemplate : null,
      bccTemplate: isDefinedAndNotNull(configuration?.bccTemplate) ? configuration.bccTemplate : null,
      subjectTemplate: isDefinedAndNotNull(configuration?.subjectTemplate) ? configuration.subjectTemplate : null,
      mailBodyType: isDefinedAndNotNull(configuration?.mailBodyType) ? configuration.mailBodyType : null,
      isHtmlTemplate: isDefinedAndNotNull(configuration?.isHtmlTemplate) ? configuration.isHtmlTemplate : null,
      bodyTemplate: isDefinedAndNotNull(configuration?.bodyTemplate) ? configuration.bodyTemplate : null,
    };
  }

  protected updateValidators(emitEvent: boolean) {
    if (this.toEmailConfigForm.get('mailBodyType').value === 'dynamic') {
      this.toEmailConfigForm.get('isHtmlTemplate').enable({emitEvent: false});
    } else {
      this.toEmailConfigForm.get('isHtmlTemplate').disable({emitEvent: false});
    }
    this.toEmailConfigForm.get('isHtmlTemplate').updateValueAndValidity({emitEvent});
  }

  protected validatorTriggers(): string[] {
    return ['mailBodyType'];
  }

  getBodyTypeName(): string {
    return this.mailBodyTypes.find(type => type.value === this.toEmailConfigForm.get('mailBodyType').value).name;
  }
}
