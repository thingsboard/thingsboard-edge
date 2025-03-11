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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ImageMapSourceSettings, ImageSourceType } from '@shared/models/widget/maps/map.models';
import { DataKey, DatasourceType, widgetType } from '@shared/models/widget.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

@Component({
  selector: 'tb-image-map-source-settings',
  templateUrl: './image-map-source-settings.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ImageMapSourceSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ImageMapSourceSettingsComponent),
      multi: true
    }
  ]
})
export class ImageMapSourceSettingsComponent implements OnInit, ControlValueAccessor, Validator {

  ImageSourceType = ImageSourceType;
  DatasourceType = DatasourceType;
  widgetType = widgetType;
  DataKeyType = DataKeyType;

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  private modelValue: ImageMapSourceSettings;

  private propagateChange = null;

  public imageMapSourceFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.imageMapSourceFormGroup = this.fb.group({
      sourceType: [null, [Validators.required]],
      url: [null, [Validators.required]],
      entityAliasId: [null, [Validators.required]],
      entityKey: [null, [Validators.required]]
    });
    this.imageMapSourceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.imageMapSourceFormGroup.get('sourceType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.imageMapSourceFormGroup.disable({emitEvent: false});
    } else {
      this.imageMapSourceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ImageMapSourceSettings): void {
    this.modelValue = value;
    this.imageMapSourceFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  editKey() {
    const entityKey: DataKey = this.imageMapSourceFormGroup.get('entityKey').value;
    this.context.editKey(entityKey,
      null, this.imageMapSourceFormGroup.get('entityAliasId').value).subscribe(
      (updatedDataKey) => {
        if (updatedDataKey) {
          this.imageMapSourceFormGroup.get('entityKey').patchValue(updatedDataKey);
        }
      }
    );
  }

  public validate(c: UntypedFormControl) {
    const valid = this.imageMapSourceFormGroup.valid;
    return valid ? null : {
      imageMapSource: {
        valid: false,
      },
    };
  }


  private updateValidators() {
    const sourceType: ImageSourceType = this.imageMapSourceFormGroup.get('sourceType').value;
    if (sourceType === ImageSourceType.image) {
      this.imageMapSourceFormGroup.get('url').enable({emitEvent: false});
      this.imageMapSourceFormGroup.get('entityAliasId').disable({emitEvent: false});
      this.imageMapSourceFormGroup.get('entityKey').disable({emitEvent: false});
    } else {
      this.imageMapSourceFormGroup.get('url').disable({emitEvent: false});
      this.imageMapSourceFormGroup.get('entityAliasId').enable({emitEvent: false});
      this.imageMapSourceFormGroup.get('entityKey').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.imageMapSourceFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
