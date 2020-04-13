///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, OnInit, Input, AfterViewInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { Integration, IntegrationType } from '@shared/models/integration.models';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-http-integration-form',
  templateUrl: './http-integration-form.component.html',
  styleUrls: ['./http-integration-form.component.scss']
})
export class HttpIntegrationFormComponent implements AfterViewInit {


  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;
  @Input() routingKey;

  integrationTypes = IntegrationType;

  constructor(protected store: Store<AppState>, private translate: TranslateService) { }

  ngAfterViewInit(): void {
    this.integrationBaseUrlChanged();
    this.form.get('httpEndpoint').disable();
  }

  httpEnableSecurityChanged = () => {
    if (this.form.get('enableSecurity').value &&
      !this.form.get('headersFilter').value) {
      this.form.get('headersFilter').patchValue({});
    } else if (!this.form.get('enableSecurity').value) {
      this.form.get('headersFilter').patchValue(null)
    }
  };

  thingparkEnableSecurityChanged = () => {
    if (this.form.get('enableSecurity').value &&
      !this.form.get('maxTimeDiffInSeconds').value) {
      this.form.get('maxTimeDiffInSeconds').patchValue(60);
    }
    else {
      this.form.get('enableSecurityNew').patchValue(false);
      this.form.get('clientIdNew').patchValue(null);
      this.form.get('clientSecret').patchValue(null);
      this.form.get('asIdNew').patchValue(null);
      this.form.get('asKey').patchValue(null);
    }
  };

  onHttpEndpointCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('integration.http-endpoint-url-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

  integrationBaseUrlChanged() {
    let url = this.form.get('baseUrl').value;
    const type = this.integrationType ? this.integrationType.toLowerCase() : '';
    const key = this.routingKey || '';
    url += `/api/v1/integrations/${type}/${key}`;
    this.form.get('httpEndpoint').patchValue(url);
  };
}
