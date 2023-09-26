///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef } from '@angular/core';
import { UntypedFormBuilder, NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { IntegrationType } from '@shared/models/integration.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  ThingParkIntegrationFormComponent
} from '@home/components/integration/configuration/thing-park-integration-form/thing-park-integration-form.component';

@Component({
  selector: 'tb-thing-spark-enterprise-integration-form',
  templateUrl: './thing-park-integration-form.component.html',
  styleUrls: ['./thing-park-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ThingParkEnterpriseIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => ThingParkEnterpriseIntegrationFormComponent),
    multi: true,
  }]
})
export class ThingParkEnterpriseIntegrationFormComponent extends ThingParkIntegrationFormComponent {

  protected integrationType = IntegrationType.TPE;

  constructor(protected fb: UntypedFormBuilder,
              protected store: Store<AppState>,
              protected translate: TranslateService) {
    super(fb, store, translate);
  }

  ngOnInit() {
    super.ngOnInit();
    this.thingParkConfigForm.get('enableSecurity').disable({emitEvent: true});
    this.thingParkConfigForm.get('enableSecurity').setValue(true);
  }

  setDisabledState(isDisabled: boolean) {
    super.setDisabledState(isDisabled);
    this.thingParkConfigForm.get('enableSecurity').disable({emitEvent: true});
  }
}
