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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ShapeFillImageSettings, ShapeFillImageType } from '@shared/models/widget/maps/map.models';

@Component({
  selector: 'tb-shape-fill-image-settings-panel',
  templateUrl: './shape-fill-image-settings-panel.component.html',
  providers: [],
  styleUrls: ['./shape-fill-image-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ShapeFillImageSettingsPanelComponent implements OnInit {

  @Input()
  shapeFillImageSettings: ShapeFillImageSettings;

  @Output()
  shapeFillImageSettingsApplied = new EventEmitter<ShapeFillImageSettings>();

  ShapeFillImageType = ShapeFillImageType;

  shapeFillImageSettingsFormGroup: UntypedFormGroup;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(private fb: UntypedFormBuilder,
              private popover: TbPopoverComponent,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.shapeFillImageSettingsFormGroup = this.fb.group(
      {
        type: [this.shapeFillImageSettings?.type || ShapeFillImageType.image, []],
        image: [this.shapeFillImageSettings?.image, [Validators.required]],
        preserveAspectRatio: [this.shapeFillImageSettings?.preserveAspectRatio, []],
        opacity: [this.shapeFillImageSettings?.opacity, [Validators.min(0), Validators.max(1)]],
        angle: [this.shapeFillImageSettings?.angle, [Validators.min(0), Validators.max(360)]],
        scale: [this.shapeFillImageSettings?.scale, [Validators.min(0)]],
        imageFunction: [this.shapeFillImageSettings?.imageFunction, [Validators.required]],
        images: [this.shapeFillImageSettings?.images, []]
      }
    );
    this.shapeFillImageSettingsFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applyShapeFillImageSettings() {
    const shapeFillImageSettings: ShapeFillImageSettings = this.shapeFillImageSettingsFormGroup.value;
    this.shapeFillImageSettingsApplied.emit(shapeFillImageSettings);
    this.popover?.hide();
  }

  private updateValidators() {
    const type: ShapeFillImageType = this.shapeFillImageSettingsFormGroup.get('type').value;
    if (type === ShapeFillImageType.image) {
      this.shapeFillImageSettingsFormGroup.get('image').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('preserveAspectRatio').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('opacity').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('angle').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('scale').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('imageFunction').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('images').disable({emitEvent: false});
    } else {
      this.shapeFillImageSettingsFormGroup.get('image').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('preserveAspectRatio').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('opacity').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('angle').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('scale').disable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('imageFunction').enable({emitEvent: false});
      this.shapeFillImageSettingsFormGroup.get('images').enable({emitEvent: false});
    }
  }

}
