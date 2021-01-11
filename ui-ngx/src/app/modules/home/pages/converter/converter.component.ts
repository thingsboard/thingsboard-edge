///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import {
  Converter,
  ConverterDebugInput,
  ConverterType,
  converterTypeTranslationMap
} from '@shared/models/converter.models';

import jsDecoderTemplate from '!raw-loader!./js-decoder.raw';
import jsEncoderTemplate from '!raw-loader!./js-encoder.raw';
import { ConverterService } from '@core/http/converter.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { MatDialog } from '@angular/material/dialog';
import {
  ConverterTestDialogComponent,
  ConverterTestDialogData
} from '@home/pages/converter/converter-test-dialog.component';

@Component({
  selector: 'tb-converter',
  templateUrl: './converter.component.html',
  styleUrls: ['./converter.component.scss']
})
export class ConverterComponent extends EntityComponent<Converter> {

  converterType = ConverterType;

  converterTypes = Object.keys(ConverterType);

  converterTypeTranslations = converterTypeTranslationMap;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private converterService: ConverterService,
              private dialog: MatDialog,
              @Inject('entity') protected entityValue: Converter,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Converter>,
              protected fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue);
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

  buildForm(entity: Converter): FormGroup {
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        type: [entity ? entity.type : null, [Validators.required]],
        debugMode: [entity ? entity.debugMode : null],
        configuration: this.fb.group(
          {
            decoder: [entity && entity.configuration ? entity.configuration.decoder : null],
            encoder: [entity && entity.configuration ? entity.configuration.encoder : null],
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
    this.checkIsNewConverter(entity, form);
    return form;
  }

  private checkIsNewConverter(entity: Converter, form: FormGroup) {
    if (entity && !entity.id) {
      form.get('type').patchValue(ConverterType.UPLINK, {emitEvent: true});
    }
  }

  private converterTypeChanged(form: FormGroup) {
    const converterType: ConverterType = form.get('type').value;
    if (converterType) {
      if (converterType === ConverterType.UPLINK) {
        form.get('configuration').get('encoder').patchValue(null, {emitEvent: false});
        const decoder: string = form.get('configuration').get('decoder').value;
        if (!decoder || !decoder.length) {
          form.get('configuration').get('decoder').patchValue(jsDecoderTemplate, {emitEvent: false});
        }
      } else {
        form.get('configuration').get('decoder').patchValue(null, {emitEvent: false});
        const encoder: string = form.get('configuration').get('encoder').value;
        if (!encoder || !encoder.length) {
          form.get('configuration').get('encoder').patchValue(jsEncoderTemplate, {emitEvent: false});
        }
      }
    }
  }

  updateForm(entity: Converter) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type}, {emitEvent: false});
    this.entityForm.patchValue({debugMode: entity.debugMode});
    this.entityForm.patchValue({configuration:
        {
          decoder: entity.configuration ? entity.configuration.decoder : null,
          encoder: entity.configuration ? entity.configuration.encoder : null
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
    const funcBody = isDecoder ? this.entityForm.get('configuration').get('decoder').value :
      this.entityForm.get('configuration').get('encoder').value;
    this.dialog.open<ConverterTestDialogComponent, ConverterTestDialogData, string>(ConverterTestDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-sm'],
        data: {
          debugIn,
          isDecoder,
          funcBody
        }
      })
      .afterClosed().subscribe((result) => {
        if (result !== null) {
          if (isDecoder) {
            this.entityForm.get('configuration').get('decoder').patchValue(result);
          } else {
            this.entityForm.get('configuration').get('encoder').patchValue(result);
          }
        }
    });
  }
}
