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

import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import {
  Converter,
  ConverterDebugInput,
  ConverterType,
  converterTypeTranslationMap
} from '@shared/models/converter.models';

import jsDecoderTemplate from '!raw-loader!./js-decoder.raw';
import tbelDecoderTemplate from '!raw-loader!./tbel-decoder.raw';
import jsEncoderTemplate from '!raw-loader!./js-encoder.raw';
import tbelEncoderTemplate from '!raw-loader!./tbel-encoder.raw';
import { ConverterService } from '@core/http/converter.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { MatDialog } from '@angular/material/dialog';
import {
  ConverterTestDialogComponent,
  ConverterTestDialogData
} from '@home/components/converter/converter-test-dialog.component';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

@Component({
  selector: 'tb-converter',
  templateUrl: './converter.component.html',
  styleUrls: []
})
export class ConverterComponent extends EntityComponent<Converter> {

  @Input()
  hideType = false;

  converterType = ConverterType;

  converterTypes = Object.keys(ConverterType);

  converterTypeTranslations = converterTypeTranslationMap;

  tbelEnabled: boolean;

  scriptLanguage = ScriptLanguage;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private converterService: ConverterService,
              private dialog: MatDialog,
              @Optional() @Inject('entity') protected entityValue: Converter,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Converter>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Converter): UntypedFormGroup {
    this.tbelEnabled = getCurrentAuthState(this.store).tbelEnabled;
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255), Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
        type: [entity ? entity.type : null, [Validators.required]],
        debugMode: [entity ? entity.debugMode : null],
        configuration: this.fb.group(
          {
            scriptLang: [entity && entity.configuration ? entity.configuration.scriptLang : ScriptLanguage.JS],
            decoder: [entity && entity.configuration ? entity.configuration.decoder : null],
            tbelDecoder: [entity && entity.configuration ? entity.configuration.tbelDecoder : null],
            encoder: [entity && entity.configuration ? entity.configuration.encoder : null],
            tbelEncoder: [entity && entity.configuration ? entity.configuration.tbelEncoder : null],
          }
        ),
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
    form.get('type').valueChanges.subscribe(() => {
      this.converterTypeChanged(form);
    });
    form.get('configuration').get('scriptLang').valueChanges.subscribe(() => {
      this.converterScriptLangChanged(form);
    });
    this.checkIsNewConverter(entity, form);
    return form;
  }

  private checkIsNewConverter(entity: Converter, form: UntypedFormGroup) {
    if (entity && !entity.id) {
      form.get('type').patchValue(entity.type || ConverterType.UPLINK, {emitEvent: true});
      form.get('configuration').get('scriptLang').patchValue(
        this.tbelEnabled ? ScriptLanguage.TBEL : ScriptLanguage.JS, {emitEvent: false});
      form.get('type').patchValue(entity.type || ConverterType.UPLINK, {emitEvent: true});
    } else {
      form.get('type').disable({emitEvent: false});
      let scriptLang: ScriptLanguage = form.get('configuration').get('scriptLang').value;
      if (scriptLang === ScriptLanguage.TBEL && !this.tbelEnabled) {
        scriptLang = ScriptLanguage.JS;
        form.get('configuration').get('scriptLang').patchValue(scriptLang, {emitEvent: true});
      }
    }
  }

  private converterTypeChanged(form: UntypedFormGroup) {
    const converterType: ConverterType = form.get('type').value;
    if (converterType) {
      if (converterType === ConverterType.UPLINK) {
        form.get('configuration').get('encoder').patchValue(null, {emitEvent: false});
        form.get('configuration').get('tbelEncoder').patchValue(null, {emitEvent: false});
      } else {
        form.get('configuration').get('decoder').patchValue(null, {emitEvent: false});
        form.get('configuration').get('tbelDecoder').patchValue(null, {emitEvent: false});
      }
      this.setupDefaultScriptBody(form, converterType);
    }
  }

  private converterScriptLangChanged(form: UntypedFormGroup) {
    const converterType: ConverterType = form.get('type').value;
    this.setupDefaultScriptBody(form, converterType);
  }

  private setupDefaultScriptBody(form: UntypedFormGroup, converterType: ConverterType) {
    const scriptLang: ScriptLanguage = form.get('configuration').get('scriptLang').value;
    let targetField: string;
    let targetTemplate: string;
    if (scriptLang === ScriptLanguage.JS) {
      targetField = converterType === ConverterType.UPLINK ? 'decoder' : 'encoder';
      targetTemplate = converterType === ConverterType.UPLINK ? jsDecoderTemplate : jsEncoderTemplate;
    } else {
      targetField = converterType === ConverterType.UPLINK ? 'tbelDecoder' : 'tbelEncoder';
      targetTemplate = converterType === ConverterType.UPLINK ? tbelDecoderTemplate : tbelEncoderTemplate;
    }
    const scriptBody: string = form.get('configuration').get(targetField).value;
    if (!scriptBody || !scriptBody.length) {
      form.get('configuration').get(targetField).patchValue(targetTemplate, {emitEvent: false});
    }
  }

  updateForm(entity: Converter) {
    const scriptLang = entity.configuration && entity.configuration.scriptLang ? entity.configuration.scriptLang : ScriptLanguage.JS;
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type}, {emitEvent: false});
    this.entityForm.patchValue({debugMode: entity.debugMode});
    this.entityForm.patchValue({configuration:
        {
          scriptLang,
          decoder: entity.configuration ? entity.configuration.decoder : null,
          tbelDecoder: entity.configuration ? entity.configuration.tbelDecoder : null,
          encoder: entity.configuration ? entity.configuration.encoder : null,
          tbelEncoder: entity.configuration ? entity.configuration.tbelEncoder : null
        }
    });
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
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

  openConverterTestDialog(isDecoder: boolean) {
    if (this.entity.id) {
      this.converterService.getLatestConverterDebugInput(this.entity.id.id).subscribe(
        (debugIn) => {
          this.showConverterTestDialog(isDecoder, debugIn);
        }
      );
    } else {
      this.showConverterTestDialog(isDecoder, null);
    }
  }

  showConverterTestDialog(isDecoder: boolean, debugIn: ConverterDebugInput) {
    const scriptLang: ScriptLanguage = this.entityForm.get('configuration').get('scriptLang').value;
    let targetField;
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
      .afterClosed().subscribe((result) => {
        if (result !== null) {
          this.entityForm.get(`configuration.${targetField}`).patchValue(result);
          this.entityForm.get(`configuration.${targetField}`).markAsDirty();
          this.entityForm.updateValueAndValidity();
        }
    });
  }
}
