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

import { ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import {
  Integration,
  IntegrationInfo,
  IntegrationType,
  integrationTypeInfoMap
} from '@shared/models/integration.models';
import { isDefined } from '@core/utils';
import { ConverterType } from '@shared/models/converter.models';
import { IntegrationService } from '@core/http/integration.service';
import { PageLink } from '@shared/models/page/page-link';

@Component({
  selector: 'tb-integration',
  templateUrl: './integration.component.html',
  styleUrls: ['./integration.component.scss']
})
export class IntegrationComponent extends EntityComponent<Integration, PageLink, IntegrationInfo> implements OnInit {

  converterType = ConverterType;

  integrationScope: 'tenant' | 'edges' | 'edge';

  private integrationType: IntegrationType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Integration,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Integration, PageLink, IntegrationInfo>,
              protected fb: FormBuilder,
              protected integrationService: IntegrationService,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    this.integrationScope = this.entitiesTableConfig.componentsData.integrationScope
      ? this.entitiesTableConfig.componentsData.integrationScope : 'tenant';
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Integration): FormGroup {
    this.integrationType = entity ? entity.type : null;
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255), Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
        type: [{value: this.integrationType, disabled: true}, [Validators.required]],
        enabled: [isDefined(entity?.enabled) ? entity.enabled : true],
        debugMode: [isDefined(entity?.debugMode) ? entity.debugMode : true],
        allowCreateDevicesOrAssets: [entity && isDefined(entity.allowCreateDevicesOrAssets) ? entity.allowCreateDevicesOrAssets : true],
        defaultConverterId: [entity ? entity.defaultConverterId : null, [Validators.required]],
        downlinkConverterId: [entity ? entity.downlinkConverterId : null, []],
        remote: [entity ? entity.remote : null],
        routingKey: this.fb.control({ value: entity ? entity.routingKey : null, disabled: true }),
        secret: this.fb.control({ value: entity ? entity.secret : null, disabled: true }),
        configuration: this.fb.control([entity ? entity.configuration : null]),
        metadata: [entity && entity.configuration ? entity.configuration.metadata : {}],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
  }

  updateFormState() {
    super.updateFormState();
    this.entityForm.get('type').disable({ emitEvent: false });
    if (this.isEditValue && this.entityForm) {
      this.checkIsRemote(this.entityForm);
      this.entityForm.get('routingKey').disable({ emitEvent: false });
      this.entityForm.get('secret').disable({ emitEvent: false });
    }
  }

  private checkIsRemote(form: FormGroup) {
    const integrationType: IntegrationType = form.get('type').value;
    if (integrationType && integrationTypeInfoMap.get(integrationType).remote) {
      form.get('remote').patchValue(true, { emitEvent: false });
      form.get('remote').disable({ emitEvent: false });
    } else if (this.isEditValue) {
      form.get('remote').enable({ emitEvent: false });
    }
  }

  get showDownlinkConvector(): boolean {
    if (integrationTypeInfoMap.has(this.integrationType)) {
      return !integrationTypeInfoMap.get(this.integrationType).hideDownlink;
    }
    return true;
  }

  private get allowCheckConnection(): boolean {
    if (integrationTypeInfoMap.has(this.integrationType)) {
      return integrationTypeInfoMap.get(this.integrationType).checkConnection || false;
    }
    return false;
  }

  get isCheckConnectionAvailable(): boolean {
    return this.allowCheckConnection && !this.isEdgeTemplate && !this.isRemoteIntegration;
  }

  get isRemoteIntegration(): boolean {
    return this.entityForm ? this.entityForm.value.remote : false;
  }

  get isEdgeTemplate(): boolean {
    return this.integrationScope === 'edge' || this.integrationScope === 'edges';
  }

  updateForm(entity: Integration) {
    this.entityForm.patchValue({
      name: entity.name,
      type: entity.type,
      enabled: isDefined(entity.enabled) ? entity.enabled : true,
      debugMode: isDefined(entity.debugMode) ? entity.debugMode : true,
      allowCreateDevicesOrAssets: isDefined(entity.allowCreateDevicesOrAssets) ? entity.allowCreateDevicesOrAssets : true,
      defaultConverterId: entity.defaultConverterId,
      downlinkConverterId: entity.downlinkConverterId,
      remote: entity.remote,
      routingKey: entity.routingKey,
      secret: entity.secret,
      metadata: entity.configuration ? entity.configuration.metadata : {},
      configuration: entity.configuration,
      additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : '' }
      },
      {emitEvent: false}
    );
    this.integrationType = entity.type;
  }

  prepareFormValue(formValue: any): any {
    if (!formValue.configuration) {
      formValue.configuration = {};
    }
    formValue.configuration.metadata = formValue.metadata || {};
    formValue.name = formValue.name ? formValue.name.trim() : formValue.name;
    delete formValue.metadata;
    return formValue;
  }

  onIntegrationIdCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('integration.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right',
        target: 'integrationRoot'
      }));
  }

  onIntegrationInfoCopied(type: string) {
    const message = type === 'key' ? 'integration.integration-key-copied-message'
      : 'integration.integration-secret-copied-message';
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant(message),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right',
        target: 'integrationRoot'
      }));
  }

  onIntegrationCheck(){
    this.integrationService.checkIntegrationConnection(this.entityFormValue(), {ignoreErrors: true}).subscribe(() =>
    {
      this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('integration.check-success'),
        type: 'success',
        duration: 5000,
        verticalPosition: 'bottom',
        horizontalPosition: 'right',
        target: 'integrationRoot'
      }));
    },
    error => {
      this.store.dispatch(new ActionNotificationShow(
        {
          message: error.error.message,
          type: 'error',
          duration: 5000,
          verticalPosition: 'bottom',
          horizontalPosition: 'right',
          target: 'integrationRoot'
        }));
    });
  }
}
