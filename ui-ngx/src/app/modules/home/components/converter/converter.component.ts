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

import { ChangeDetectorRef, Component, DestroyRef, Inject, Input, OnInit, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../entity/entity.component';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import {
  Converter,
  ConverterConfigV2,
  ConverterDebugInput,
  ConverterMsg,
  ConverterType,
  converterTypeTranslationMap,
  ConverterVersion,
  DefaultUpdateOnlyKeys,
  DefaultUpdateOnlyKeysValue,
  getConverterFunctionArgs,
  getConverterFunctionHeldId,
  getConverterFunctionHeldPopupStyle,
  getConverterFunctionName,
  getConverterTestFunctionName,
  getTargetField,
  getTargetTemplateUrl,
  LatestConverterParameters
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
import { ConverterInfo, IntegrationsConvertersInfo, IntegrationType } from '@shared/models/integration.models';
import { capitalize, deepClone, isDefinedAndNotNull, isEmptyStr, isNotEmptyStr } from '@core/utils';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { catchError, map } from 'rxjs/operators';
import { forkJoin, merge, Observable, of } from 'rxjs';
import { ContentType } from '@shared/models/constants';
import { ConverterLibraryService } from '@core/http/converter-library.service';
import { EntityType } from '@shared/models/entity-type.models';
import { IntegrationService } from '@core/http/integration.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { StringItemsOption } from '@shared/components/string-items-list.component';

@Component({
  selector: 'tb-converter',
  templateUrl: './converter.component.html',
  styleUrls: ['./converter.component.scss']
})
export class ConverterComponent extends EntityComponent<Converter> implements OnInit {

  private predefinedConverterName: string

  @Input()
  hideTypes = false;

  @Input()
  set converterName(value: string) {
    this.predefinedConverterName = value;
    if (isDefinedAndNotNull(value) && this.entityForm.get('name').pristine) {
      this.entityForm.get('name').patchValue(value, {emitEvent: false});
    }
  }

  @Input()
  libraryInfo: { vendorName: string; modelName: string };

  @Input()
  integrationName: string;

  converterType = ConverterType;

  converterTypes = Object.values(ConverterType) as ConverterType[];

  converterTypeTranslations = converterTypeTranslationMap;

  tbelEnabled: boolean;

  scriptLanguage = ScriptLanguage;

  EntityType = EntityType;

  predefinedKeys: Array<string> = [];

  entityTypeKeysTranslationMap = new Map<EntityType, Record<string, string>>([
    [EntityType.ASSET, {
      'name': 'converter.asset-name',
      'name-required': 'converter.asset-name-required',
      'profile': 'converter.asset-profile-name',
      'label': 'converter.asset-label',
      'group': 'converter.asset-group-name'
    }
    ],
    [EntityType.DEVICE, {
      'name': 'converter.device-name',
      'name-required': 'converter.device-name-required',
      'profile': 'converter.device-profile-name',
      'label': 'converter.device-label',
      'group': 'converter.device-group-name'
    }]
  ])

  private predefinedConverterKeys: StringItemsOption[];
  private integrationsConvertersInfo: IntegrationsConvertersInfo;

  private defaultUpdateOnlyKeysByIntegrationType: DefaultUpdateOnlyKeys = {};
  private defaultConverterV2Configuration: Record<IntegrationType, ConverterConfigV2> = {} as Record<IntegrationType, ConverterConfigV2>;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private converterService: ConverterService,
              private integrationService: IntegrationService,
              private dialog: MatDialog,
              private resourcesService: ResourcesService,
              private converterLibraryService: ConverterLibraryService,
              @Optional() @Inject('entity') protected entityValue: Converter,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Converter>,
              protected fb: FormBuilder,
              protected cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
    forkJoin({
      updateOnlyKeys: this.resourcesService.loadJsonResource<DefaultUpdateOnlyKeys>('/assets/converters/default-update-only-keys.json'),
      converterV2Config: this.resourcesService.loadJsonResource<Record<IntegrationType, ConverterConfigV2>>('/assets/converters/default-converters-v2-configuration.json'),
      integrationsConvertersInfo: this.integrationService.getIntegrationsConvertersInfoCached()
    }).pipe(
      takeUntilDestroyed()
    ).subscribe((value) =>{
      this.defaultUpdateOnlyKeysByIntegrationType = value.updateOnlyKeys;
      this.defaultConverterV2Configuration = value.converterV2Config;
      this.integrationsConvertersInfo = value.integrationsConvertersInfo;
    })
  }

  ngOnInit() {
    merge<[ConverterType, IntegrationType]>(
      this.entityForm.get('type').valueChanges,
      this.entityForm.get('integrationType').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      const converterType: ConverterType = this.entityForm.get('type').value;
      this.updateConverterVersion();
      this.onConverterTypeChanged(converterType);
      if (converterType === ConverterType.UPLINK) {
        this.updateDefaultConfiguration(this.entityForm.get('integrationType').value);
      }
    });
    this.entityForm.get('configuration.scriptLang').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.onSetDefaultScriptBody(this.entityForm.get('type').value);
    });
    this.entityForm.get('configuration.type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((type: EntityType) => {
      this.updateConfigurationEntityName(type);
    });
    this.checkIsNewConverter(this.entity, this.entityForm);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    }
    return false;
  }

  buildForm(entity: Converter): FormGroup {
    this.tbelEnabled = getCurrentAuthState(this.store).tbelEnabled;
    const form = this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255), Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      type: [entity?.type ? entity.type : ConverterType.UPLINK, [Validators.required]],
      debugSettings: [entity?.debugSettings ?? { failuresEnabled: true, allEnabled: false, allEnabledUntil: 0 }],
      integrationType: [entity?.integrationType ?? null],
      converterVersion: [entity?.converterVersion ?? 1],
      configuration: this.fb.group({
        scriptLang: [entity?.configuration ? entity.configuration.scriptLang : ScriptLanguage.JS],
        decoder: [entity?.configuration ? entity.configuration.decoder : null],
        tbelDecoder: [entity?.configuration ? entity.configuration.tbelDecoder : null],
        encoder: [entity?.configuration ? entity.configuration.encoder : null],
        tbelEncoder: [entity?.configuration ? entity.configuration.tbelEncoder : null],
        updateOnlyKeys: [entity?.configuration ? entity.configuration.updateOnlyKeys : null],
        type: [entity?.configuration?.type ?? EntityType.DEVICE],
        name: [entity?.configuration?.name ?? '', [Validators.required, Validators.maxLength(255), Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
        profile: [entity?.configuration?.profile ?? ''],
        label: [entity?.configuration?.label ?? ''],
        customer: [entity?.configuration?.customer ?? ''],
        group: [entity?.configuration?.group ?? ''],
        telemetry: [entity?.configuration?.telemetry ?? null],
        attributes: [entity?.configuration?.attributes ?? null],
      }),
      additionalInfo: this.fb.group({
        description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
      })
    });
    return form;
  }

  prepareFormValue(value: Converter): Converter {
    if (value.converterVersion !== 2) {
      delete value.configuration.type;
      delete value.configuration.name;
      delete value.configuration.profile;
      delete value.configuration.label;
      delete value.configuration.customer;
      delete value.configuration.group;
      delete value.configuration.attributes;
      delete value.configuration.telemetry;
    }
    return super.prepareFormValue(value);
  }

  updatedValidators() {
    this.updatedConverterVersionDisableState();
  }

  private checkIsNewConverter(entity: Converter, form: FormGroup) {
    if (entity && !entity.id) {
      if (!this.libraryInfo) {
        form.get('type').patchValue(entity.type || ConverterType.UPLINK, {emitEvent: true});
        form.get('configuration.scriptLang').patchValue(
          this.tbelEnabled ? ScriptLanguage.TBEL : ScriptLanguage.JS, {emitEvent: true});
        form.get('integrationType').updateValueAndValidity({onlySelf: true});
      } else {
        this.updatedPredefinedConverterKeys();
        this.updatedConverterVersionDisableState();
        form.updateValueAndValidity();
      }
    } else {
      form.get('type').disable({emitEvent: false});
      form.get('integrationType').disable({emitEvent: false});
      let scriptLang: ScriptLanguage = form.get('configuration.scriptLang').value;
      if (scriptLang === ScriptLanguage.TBEL && !this.tbelEnabled) {
        scriptLang = ScriptLanguage.JS;
        form.get('configuration.scriptLang').patchValue(scriptLang, {emitEvent: true});
      }
      if (isDefinedAndNotNull(entity?.converterVersion)) {
        this.updatedPredefinedConverterKeys();
        this.updatedConverterVersionDisableState();
      } else {
        this.updateConverterVersion();
      }
    }
  }

  private updatedOnlyKeysValue(integrationType: IntegrationType) {
    let updateOnlyKeys = DefaultUpdateOnlyKeysValue;
    if (this.defaultUpdateOnlyKeysByIntegrationType.hasOwnProperty(integrationType)) {
      updateOnlyKeys = this.defaultUpdateOnlyKeysByIntegrationType[integrationType];
    }
    this.entityForm.get('configuration').patchValue({updateOnlyKeys}, {emitEvent: false});
  }

  private updateDefaultConfiguration(integrationType: IntegrationType) {
    if (this.entityForm.get('converterVersion').value === 2) {
      if (this.defaultConverterV2Configuration.hasOwnProperty(integrationType)) {
        this.entityForm.get('configuration').patchValue(this.defaultConverterV2Configuration[integrationType], {emitEvent: false});
      } else {
        this.updatedOnlyKeysValue(integrationType);
      }
    } else {
      this.updatedOnlyKeysValue(integrationType);
    }
  }

  private onConverterTypeChanged(converterType: ConverterType) {
    if (converterType) {
      if (converterType === ConverterType.UPLINK) {
        this.entityForm.get('configuration').patchValue({
          decoder: null,
          tbelDecoder: null
        }, {emitEvent: false});
      } else {
        this.entityForm.get('configuration').patchValue({
          encoder: null,
          tbelEncoder: null,
        }, {emitEvent: false});
      }
      this.onSetDefaultScriptBody(converterType);
    }
  }

  private updateConverterVersion() {
    const integrationType: IntegrationType = this.entityForm.get('integrationType').value;
    if (isNotEmptyStr(integrationType)) {
      const converterType: ConverterType = this.entityForm.get('type').value;
      const converterInfo: ConverterInfo = this.integrationsConvertersInfo[integrationType]?.[converterType.toLocaleLowerCase()];
      if (converterInfo?.keys?.length) {
        this.entityForm.get('converterVersion').setValue(2, {emitEvent: false});
      } else {
        this.entityForm.get('converterVersion').setValue(1, {emitEvent: false});
      }
    } else {
      this.entityForm.get('converterVersion').setValue(1, {emitEvent: false});
    }
    this.updatedPredefinedConverterKeys();
    this.updatedConverterVersionDisableState();
  }

  private updatedConverterVersionDisableState() {
    const converterVersion: ConverterVersion = this.entityForm.get('converterVersion').value;
    if (converterVersion === 2 && this.entityForm.enabled) {
      this.entityForm.get('configuration').get('name').enable({emitEvent: false});
    } else {
      this.entityForm.get('configuration').get('name').disable({emitEvent: false});
    }
  }

  private updateConfigurationEntityName(type: EntityType) {
    const prevEntityType: EntityType = this.entityForm.value.configuration.type;
    const nameControl = this.entityForm.get('configuration').get('name');
    if (this.entity && !this.entity.id && nameControl.pristine) {
      let nameControlValue: string = nameControl.value;
      nameControlValue = nameControlValue.replace(new RegExp(prevEntityType, "i"), capitalize(type));
      nameControl.setValue(nameControlValue, {emitEvent: false});
    }
  }

  private updatedPredefinedConverterKeys() {
    if (this.entityForm.get('converterVersion').value === 2) {
      const converterType: ConverterType = this.entityForm.get('type').value;
      const integrationType: IntegrationType = this.entityForm.get('integrationType').value;
      const converterInfo: ConverterInfo = this.integrationsConvertersInfo[integrationType]?.[converterType.toLocaleLowerCase()];
      this.predefinedConverterKeys = converterInfo.keys.map(value => ({name: value, value}));
      this.predefinedKeys = deepClone(converterInfo.keys);
    } else {
      this.predefinedConverterKeys = null;
      this.predefinedKeys = null;
    }
  }

  private onSetDefaultScriptBody(converterType: ConverterType): void {
    if (this.libraryInfo) {
      return;
    }

    const scriptLang = this.entityForm.get('configuration.scriptLang').value;
    const integrationType: IntegrationType = this.entityForm.get('integrationType').value;
    const targetField = getTargetField(converterType, scriptLang);
    this.setupDefaultScriptBody(targetField, converterType, scriptLang, integrationType);
  }

  private setupDefaultScriptBody(targetField: string, converterType: ConverterType,
                                 scriptLang: ScriptLanguage, integrationType: IntegrationType): void {
    const scriptBody: string = this.entityForm.get('configuration').get(targetField).value;
    const converterVersion: ConverterVersion = this.entityForm.get('converterVersion').value;

    if (!isNotEmptyStr(scriptBody)) {
      const targetTemplateUrl = getTargetTemplateUrl(converterType, scriptLang, integrationType, converterVersion);
      this.resourcesService.loadJsonResource(targetTemplateUrl)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(template => {
          this.entityForm.get('configuration').get(targetField)
            .patchValue(template, { emitEvent: false });
        });
    }
  }

  updateForm(entity: Converter) {
    const scriptLang = entity.configuration && entity.configuration.scriptLang ? entity.configuration.scriptLang : ScriptLanguage.JS;
    let converterName = entity?.name ?? '';
    if (this.predefinedConverterName && isEmptyStr(converterName) && this.entityForm.get('name').pristine) {
      converterName = this.predefinedConverterName
    }
    this.entityForm.patchValue({
      type: entity.type,
      name: converterName,
      debugSettings: entity?.debugSettings ?? { failuresEnabled: false, allEnabled: false, allEnabledUntil: 0 },
      integrationType: entity?.integrationType ?? null,
      converterVersion: entity?.converterVersion ?? 1,
      configuration: {
        scriptLang,
        decoder: entity.configuration?.decoder ?? null,
        tbelDecoder: entity.configuration?.tbelDecoder ?? null,
        encoder: entity.configuration?.encoder ?? null,
        tbelEncoder: entity.configuration?.tbelEncoder ?? null,
        updateOnlyKeys: entity.configuration?.updateOnlyKeys ?? null,
        type: entity.configuration?.type ?? EntityType.DEVICE,
        name: entity.configuration?.name ?? '',
        profile: entity.configuration?.profile ?? '',
        label: entity.configuration?.label ?? '',
        customer: entity.configuration?.customer ?? '',
        group: entity.configuration?.group ?? '',
        telemetry: entity.configuration?.telemetry ?? null,
        attributes: entity.configuration?.attributes ?? null,
      },
      additionalInfo: {
        description: entity.additionalInfo?.description ?? ''
      }
    }, {emitEvent: false});

    this.checkIsNewConverter(entity, this.entityForm);
  }

  onConverterIdCopied() {
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
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((debugIn: ConverterDebugInput) => this.showConverterTestDialog(debugIn));
  }

  get mainConfigurationTitle(): string {
    return this.entityForm.get('type').value === ConverterType.UPLINK ? 'converter.main-decoding-configuration' : 'converter.main-encoding-configuration';
  }

  get functionFormControl(): FormControl {
    const scriptLang: ScriptLanguage = this.entityForm.get('configuration.scriptLang').value;
    const converterType: ConverterType = this.entityForm.get('type').value;
    return this.entityForm.get('configuration').get(getTargetField(converterType, scriptLang)) as FormControl;
  }

  get functionName(): string {
    return getConverterFunctionName(this.entityForm.get('type').value, this.entityForm.get('converterVersion').value);
  }

  get functionArgs(): string[] {
    return getConverterFunctionArgs(this.entityForm.get('type').value);
  }

  get functionHelpId(): string {
    const scriptLang: ScriptLanguage = this.entityForm.get('configuration.scriptLang').value;
    const converterType: ConverterType = this.entityForm.get('type').value;
    const converterVersion: ConverterVersion = this.entityForm.get('converterVersion').value
    return getConverterFunctionHeldId(converterType, scriptLang, converterVersion);
  }

  get functionHelpPopupStyle(): Record<string, string> {
    return getConverterFunctionHeldPopupStyle(this.entityForm.get('type').value);
  }

  get testFunctionButtonLabel(): string {
    return getConverterTestFunctionName(this.entityForm.get('type').value, this.entityForm.get('converterVersion').value);
  }

  get fetchLatestKey(): (searchText?: string) => Observable<Array<StringItemsOption>> {
    return this.predefinedConverterKeys?.length
      ? this.fetchConverterKeys.bind(this)
      : null;
  }

  private fetchConverterKeys(searchText?: string): Observable<Array<StringItemsOption>> {
    let result = this.predefinedConverterKeys;
    if (searchText && searchText.length) {
      result = this.predefinedConverterKeys.filter(option => option.name.toLowerCase().includes(searchText.toLowerCase()));
      if (!result.length) {
        result = [{value: searchText, name: searchText}];
      }
    }
    return of(result);
  }

  private getLibraryDebugIn(): Observable<ConverterDebugInput> {
    return forkJoin({
      inContent: this.converterLibraryService
        .getConverterPayload(this.entityForm.get('integrationType').value, this.libraryInfo.vendorName,
          this.libraryInfo.modelName, this.entityForm.get('type').value),
      inMetadata: this.converterLibraryService
        .getConverterMetaData(this.entityForm.get('integrationType').value, this.libraryInfo.vendorName,
          this.libraryInfo.modelName, this.entityForm.get('type').value)
    })
      .pipe(
        map((payload) => ({
          inMetadata: payload.inMetadata,
          inContent: payload.inContent,
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
        converterType: this.entityForm.get('type').value,
        integrationType: this.entityForm.get('integrationType').value,
        converterVersion: this.entityForm.get('converterVersion').value
      };
      if (this.integrationName) {
        parameters.integrationName = this.integrationName;
      }

      request = this.converterService.getLatestConverterDebugInput(NULL_UUID, parameters);
    }
    return request;
  }

  showConverterTestDialog(debugIn: ConverterDebugInput, setFirstTab = false) {
    const converterVersion: ConverterVersion = this.entityForm.get('converterVersion').value;
    if (converterVersion === 2) {
      const integrationType: IntegrationType = this.entityForm.get('integrationType').value;
      let currentMsg: ConverterMsg;
      try {
        currentMsg = {
          payload: JSON.parse(debugIn.inContent),
          metadata: {}
        }
      } catch (e) {
        currentMsg = {
          payload: debugIn.inContent,
          metadata: {}
        };
      }
      try {
        currentMsg.metadata = JSON.parse(debugIn.inMetadata);
      } catch (e) { /* empty */ }
      this.converterService.unwrapRawPayload(integrationType, currentMsg, {ignoreErrors: true}).pipe(
        catchError(() => of(null))
      ).subscribe(value => {
        if (value) {
          debugIn.inContent = value.payload;
          debugIn.inContentType = value.contentType;
          debugIn.inMetadata = JSON.stringify(value.metadata);
        }
        this.openTestDialog(debugIn, setFirstTab, currentMsg);
      });
    } else {
      this.openTestDialog(debugIn, setFirstTab);
    }
  }

  private openTestDialog(debugIn: ConverterDebugInput, setFirstTab = false, originalMsg?: ConverterMsg) {
    const isDecoder = this.entityForm.get('type').value === ConverterType.UPLINK;
    const scriptLang: ScriptLanguage = this.entityForm.get('configuration').get('scriptLang').value;
    const targetField = getTargetField(this.entityForm.get('type').value, scriptLang);
    const funcBody = this.entityForm.get('configuration').get(targetField).value;
    this.dialog.open<ConverterTestDialogComponent, ConverterTestDialogData, string>(ConverterTestDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
        data: {
          debugIn,
          isDecoder,
          funcBody,
          scriptLang,
          converter: this.entityFormValue(),
          originalMsg
        }
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => {
        if (result !== null) {
          if (setFirstTab) {
            if (this.isDetailsPage) {
              this.entitiesTableConfig.getEntityDetailsPage().onToggleEditMode(true);
            } else {
              this.entitiesTableConfig.getTable().entityDetailsPanel.onToggleEditMode(true);
            }
          }
          this.entityForm.get('configuration').get(targetField).patchValue(result);
          this.entityForm.get('configuration').get(targetField).markAsDirty();
          this.entityForm.updateValueAndValidity();
        }
      });
  }
}
