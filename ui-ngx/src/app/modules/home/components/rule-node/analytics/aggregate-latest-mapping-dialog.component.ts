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

import { AggLatestMapping, AggLatestMappingFilter } from './aggregate-latest-mapping.models';
import { Component, Inject, OnInit } from '@angular/core';
import {
  AttributeScope,
  DialogComponent,
  LatestTelemetry,
  ScriptLanguage,
  telemetryTypeTranslations
} from '@shared/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState, getCurrentAuthState } from '@core/public-api';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';
import { AggMathFunction, aggMathFunctionTranslations } from '@home/components/rule-node/rule-node-config.models';

export interface AggregateLatestMappingDialogData {
  isAdd: boolean;
  mapping?: AggLatestMapping;
  tbelFilterFunctionOnly: boolean;
}

@Component({
  selector: 'tb-agg-latest-mapping-dialog',
  templateUrl: './aggregate-latest-mapping-dialog.component.html',
  styleUrls: []
})
export class AggregateLatestMappingDialogComponent
  extends DialogComponent<AggregateLatestMappingDialogComponent, AggLatestMapping> implements OnInit {

  latestTelemetry = LatestTelemetry;
  attributeScope = AttributeScope;

  attributeScopes = Object.keys(AttributeScope);
  telemetryTypeTranslationMap = telemetryTypeTranslations;

  mathFunctions = Object.keys(AggMathFunction);
  mathFunctionsTranslationMap = aggMathFunctionTranslations;

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  aggLatestMappingFormGroup: UntypedFormGroup;

  isAdd: boolean;
  mapping: AggLatestMapping;
  filterEntities: boolean;

  tbelFilterFunctionOnly: boolean;

  tbelEnabled = getCurrentAuthState(this.store).tbelEnabled;

  scriptLanguage = ScriptLanguage;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AggregateLatestMappingDialogData,
              public dialogRef: MatDialogRef<AggregateLatestMappingDialogComponent, AggLatestMapping>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    this.mapping = data.mapping;
    this.tbelFilterFunctionOnly = data.tbelFilterFunctionOnly;
    if (this.isAdd && !this.mapping) {
      this.mapping = {
        source: '',
        sourceScope: LatestTelemetry.LATEST_TELEMETRY,
        defaultValue: 0,
        target: '',
        aggFunction: AggMathFunction.AVG
      }
    }
    this.filterEntities = this.mapping.filter ? true : false;
  }

  ngOnInit(): void {
    let scriptLang = this.mapping.filter?.scriptLang;
    if (!scriptLang) {
      if (this.mapping.filter?.filterFunction && !this.mapping.filter?.tbelFilterFunction) {
        scriptLang = ScriptLanguage.JS;
      } else {
        scriptLang = ScriptLanguage.TBEL;
      }
    }
    if (!this.tbelEnabled && scriptLang === ScriptLanguage.TBEL && !this.tbelFilterFunctionOnly) {
      scriptLang = ScriptLanguage.JS;
      if (this.mapping.filter && !this.mapping.filter.filterFunction) {
        this.mapping.filter.filterFunction = 'return true;';
      }
    } else if (this.tbelEnabled && this.tbelFilterFunctionOnly && scriptLang !== ScriptLanguage.TBEL) {
      scriptLang = ScriptLanguage.TBEL;
      if (this.mapping.filter && !this.mapping.filter.tbelFilterFunction) {
        this.mapping.filter.tbelFilterFunction = 'return true;';
      }
    }
    this.aggLatestMappingFormGroup = this.fb.group({
      sourceScope: [this.mapping .sourceScope, []],
      source: [this.mapping.source, []],
      defaultValue: [this.mapping.defaultValue, [Validators.required]],
      aggFunction: [this.mapping.aggFunction, []],
      target: [this.mapping.target, [Validators.required]],
      filter: this.fb.group(
        {
          clientAttributeNames: [this.mapping.filter ? this.mapping.filter.clientAttributeNames : [], []],
          sharedAttributeNames: [this.mapping.filter ? this.mapping.filter.sharedAttributeNames : [], []],
          serverAttributeNames: [this.mapping.filter ? this.mapping.filter.serverAttributeNames : [], []],
          latestTsKeyNames: [this.mapping.filter ? this.mapping.filter.latestTsKeyNames : [], []],
          scriptLang: [scriptLang, []],
          filterFunction: [this.mapping.filter ? this.mapping.filter.filterFunction : null, []],
          tbelFilterFunction: [this.mapping.filter ? this.mapping.filter.tbelFilterFunction : null, []]
        }
      )
    });
    if (!this.filterEntities) {
      this.aggLatestMappingFormGroup.get('filter').disable({emitEvent: false});
    }
  }

  filterEntitiesChange() {
    if (this.filterEntities) {
      this.aggLatestMappingFormGroup.get('filter').setValue({
        clientAttributeNames: [],
        sharedAttributeNames: [],
        serverAttributeNames: [],
        latestTsKeyNames: [],
        scriptLang: (this.tbelEnabled || this.tbelFilterFunctionOnly) ? ScriptLanguage.TBEL : ScriptLanguage.JS,
        filterFunction: 'return true;',
        tbelFilterFunction: 'return true;'
      } as AggLatestMappingFilter, {emitEvent: true});
      this.aggLatestMappingFormGroup.get('filter').enable({emitEvent: true});
    } else {
      this.aggLatestMappingFormGroup.get('filter').disable({emitEvent: true});
    }
    this.aggLatestMappingFormGroup.get('filter').markAsDirty();
  }

  removeKey(key: string, keysField: string): void {
    const keys: string[] = this.aggLatestMappingFormGroup.get('filter').get(keysField).value;
    const index = keys.indexOf(key);
    if (index >= 0) {
      keys.splice(index, 1);
      this.aggLatestMappingFormGroup.get('filter').get(keysField).setValue(keys, {emitEvent: true});
      this.aggLatestMappingFormGroup.get('filter').get(keysField).markAsDirty();
    }
  }

  addKey(event: MatChipInputEvent, keysField: string): void {
    const input = event.input;
    let value = event.value;
    if ((value || '').trim()) {
      value = value.trim();
      let keys: string[] = this.aggLatestMappingFormGroup.get('filter').get(keysField).value;
      if (!keys || keys.indexOf(value) === -1) {
        if (!keys) {
          keys = [];
        }
        keys.push(value);
        this.aggLatestMappingFormGroup.get('filter').get(keysField).setValue(keys, {emitEvent: true});
        this.aggLatestMappingFormGroup.get('filter').get(keysField).markAsDirty();
      }
    }
    if (input) {
      input.value = '';
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    this.mapping = this.aggLatestMappingFormGroup.value;
    this.dialogRef.close(this.mapping);
  }

}
