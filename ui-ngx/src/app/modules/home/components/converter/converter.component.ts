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

import { ChangeDetectorRef, Component, Inject, Input, OnDestroy, OnInit, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../entity/entity.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import {
  Converter,
  ConverterDebugInput,
  ConverterType,
  converterTypeTranslationMap,
  DefaultUpdateOnlyKeys,
  DefaultUpdateOnlyKeysValue,
  IntegrationTbelDefaultConvertersUrl,
  jsDefaultConvertorsUrl,
  LatestConverterParameters,
  tbelDefaultConvertorsUrl
} from '@shared/models/converter.models';

import { ResourcesService } from '@core/services/resources.service';
import { ConverterService } from '@core/http/converter.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { MatDialog } from '@angular/material/dialog';
import {
  ConverterTestDialogComponent,
  ConverterTestDialogData
} from '@home/components/converter/converter-test-dialog.component';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { IntegrationType } from '@shared/models/integration.models';
import { isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { map, takeUntil } from 'rxjs/operators';
import { forkJoin, Observable, Subject } from 'rxjs';
import { ContentType } from '@shared/models/constants';
import { ConverterLibraryService } from '@core/http/converter-library.service';

@Component({
  selector: 'tb-converter',
  templateUrl: './converter.component.html',
  styleUrls: []
})
export class ConverterComponent extends EntityComponent<Converter> implements OnInit, OnDestroy {

  private _integrationType: IntegrationType;

  @Input()
  hideType = false;

  @Input()
  set integrationType(value: IntegrationType) {
    this._integrationType = value;
    if (isDefinedAndNotNull(value)) {
      this.updatedOnlyKeysValue();
      this.onSetDefaultScriptBody(this.entityForm.get('type').value);
    }
  }

  get integrationType() {
    return this._integrationType;
  }

  @Input()
  set convertorName(value: string) {
    if (isDefinedAndNotNull(value) && this.entityForm.get('name').pristine) {
      this.entityForm.get('name').patchValue(value, {emitEvent: false});
    }
  }

  @Input()
  libraryInfo: { vendorName: string; modelName: string };

  @Input()
  integrationName: string;

  converterType = ConverterType;

  converterTypes = Object.keys(ConverterType);

  converterTypeTranslations = converterTypeTranslationMap;

  tbelEnabled: boolean;

  scriptLanguage = ScriptLanguage;

  readonly converterDebugPerTenantLimitsConfiguration = getCurrentAuthState(this.store).converterDebugPerTenantLimitsConfiguration;

  private defaultUpdateOnlyKeysByIntegrationType: DefaultUpdateOnlyKeys = {};
  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private converterService: ConverterService,
              private dialog: MatDialog,
              private resourcesService: ResourcesService,
              private converterLibraryService: ConverterLibraryService,
              @Optional() @Inject('entity') protected entityValue: Converter,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Converter>,
              protected fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
    this.resourcesService.loadJsonResource<DefaultUpdateOnlyKeys>('/assets/converters/default-update-only-keys.json')
        .subscribe(value => this.defaultUpdateOnlyKeysByIntegrationType = value);
  }

  ngOnInit() {
    this.entityForm.get('type').valueChanges.pipe(
        takeUntil(this.destroy$)
    ).subscribe(() => {
      this.converterTypeChanged();
    });
    this.entityForm.get('configuration.scriptLang').valueChanges.pipe(
        takeUntil(this.destroy$)
    ).subscribe(() => {
      this.onSetDefaultScriptBody(this.entityForm.get('type').value);
    });
    this.checkIsNewConverter(this.entity, this.entityForm);
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Converter): FormGroup {
    this.tbelEnabled = getCurrentAuthState(this.store).tbelEnabled;
    const form = this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255), Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      type: [entity?.type ? entity.type : ConverterType.UPLINK, [Validators.required]],
      debugSettings: [entity?.debugSettings ?? { failuresEnabled: false, allEnabled: false, allEnabledUntil: 0 }],
      configuration: this.fb.group({
        scriptLang: [entity?.configuration ? entity.configuration.scriptLang : ScriptLanguage.JS],
        decoder: [entity?.configuration ? entity.configuration.decoder : null],
        tbelDecoder: [entity?.configuration ? entity.configuration.tbelDecoder : null],
        encoder: [entity?.configuration ? entity.configuration.encoder : null],
        tbelEncoder: [entity?.configuration ? entity.configuration.tbelEncoder : null],
        updateOnlyKeys: [entity?.configuration ? entity.configuration.updateOnlyKeys : []],
      }),
      additionalInfo: this.fb.group({
        description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
      })
    });
    return form;
  }

  private checkIsNewConverter(entity: Converter, form: FormGroup) {
    if (entity && !entity.id) {
      if (!this.libraryInfo) {
        form.get('type').patchValue(entity.type || ConverterType.UPLINK, {emitEvent: true});
        form.get('configuration.scriptLang').patchValue(
          this.tbelEnabled ? ScriptLanguage.TBEL : ScriptLanguage.JS, {emitEvent: true});
      } else {
        form.updateValueAndValidity();
      }
    } else {
      form.get('type').disable({emitEvent: false});
      let scriptLang: ScriptLanguage = form.get('configuration.scriptLang').value;
      if (scriptLang === ScriptLanguage.TBEL && !this.tbelEnabled) {
        scriptLang = ScriptLanguage.JS;
        form.get('configuration.scriptLang').patchValue(scriptLang, {emitEvent: true});
      }
    }
  }

  private updatedOnlyKeysValue() {
    let updateOnlyKeys = DefaultUpdateOnlyKeysValue;
    if (this.defaultUpdateOnlyKeysByIntegrationType.hasOwnProperty(this.integrationType)) {
      updateOnlyKeys = this.defaultUpdateOnlyKeysByIntegrationType[this.integrationType];
    }
    this.entityForm.get('configuration').patchValue({updateOnlyKeys}, {emitEvent: false});
  }

  private converterTypeChanged() {
    const converterType: ConverterType = this.entityForm.get('type').value;
    if (converterType) {
      if (converterType === ConverterType.UPLINK) {
        this.entityForm.get('configuration').patchValue({
          decoder: null,
          tbelDecoder: null
        }, {emitEvent: false});
        this.updatedOnlyKeysValue();
      } else {
        this.entityForm.get('configuration').patchValue({
          encoder: null,
          tbelEncoder: null,
        }, {emitEvent: false});
      }
      this.onSetDefaultScriptBody(converterType);
    }
  }

  private onSetDefaultScriptBody(converterType: ConverterType): void {
    if (this.libraryInfo) {
      return;
    }

    const scriptLang = this.entityForm.get('configuration.scriptLang').value;
    const targetField = this.getTargetField(converterType, scriptLang);
    this.setupDefaultScriptBody(targetField, converterType, scriptLang);
  }

  private getTargetField(converterType: ConverterType, scriptLang: ScriptLanguage): string {
    return scriptLang === ScriptLanguage.JS
      ? (converterType === ConverterType.UPLINK ? 'decoder' : 'encoder')
      : (converterType === ConverterType.UPLINK ? 'tbelDecoder' : 'tbelEncoder');
  }

  private getTargetTemplateUrl(converterType: ConverterType, scriptLang: ScriptLanguage): string {
    if (scriptLang === ScriptLanguage.JS) {
      return jsDefaultConvertorsUrl.get(converterType);
    } else if (converterType === ConverterType.UPLINK && IntegrationTbelDefaultConvertersUrl.has(this.integrationType)) {
      return IntegrationTbelDefaultConvertersUrl.get(this.integrationType);
    } else {
      return tbelDefaultConvertorsUrl.get(converterType);
    }
  }

  private setupDefaultScriptBody(targetField: string, converterType: ConverterType, scriptLang: ScriptLanguage): void {
    const scriptBody = this.entityForm.get('configuration').get(targetField).value;

    if (!isNotEmptyStr(scriptBody) || isDefinedAndNotNull(this.integrationType)) {
      const targetTemplateUrl = this.getTargetTemplateUrl(converterType, scriptLang);
      this.resourcesService.loadJsonResource(targetTemplateUrl)
        .pipe(takeUntil(this.destroy$))
        .subscribe(template => {
          this.entityForm.get('configuration').get(targetField)
            .patchValue(template, { emitEvent: false });
        });
    }
  }

  updateForm(entity: Converter) {
    const scriptLang = entity.configuration && entity.configuration.scriptLang ? entity.configuration.scriptLang : ScriptLanguage.JS;
    this.entityForm.patchValue({
      type: entity.type,
      name: entity?.name ? entity.name : '',
      debugSettings: entity?.debugSettings ?? { failuresEnabled: false, allEnabled: false, allEnabledUntil: 0 },
      configuration: {
        scriptLang,
        decoder: entity.configuration ? entity.configuration.decoder : null,
        tbelDecoder: entity.configuration ? entity.configuration.tbelDecoder : null,
        encoder: entity.configuration ? entity.configuration.encoder : null,
        tbelEncoder: entity.configuration ? entity.configuration.tbelEncoder : null,
        updateOnlyKeys: entity.configuration ? entity.configuration.updateOnlyKeys : []
      },
      additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : ''
      }
    }, {emitEvent: false});

    this.checkIsNewConverter(entity, this.entityForm);
  }

  onConverterIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('converter.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  openConverterTestDialog(): void {
    (this.libraryInfo ? this.getLibraryDebugIn() : this.getDefaultDebugIn())
      .pipe(takeUntil(this.destroy$))
      .subscribe((debugIn: ConverterDebugInput) => this.showConverterTestDialog(debugIn));
  }

  private getLibraryDebugIn(): Observable<ConverterDebugInput> {
    return forkJoin([
      this.converterLibraryService
        .getConverterPayload(this.integrationType, this.libraryInfo.vendorName, this.libraryInfo.modelName, this.entityForm.get('type').value),
      this.converterLibraryService
        .getConverterMetaData(this.integrationType, this.libraryInfo.vendorName, this.libraryInfo.modelName, this.entityForm.get('type').value)
    ])
      .pipe(
        map(([inContent, inMetadata]) => ({
          inMetadata: JSON.stringify(inMetadata),
          inContent: JSON.stringify(inContent),
          inContentType: ContentType.JSON
        } as ConverterDebugInput))
      );
  }

  private getDefaultDebugIn(): Observable<ConverterDebugInput> {
    let request: Observable<ConverterDebugInput>;
    if (this.entity.id) {
      request = this.converterService.getLatestConverterDebugInput(this.entity.id.id);
    } else {
      const parameters: LatestConverterParameters = {
        converterType: this.entityForm.get('type').value
      };

      if (this.integrationName && this.integrationType) {
        parameters.integrationName = this.integrationName;
        parameters.integrationType = this.integrationType;
      }

      request = this.converterService.getLatestConverterDebugInput(NULL_UUID, parameters);
    }
    return request;
  }

  showConverterTestDialog(debugIn: ConverterDebugInput, setFirstTab = false) {
    const isDecoder = this.entityForm.get('type').value === ConverterType.UPLINK;
    const scriptLang: ScriptLanguage = this.entityForm.get('configuration').get('scriptLang').value;
    let targetField: string;
    if (scriptLang === ScriptLanguage.JS) {
      targetField = isDecoder ? 'decoder' : 'encoder';
    } else {
      targetField = isDecoder ? 'tbelDecoder' : 'tbelEncoder';
    }
    const funcBody = this.entityForm.get('configuration').get(targetField).value;
    this.dialog.open<ConverterTestDialogComponent, ConverterTestDialogData, string>(ConverterTestDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
        data: {
          debugIn,
          isDecoder,
          funcBody,
          scriptLang
        }
      })
      .afterClosed().pipe(
        takeUntil(this.destroy$)
    ).subscribe((result) => {
        if (result !== null) {
          if (setFirstTab) {
            if (this.isDetailsPage) {
              this.entitiesTableConfig.getEntityDetailsPage().onToggleEditMode(true);
            } else {
              this.entitiesTableConfig.getTable().entityDetailsPanel.onToggleEditMode(true);
            }
          }
          this.entityForm.get(`configuration.${targetField}`).patchValue(result);
          this.entityForm.get(`configuration.${targetField}`).markAsDirty();
          this.entityForm.updateValueAndValidity();
        }
    });
  }
}
