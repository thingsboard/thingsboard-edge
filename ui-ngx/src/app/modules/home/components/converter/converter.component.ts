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

import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
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
} from '@home/components/converter/converter-test-dialog.component';

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

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private converterService: ConverterService,
              private dialog: MatDialog,
              @Optional() @Inject('entity') protected entityValue: Converter,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Converter>,
              protected fb: FormBuilder,
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

  buildForm(entity: Converter): FormGroup {
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255), Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
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
    } else {
      form.get('type').disable({emitEvent: false});
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
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
        data: {
          debugIn,
          isDecoder,
          funcBody
        }
      })
      .afterClosed().subscribe((result) => {
        if (result !== null) {
          if (isDecoder) {
            this.entityForm.get('configuration.decoder').patchValue(result);
            this.entityForm.get('configuration.decoder').markAsDirty();
          } else {
            this.entityForm.get('configuration.encoder').patchValue(result);
            this.entityForm.get('configuration.encoder').markAsDirty();
          }
          this.entityForm.updateValueAndValidity();
        }
    });
  }
}
