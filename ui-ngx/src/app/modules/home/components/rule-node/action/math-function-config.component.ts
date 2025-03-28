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
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import {
  ArgumentTypeResult,
  ArgumentTypeResultMap,
  AttributeScopeMap,
  AttributeScopeResult,
  MathFunction
} from '../rule-node-config.models';


@Component({
  selector: 'tb-action-node-math-function-config',
  templateUrl: './math-function-config.component.html',
  styleUrls: ['./math-function-config.component.scss']
})
export class MathFunctionConfigComponent extends RuleNodeConfigurationComponent {

  mathFunctionConfigForm: UntypedFormGroup;

  MathFunction = MathFunction;
  ArgumentTypeResult = ArgumentTypeResult;
  argumentTypeResultMap = ArgumentTypeResultMap;
  attributeScopeMap = AttributeScopeMap;
  argumentsResult = Object.values(ArgumentTypeResult);
  attributeScopeResult = Object.values(AttributeScopeResult);

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.mathFunctionConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.mathFunctionConfigForm = this.fb.group({
      operation: [configuration ? configuration.operation : null, [Validators.required]],
      arguments: [configuration ? configuration.arguments : null, [Validators.required]],
      customFunction: [configuration ? configuration.customFunction : '', [Validators.required]],
      result: this.fb.group({
        type: [configuration ? configuration.result.type: null, [Validators.required]],
        attributeScope: [configuration ? configuration.result.attributeScope : null, [Validators.required]],
        key: [configuration ? configuration.result.key : '', [Validators.required]],
        resultValuePrecision: [configuration ? configuration.result.resultValuePrecision : 0],
        addToBody: [configuration ? configuration.result.addToBody : false],
        addToMetadata: [configuration ? configuration.result.addToMetadata : false]
      })
    });
  }

  protected updateValidators(emitEvent: boolean) {
    const operation: MathFunction = this.mathFunctionConfigForm.get('operation').value;
    const resultType: ArgumentTypeResult = this.mathFunctionConfigForm.get('result.type').value;
    if (operation === MathFunction.CUSTOM) {
      this.mathFunctionConfigForm.get('customFunction').enable({emitEvent: false});
      if (this.mathFunctionConfigForm.get('customFunction').value === null) {
        this.mathFunctionConfigForm.get('customFunction').patchValue('(x - 32) / 1.8', {emitEvent: false});
      }
    } else {
      this.mathFunctionConfigForm.get('customFunction').disable({emitEvent: false});
    }
    if (resultType === ArgumentTypeResult.ATTRIBUTE) {
      this.mathFunctionConfigForm.get('result.attributeScope').enable({emitEvent: false});
    } else {
      this.mathFunctionConfigForm.get('result.attributeScope').disable({emitEvent: false});
    }
    this.mathFunctionConfigForm.get('customFunction').updateValueAndValidity({emitEvent});
    this.mathFunctionConfigForm.get('result.attributeScope').updateValueAndValidity({emitEvent});
  }

  protected validatorTriggers(): string[] {
    return ['operation', 'result.type'];
  }
}
