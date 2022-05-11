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

import { ChangeDetectorRef, Component, Inject, OnInit, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormArray, FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import {
  Integration,
  IntegrationType,
  IntegrationTypeInfo,
  integrationTypeInfoMap
} from '@shared/models/integration.models';
import { guid, isDefined, isUndefined, removeEmptyObjects } from '@core/utils';
import { ConverterType } from '@shared/models/converter.models';
import {
  templates,
  updateIntegrationFormDefaultFields,
  updateIntegrationFormValidators,
  updateIntegrationFormState
} from './integration-forms-templates';
import _ from 'lodash';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';
import { IntegrationService } from '@core/http/integration.service';

@Component({
  selector: 'tb-integration',
  templateUrl: './integration.component.html',
  styleUrls: ['./integration.component.scss']
})
export class IntegrationComponent extends EntityComponent<Integration> implements OnInit {

  @ViewChild('integrationFormComponent', {static: false}) integrationFormComponent: IntegrationFormComponent;

  integrationType: IntegrationType;

  converterType = ConverterType;

  integrationTypes = IntegrationType;

  integrationTypeKeys = Object.keys(IntegrationType);

  integrationTypeInfos = integrationTypeInfoMap;

  integrationForm: FormGroup;

  integrationInfo: IntegrationTypeInfo;

  isEdgeTemplate: boolean;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Integration,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Integration>,
              protected fb: FormBuilder,
              protected integrationService: IntegrationService,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    this.isEdgeTemplate = this.entitiesTableConfig.componentsData.isEdgeTemplate;
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
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        type: [entity ? entity.type : null, [Validators.required]],
        enabled: [entity && isDefined(entity.enabled) ? entity.enabled : true],
        debugMode: [entity ? entity.debugMode : null],
        allowCreateDevicesOrAssets: [entity && isDefined(entity.allowCreateDevicesOrAssets) ? entity.allowCreateDevicesOrAssets : true],
        defaultConverterId: [entity ? entity.defaultConverterId : null, [Validators.required]],
        downlinkConverterId: [entity ? entity.downlinkConverterId : null, []],
        remote: [entity ? entity.remote : null],
        routingKey: this.fb.control({ value: entity ? entity.routingKey : null, disabled: true }),
        secret: this.fb.control({ value: entity ? entity.secret : null, disabled: true }),
        configuration: this.fb.array([entity ? entity.configuration : {}, []]),
        metadata: [entity && entity.configuration ? entity.configuration.metadata : {}],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
    this.integrationType = entity ? entity.type : null;
    form.get('type').valueChanges.subscribe((type: IntegrationType) => {
      this.integrationType = type;
      this.setConfigurationForm();
      this.integrationTypeChanged(form);
    });
    this.checkIsNewIntegration(entity, form);
    return form;
  }

  setConfigurationForm(configuration = {}) {
    const configurationForm = this.entityForm.get('configuration') as FormArray;
    configurationForm.controls = [];
    if (this.integrationType) {
      this.integrationInfo = this.integrationTypeInfos.get(this.integrationType);
      const formTemplate = _.cloneDeep(this.integrationInfo.http ? templates.http : templates[this.integrationType]);
      const ignoreNonPrimitiveFields: string[] = formTemplate.ignoreNonPrimitiveFields || [];
      const fieldValidators: {[key: string]: ValidatorFn | ValidatorFn[]} = formTemplate.fieldValidators || {};
      delete formTemplate.ignoreNonPrimitiveFields;
      delete formTemplate.fieldValidators;
      this.integrationForm = this.getIntegrationForm(_.merge(formTemplate, configuration), ignoreNonPrimitiveFields);
      updateIntegrationFormDefaultFields(this.integrationType, this.integrationForm);
      updateIntegrationFormValidators(this.integrationForm, fieldValidators);
      updateIntegrationFormState(this.integrationType, this.integrationInfo, this.integrationForm, !this.isEditValue);
      configurationForm.push(this.integrationForm);
    } else {
      this.integrationForm = null;
    }
  }

  updateFormState() {
    super.updateFormState();
    if (this.isEditValue && this.entityForm) {
      this.checkIsRemote(this.entityForm);
      this.entityForm.get('routingKey').disable({ emitEvent: false });
      this.entityForm.get('secret').disable({ emitEvent: false });
    }
    if (this.integrationForm) {
      updateIntegrationFormState(this.integrationType, this.integrationInfo, this.integrationForm, !this.isEditValue);
      if (this.integrationFormComponent) {
        this.integrationFormComponent.updateFormState(!this.isEditValue);
      }
    }
  }

  private checkIsNewIntegration(entity: Integration, form: FormGroup) {
    if (entity && !entity.id) {
      form.get('routingKey').patchValue(guid(), { emitEvent: false });
      form.get('secret').patchValue(this.generateSecret(20), { emitEvent: false });
    }
  }

  private integrationTypeChanged(form: FormGroup) {
   // form.get('configuration').patchValue({}, { emitEvent: false });
    form.get('metadata').patchValue({}, { emitEvent: false });
    this.checkIsRemote(form);
  }

  private checkIsRemote(form: FormGroup) {
    const integrationType: IntegrationType = form.get('type').value;
    if (integrationType && this.integrationTypeInfos.get(integrationType).remote) {
      form.get('remote').patchValue(true, { emitEvent: false });
      form.get('remote').disable({ emitEvent: false });
    } else if (this.isEditValue) {
      form.get('remote').enable({ emitEvent: false });
    }
  }

  get isRemoteIntegration(): boolean {
    return this.entityForm ? this.entityForm.value.remote : false;
  }

  updateForm(entity: Integration) {
    this.entityForm.patchValue({ name: entity.name });
    this.entityForm.patchValue({ type: entity.type }, { emitEvent: false });
    this.entityForm.patchValue({ enabled: isDefined(entity.enabled) ? entity.enabled : true });
    this.entityForm.patchValue({ debugMode: entity.debugMode });
    this.entityForm.patchValue(
      {allowCreateDevicesOrAssets: isDefined(entity.allowCreateDevicesOrAssets) ? entity.allowCreateDevicesOrAssets : true}
    );
    this.entityForm.patchValue({ defaultConverterId: entity.defaultConverterId });
    this.entityForm.patchValue({ downlinkConverterId: entity.downlinkConverterId });
    this.entityForm.patchValue({ remote: entity.remote });
    this.entityForm.patchValue({ routingKey: entity.routingKey });
    this.entityForm.patchValue({ secret: entity.secret });
    // this.entityForm.patchValue({ configuration: entity.configuration });
    this.entityForm.patchValue({ metadata: entity.configuration ? entity.configuration.metadata : {} });
    this.entityForm.patchValue({ additionalInfo: { description: entity.additionalInfo ? entity.additionalInfo.description : '' } });
    this.checkIsNewIntegration(entity, this.entityForm);
    this.integrationType = entity.type;
    this.setConfigurationForm(entity.configuration);
  }

  getIntegrationForm(form: object, ignoreNonPrimitiveFields: string[] = []): FormGroup {
    const template = {};
    for (const key of Object.keys(form)) {
      if (Array.isArray(form[key]) && !ignoreNonPrimitiveFields.includes(key)) {
        template[key] = this.fb.array(form[key].map(el => this.getIntegrationForm(el, ignoreNonPrimitiveFields)));
      }
      else if (typeof (form[key]) === 'object' && !ignoreNonPrimitiveFields.includes(key)) {
        template[key] = this.getIntegrationForm(form[key], ignoreNonPrimitiveFields);
      }
      else {
        template[key] = this.fb.control(form[key]);
      }
    }
    return this.fb.group(
      template
    );
  }

  prepareFormValue(formValue: any): any {
    if (!formValue.configuration) {
      formValue.configuration = {};
    }
    formValue.configuration = { ...removeEmptyObjects(this.integrationForm.getRawValue()) };
    formValue.configuration.metadata = formValue.metadata || {};
    formValue.name = formValue.name ? formValue.name.trim() : formValue.name;
    delete formValue.metadata;
    return formValue;
  }

  private generateSecret(length?: number): string {
    if (isUndefined(length) || length == null) {
      length = 1;
    }
    const l = length > 10 ? 10 : length;
    const str = Math.random().toString(36).substr(2, l);
    if (str.length >= length) {
      return str;
    }
    return str.concat(this.generateSecret(length - str.length));
  }

  onIntegrationIdCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('integration.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
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
        horizontalPosition: 'right'
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
        horizontalPosition: 'right'
      }));
    },
    error => {
      this.store.dispatch(new ActionNotificationShow(
        {
          message: error.error.message,
          type: 'error',
          duration: 5000,
          verticalPosition: 'bottom',
          horizontalPosition: 'right'
        }));
      return;
    });
  }
}
