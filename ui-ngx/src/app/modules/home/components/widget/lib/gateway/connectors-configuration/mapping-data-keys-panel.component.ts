///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import {
  AbstractControl,
  UntypedFormArray,
  UntypedFormBuilder,
  Validators
} from '@angular/forms';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { isDefinedAndNotNull } from '@core/utils';
import {
  MappingDataKey,
  MappingKeysType,
  MappingValueType,
  mappingValueTypesMap,
  noLeadTrailSpacesRegex
} from '@home/components/widget/lib/gateway/gateway-widget.models';

@Component({
  selector: 'tb-mapping-data-keys-panel',
  templateUrl: './mapping-data-keys-panel.component.html',
  styleUrls: ['./mapping-data-keys-panel.component.scss'],
  providers: []
})
export class MappingDataKeysPanelComponent extends PageComponent implements OnInit {

  @Input()
  panelTitle: string;

  @Input()
  addKeyTitle: string;

  @Input()
  deleteKeyTitle: string;

  @Input()
  noKeysText: string;

  @Input()
  keys: Array<MappingDataKey> | {[key: string]: any};

  @Input()
  keysType: string;

  @Input()
  @coerceBoolean()
  rawData = false;

  @Input()
  popover: TbPopoverComponent<MappingDataKeysPanelComponent>;

  @Output()
  keysDataApplied = new EventEmitter<Array<MappingDataKey> | {[key: string]: any}>();

  valueTypeKeys = Object.values(MappingValueType);

  MappingKeysType = MappingKeysType;

  valueTypeEnum = MappingValueType;

  valueTypes = mappingValueTypesMap;

  dataKeyType: DataKeyType;

  keysListFormArray: UntypedFormArray;

  errorText = '';

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    this.keysListFormArray = this.prepareKeysFormArray(this.keys)
  }

  trackByKey(index: number, keyControl: AbstractControl): any {
    return keyControl;
  }

  addKey(): void {
    const dataKeyFormGroup = this.fb.group({
      key: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      value: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]]
    });
    if (this.keysType !== MappingKeysType.CUSTOM) {
      dataKeyFormGroup.addControl('type', this.fb.control(this.rawData ? 'raw' : MappingValueType.STRING));
    }
    this.keysListFormArray.push(dataKeyFormGroup);
  }

  deleteKey($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.keysListFormArray.removeAt(index);
    this.keysListFormArray.markAsDirty();
  }

  cancel() {
    this.popover?.hide();
  }

  applyKeysData() {
    let keys = this.keysListFormArray.value;
    if (this.keysType === MappingKeysType.CUSTOM) {
      keys = {};
      for (let key of this.keysListFormArray.value) {
        keys[key.key] = key.value;
      }
    }
    this.keysDataApplied.emit(keys);
  }

  private prepareKeysFormArray(keys: Array<MappingDataKey> | {[key: string]: any}): UntypedFormArray {
    const keysControlGroups: Array<AbstractControl> = [];
    if (keys) {
      if (this.keysType === MappingKeysType.CUSTOM) {
        keys = Object.keys(keys).map(key => {
          return {key, value: keys[key], type: ''};
        });
      }
      keys.forEach((keyData) => {
        const { key, value, type } = keyData;
        const dataKeyFormGroup = this.fb.group({
          key: [key, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
          value: [value, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
          type: [type, []]
        });
        keysControlGroups.push(dataKeyFormGroup);
      });
    }
    return this.fb.array(keysControlGroups);
  }

  valueTitle(value: any): string {
    if (isDefinedAndNotNull(value)) {
      if (typeof value === 'object') {
        return JSON.stringify(value);
      }
      return value;
    }
    return '';
  }

}
