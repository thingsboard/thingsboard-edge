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

import { ChangeDetectorRef, Component, forwardRef, Input, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { MapDataLayerType, ShapeFillStripeSettings } from '@shared/models/widget/maps/map.models';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { isDefinedAndNotNull, plainColorFromVariable, stringToBase64 } from '@core/utils';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { DatasourceType } from '@shared/models/widget.models';
import {
  ShapeFillStripeSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/shape-fill-stripe-settings-panel.component';

@Component({
  selector: 'tb-shape-fill-stripe-settings',
  templateUrl: './shape-fill-stripe-settings.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ShapeFillStripeSettingsComponent),
      multi: true
    }
  ]
})
export class ShapeFillStripeSettingsComponent implements ControlValueAccessor {

  @Input()
  disabled: boolean;

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

  modelValue: ShapeFillStripeSettings;

  stripePreviewUrl: SafeUrl;

  private propagateChange: (v: any) => void = () => { };

  constructor(private popoverService: TbPopoverService,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef,
              private viewContainerRef: ViewContainerRef) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: ShapeFillStripeSettings): void {
    if (value) {
      this.modelValue = value;
    }
    this.updatePreview();
  }

  openStripeSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ShapeFillStripeSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'left',
        context: {
          shapeFillStripeSettings: this.modelValue,
          context: this.context,
          dsType: this.dsType,
          dsEntityAliasId: this.dsEntityAliasId,
          dsDeviceId: this.dsDeviceId,
          dataLayerType: this.dataLayerType
        },
        isModal: true
      }).tbComponentRef.instance.shapeFillStripeSettingsApplied.subscribe((shapeFillStripeSettings) => {
        this.modelValue = shapeFillStripeSettings;
        this.updatePreview();
        this.propagateChange(this.modelValue);
        this.cd.detectChanges();
      });
    }
  }

  private updatePreview() {
    this.stripePreviewUrl = this.sanitizer.bypassSecurityTrustUrl(generateStripePreviewUrl(this.modelValue));
  }
}

export const generateStripePreviewUrl = (settings: ShapeFillStripeSettings): string => {
  const weight = isDefinedAndNotNull(settings?.weight) ? settings.weight : 3;
  const spaceWeight = isDefinedAndNotNull(settings?.spaceWeight) ? settings.spaceWeight : 9;
  const angle = isDefinedAndNotNull(settings?.angle) ? settings.angle : 45;
  const height = weight + spaceWeight;
  const color = plainColorFromVariable(settings?.color?.color || '#8f8f8f');
  const spaceColor = plainColorFromVariable(settings?.spaceColor?.color || 'rgba(143,143,143,0)');
  const svgStr = `<svg x="0" y="0" width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="0" y="0" width="48" height="48" fill="url(#stripePattern)" fill-opacity="1"></rect>
        <defs>
          <pattern id="stripePattern" x="0" y="0" width="8" height="${height}" patternUnits="userSpaceOnUse"
                    patternContentUnits="userSpaceOnUse" patternTransform="rotate(${angle})">
              <path d="M0 ${weight/2} H 8" stroke="${color}" stroke-width="${weight}" stroke-opacity="1"></path>
              <path d="M0 ${weight + spaceWeight/2} H 8" stroke="${spaceColor}" stroke-width="${spaceWeight}" stroke-opacity="1"></path>
          </pattern>
        </defs>
      </svg>`;
  const encodedSvg = stringToBase64(svgStr);
  return `data:image/svg+xml;base64,${encodedSvg}`;
}
