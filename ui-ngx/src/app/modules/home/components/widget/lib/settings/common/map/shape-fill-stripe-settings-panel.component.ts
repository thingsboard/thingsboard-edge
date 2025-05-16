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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MapDataLayerType, ShapeFillStripeSettings } from '@shared/models/widget/maps/map.models';
import { DomSanitizer } from '@angular/platform-browser';
import {
  generateStripePreviewUrl
} from '@home/components/widget/lib/settings/common/map/shape-fill-stripe-settings.component';
import { ComponentStyle } from '@shared/models/widget-settings.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { DatasourceType } from '@shared/models/widget.models';

@Component({
  selector: 'tb-shape-fill-stripe-settings-panel',
  templateUrl: './shape-fill-stripe-settings-panel.component.html',
  providers: [],
  styleUrls: ['./shape-fill-stripe-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ShapeFillStripeSettingsPanelComponent implements OnInit {

  @Input()
  shapeFillStripeSettings: ShapeFillStripeSettings;

  @Input()
  context: MapSettingsContext;

  @Input()
  dsType: DatasourceType;

  @Input()
  dsEntityAliasId: string;

  @Input()
  dsDeviceId: string;

  @Input()
  dataLayerType: MapDataLayerType;

  @Output()
  shapeFillStripeSettingsApplied = new EventEmitter<ShapeFillStripeSettings>();

  stripePreviewStyle: ComponentStyle;

  shapeFillStripeSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private sanitizer: DomSanitizer,
              private popover: TbPopoverComponent,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.shapeFillStripeSettingsFormGroup = this.fb.group(
      {
        weight: [this.shapeFillStripeSettings?.weight, [Validators.min(0)]],
        color: [this.shapeFillStripeSettings?.color, []],
        spaceWeight: [this.shapeFillStripeSettings?.spaceWeight, [Validators.min(0)]],
        spaceColor: [this.shapeFillStripeSettings?.spaceColor, []],
        angle: [this.shapeFillStripeSettings?.angle, [Validators.min(0), Validators.max(180)]]
      }
    );
    this.shapeFillStripeSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updatePreview();
    });
    this.updatePreview();
  }

  cancel() {
    this.popover?.hide();
  }

  applyShapeFillStripeSettings() {
    const shapeFillStripeSettings: ShapeFillStripeSettings = this.shapeFillStripeSettingsFormGroup.value;
    this.shapeFillStripeSettingsApplied.emit(shapeFillStripeSettings);
    this.popover?.hide();
  }

  private updatePreview() {
    const shapeFillStripeSettings: ShapeFillStripeSettings = this.shapeFillStripeSettingsFormGroup.value;
    const previewUrl = generateStripePreviewUrl(shapeFillStripeSettings, 136, 118);
    this.stripePreviewStyle = {
      background: this.sanitizer.bypassSecurityTrustStyle(`url(${previewUrl}) no-repeat 50% 50% / cover`)
    };
  }

}
