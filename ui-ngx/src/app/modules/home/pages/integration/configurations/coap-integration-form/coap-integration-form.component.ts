///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, Input, SimpleChanges } from '@angular/core';
import {
  CoapSecurityMode,
  coapSecurityModeTranslationsMap,
  IntegrationType
} from '@shared/models/integration.models';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { disableFields, enableFields } from '../../integration-utils';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';

@Component({
  selector: 'tb-coap-integration-form',
  templateUrl: './coap-integration-form.component.html',
  styleUrls: ['./coap-integration-form.component.scss']
})
export class CoapIntegrationFormComponent extends IntegrationFormComponent {

  coapSecurityModes = Object.keys(CoapSecurityMode);

  coapSecurityModeTranslations = coapSecurityModeTranslationsMap;

  @Input() integrationType: IntegrationType;
  @Input() routingKey;

  constructor(protected store: Store<AppState>, private translate: TranslateService) {
    super();
  }

  ngOnChanges(changes: SimpleChanges): void {
    super.ngOnChanges(changes);
    for (const propName of Object.keys(changes)) {
      if (['routingKey', 'integrationType'].includes(propName)) {
        setTimeout(() => {
          this.integrationBaseUrlChanged('baseUrl', 'coapEndpoint');
          this.integrationBaseUrlChanged('dtlsBaseUrl', 'dtlsCoapEndpoint');
        }, 0);
      }
    }
  }

  onIntegrationFormSet() {
    this.form.get('securityMode').valueChanges.subscribe(() => {
      this.integrationTypeChanged();
    });
    this.form.get('baseUrl').valueChanges.subscribe(() => {
      this.integrationBaseUrlChanged('baseUrl', 'coapEndpoint');
    });
    this.form.get('dtlsBaseUrl').valueChanges.subscribe(() => {
      this.integrationBaseUrlChanged('dtlsBaseUrl', 'dtlsCoapEndpoint');
    });
    disableFields(this.form, ['coapEndpoint', 'dtlsCoapEndpoint'], false);
  }

  onCoapEndpointCopied() {
    this.onEndpointCopied('integration.coap-endpoint-url-copied-message');
  }

  onDtlsCoapEndpointCopied() {
    this.onEndpointCopied('integration.coap-dtls-endpoint-url-copied-message');
  }

  get noSecureMode(): boolean {
    return this.checkSecurityMode(CoapSecurityMode.NO_SECURE);
  }

  get dtlsMode(): boolean {
    return this.checkSecurityMode(CoapSecurityMode.DTLS);
  }

  get mixedMode(): boolean {
    return this.checkSecurityMode(CoapSecurityMode.MIXED);
  }

  private checkSecurityMode(securityMode: CoapSecurityMode) {
    const coapSecurityMode = this.form.get('securityMode').value;
    return coapSecurityMode === securityMode;
  }

  private integrationTypeChanged() {
    let value = this.form.get('securityMode').value;
    switch (value) {
      case CoapSecurityMode.NO_SECURE:
        disableFields(this.form, ['dtlsBaseUrl'], false);
        break;
      case CoapSecurityMode.DTLS:
        disableFields(this.form, ['baseUrl'], false);
        break;
      case CoapSecurityMode.MIXED:
        enableFields(this.form, ['baseUrl', 'dtlsBaseUrl']);
        break;
    }
  }

  private integrationBaseUrlChanged(baseUrlKey: string, endpointKey: string) {
    let url = this.form.get(baseUrlKey).value;
    const key = this.routingKey || '';
    url += `/i/${key}`;
    this.form.get(endpointKey).patchValue(url);
  }

  private onEndpointCopied(key: string) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant(key),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

}
