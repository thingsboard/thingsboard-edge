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
import { HttpRequestType, IntLimit } from '../rule-node-config.models';

@Component({
  selector: 'tb-external-node-rest-api-call-config',
  templateUrl: './rest-api-call-config.component.html',
  styleUrls: []
})
export class RestApiCallConfigComponent extends RuleNodeConfigurationComponent {

  restApiCallConfigForm: UntypedFormGroup;

  readonly proxySchemes: string[] = ['http', 'https'];
  readonly httpRequestTypes = Object.keys(HttpRequestType);
  readonly MemoryBufferSizeInKbLimit = 25000;
  readonly IntLimit = IntLimit;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.restApiCallConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.restApiCallConfigForm = this.fb.group({
      restEndpointUrlPattern: [configuration ? configuration.restEndpointUrlPattern : null, [Validators.required]],
      requestMethod: [configuration ? configuration.requestMethod : null, [Validators.required]],
      useSimpleClientHttpFactory: [configuration ? configuration.useSimpleClientHttpFactory : false, []],
      parseToPlainText: [configuration ? configuration.parseToPlainText : false, []],
      ignoreRequestBody: [configuration ? configuration.ignoreRequestBody : false, []],
      enableProxy: [configuration ? configuration.enableProxy : false, []],
      useSystemProxyProperties: [configuration ? configuration.enableProxy : false, []],
      proxyScheme: [configuration ? configuration.proxyHost : null, []],
      proxyHost: [configuration ? configuration.proxyHost : null, []],
      proxyPort: [configuration ? configuration.proxyPort : null, []],
      proxyUser: [configuration ? configuration.proxyUser :null, []],
      proxyPassword: [configuration ? configuration.proxyPassword :null, []],
      readTimeoutMs: [configuration ? configuration.readTimeoutMs : null, [Validators.min(0), Validators.max(IntLimit)]],
      maxParallelRequestsCount: [configuration ? configuration.maxParallelRequestsCount : null, [Validators.min(0), Validators.max(IntLimit)]],
      headers: [configuration ? configuration.headers : null, []],
      credentials: [configuration ? configuration.credentials : null, []],
      maxInMemoryBufferSizeInKb: [configuration ? configuration.maxInMemoryBufferSizeInKb : null, [Validators.min(1), Validators.max(this.MemoryBufferSizeInKbLimit)]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSimpleClientHttpFactory', 'enableProxy', 'useSystemProxyProperties'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSimpleClientHttpFactory: boolean = this.restApiCallConfigForm.get('useSimpleClientHttpFactory').value;
    const enableProxy: boolean = this.restApiCallConfigForm.get('enableProxy').value;
    const useSystemProxyProperties: boolean = this.restApiCallConfigForm.get('useSystemProxyProperties').value;

    if (enableProxy && !useSystemProxyProperties) {
      this.restApiCallConfigForm.get('proxyHost').setValidators(enableProxy ? [Validators.required] : []);
      this.restApiCallConfigForm.get('proxyPort').setValidators(enableProxy ?
        [Validators.required, Validators.min(1), Validators.max(65535)] : []);
    } else {
      this.restApiCallConfigForm.get('proxyHost').setValidators([]);
      this.restApiCallConfigForm.get('proxyPort').setValidators([]);

      if (useSimpleClientHttpFactory) {
        this.restApiCallConfigForm.get('readTimeoutMs').setValidators([]);
      } else {
        this.restApiCallConfigForm.get('readTimeoutMs').setValidators([Validators.min(0), Validators.max(IntLimit)]);
      }
    }

    this.restApiCallConfigForm.get('readTimeoutMs').updateValueAndValidity({emitEvent});
    this.restApiCallConfigForm.get('proxyHost').updateValueAndValidity({emitEvent});
    this.restApiCallConfigForm.get('proxyPort').updateValueAndValidity({emitEvent});
    this.restApiCallConfigForm.get('credentials').updateValueAndValidity({emitEvent});
  }
}
