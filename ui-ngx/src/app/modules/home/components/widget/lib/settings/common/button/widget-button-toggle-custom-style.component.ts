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

import {
  ChangeDetectorRef,
  Component,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  Renderer2,
  SimpleChanges,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TbPopoverService } from '@shared/components/popover.service';
import { MatIconButton } from '@angular/material/button';
import { deepClone } from '@core/utils';
import {
  ButtonToggleAppearance,
  WidgetButtonToggleCustomStyle,
  WidgetButtonToggleState
} from '@home/components/widget/lib/button/segmented-button-widget.models';
import {
  WidgetButtonToggleCustomStylePanelComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-toggle-custom-style-panel.component';

@Component({
  selector: 'tb-widget-button-toggle-custom-style',
  templateUrl: './widget-button-toggle-custom-style.component.html',
  styleUrls: ['./widget-button-toggle-custom-style.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WidgetButtonToggleCustomStyleComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class WidgetButtonToggleCustomStyleComponent implements OnInit, OnChanges, ControlValueAccessor {

  @Input()
  disabled = false;

  @Input()
  value = false;

  @Input()
  appearance: ButtonToggleAppearance;

  @Input()
  borderRadius: string;

  @Input()
  autoScale: boolean;

  @Input()
  state: WidgetButtonToggleState;

  widgetButtonToggleState = WidgetButtonToggleState;

  modelValue: WidgetButtonToggleCustomStyle;

  previewAppearance: ButtonToggleAppearance;

  private propagateChange = (_val: any) => {};

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.updatePreviewAppearance();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange) {
        if (propName === 'appearance') {
          this.updatePreviewAppearance();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(_isDisabled: boolean): void {
  }

  writeValue(value: WidgetButtonToggleCustomStyle): void {
    this.modelValue = value;
    this.updatePreviewAppearance();
  }

  clearStyle() {
    this.updateModel(null);
  }

  openButtonCustomStylePopup($event: Event, matButton: MatIconButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const widgetButtonCustomStylePanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: WidgetButtonToggleCustomStylePanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftTopOnly', 'leftOnly', 'leftBottomOnly'],
        context: {
          appearance: this.appearance,
          borderRadius: this.borderRadius,
          autoScale: this.autoScale,
          state: this.state,
          value: this.value,
          customStyle: this.modelValue
        },
        isModal: true
      });
      widgetButtonCustomStylePanelPopover.tbComponentRef.instance.popover = widgetButtonCustomStylePanelPopover;
      widgetButtonCustomStylePanelPopover.tbComponentRef.instance.customStyleApplied.subscribe((customStyle) => {
        widgetButtonCustomStylePanelPopover.hide();
        this.updateModel(customStyle);
      });
    }
  }

  private updateModel(value: WidgetButtonToggleCustomStyle): void {
    this.modelValue = value;
    this.updatePreviewAppearance();
    this.propagateChange(this.modelValue);
  }

  private updatePreviewAppearance() {
    this.previewAppearance = deepClone(this.appearance);
    if (this.modelValue) {
      if (this.value) {
        this.previewAppearance.selectedStyle.customStyle[this.state] = this.modelValue;
      } else {
        this.previewAppearance.unselectedStyle.customStyle[this.state] = this.modelValue;
      }
    }
    this.cd.markForCheck();
  }
}
