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
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  Renderer2,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ComponentStyle, iconStyle, textStyle, validateCssSize } from '@shared/models/widget-settings.models';
import { UtilsService } from '@core/services/utils.service';
import { Observable, of } from 'rxjs';
import { WidgetContext } from '@home/models/widget-component.models';
import { isDefinedAndNotNull } from '@core/utils';
import {
  generateWidgetButtonToggleAppearanceCss,
  generateWidgetButtonToggleBorderLayout,
  segmentedButtonDefaultAppearance,
  segmentedButtonLayoutBorder
} from '@home/components/widget/lib/button/segmented-button-widget.models';
import { MatButtonToggleChange } from '@angular/material/button-toggle';

const initialButtonHeight = 60;
const horizontalLayoutPadding = 10;

@Component({
  selector: 'tb-widget-button-toggle',
  templateUrl: './widget-button-toggle.component.html',
  styleUrls: ['./widget-button-toggle.component.scss']
})
export class WidgetButtonToggleComponent implements OnInit, AfterViewInit, OnDestroy, OnChanges {

  @ViewChild('toggleGroupContainer', {static: false})
  toggleGroupContainer: ElementRef<HTMLElement>;

  @ViewChild('widgetButton', {read: ElementRef})
  widgetButton: ElementRef<HTMLElement>;

  @ViewChild('leftButtonContent', {static: false})
  leftButtonContent: ElementRef<HTMLElement>;
  @ViewChild('rightButtonContent', {static: false})
  rightButtonContent: ElementRef<HTMLElement>;

  @Input()
  appearance = segmentedButtonDefaultAppearance;

  @Input()
  borderRadius: string;

  @Input()
  autoScale: boolean;

  @Input()
  @coerceBoolean()
  value = false;

  @Input()
  @coerceBoolean()
  disabled = false;

  @Input()
  @coerceBoolean()
  hovered = false;

  @Input()
  @coerceBoolean()
  disableEvents = false;

  @Input()
  ctx: WidgetContext;

  @Output()
  clicked = new EventEmitter<MatButtonToggleChange>();

  leftLabel$: Observable<string>;
  rightLabel$: Observable<string>;

  leftIconStyle: ComponentStyle = {};
  rightIconStyle: ComponentStyle = {};

  leftLabelStyle: ComponentStyle = {};
  rightLabelStyle: ComponentStyle = {};

  computedBorderColor: string;
  computedBorderWidth: string;
  computedBorderRadius: string;

  private buttonResize$: ResizeObserver;

  private appearanceCssClass: string;

  constructor(private renderer: Renderer2,
              private elementRef: ElementRef,
              private utils: UtilsService) {}

  ngOnInit(): void {
    this.updateAppearance();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange) {
        if (propName === 'appearance') {
          this.updateAppearance();
        } else if (propName === 'borderRadius') {
          this.updateBorderRadius();
        } else if (propName === 'autoScale') {
          this.updateAutoScale();
        }
      }
    }
  }

  ngAfterViewInit(): void {
    this.updateAutoScale();
  }

  ngOnDestroy(): void {
    if (this.buttonResize$) {
      this.buttonResize$.disconnect();
    }
    this.clearAppearanceCss();
  }

  public validateSize() {
    if (this.appearance.autoScale && this.widgetButton.nativeElement) {
      this.onResize();
    }
  }

  private updateAppearance(): void {
    this.clearAppearanceCss();
    this.computedBorderColor = this.appearance.cardBorderColor;
    this.computedBorderWidth = validateCssSize(this.appearance.cardBorder.toString());
    if (this.appearance.leftAppearance.showIcon) {
      this.leftIconStyle = iconStyle(this.appearance.leftAppearance.iconSize, this.appearance.leftAppearance.iconSizeUnit);
    }
    if (this.appearance.rightAppearance.showIcon) {
      this.rightIconStyle = iconStyle(this.appearance.rightAppearance.iconSize, this.appearance.rightAppearance.iconSizeUnit);
    }
    if (this.appearance.leftAppearance.showLabel) {
      this.leftLabelStyle = textStyle(this.appearance.leftAppearance.labelFont);
      this.leftLabel$ = this.ctx ? this.ctx.registerLabelPattern(this.appearance.leftAppearance.label, this.leftLabel$) : of(this.appearance.leftAppearance.label);
    }
    if (this.appearance.rightAppearance.showLabel) {
      this.rightLabelStyle = textStyle(this.appearance.rightAppearance.labelFont);
      this.rightLabel$ = this.ctx ? this.ctx.registerLabelPattern(this.appearance.rightAppearance.label, this.rightLabel$) : of(this.appearance.rightAppearance.label);
    }
    this.updateBorderRadius();
    const appearanceCss = generateWidgetButtonToggleAppearanceCss(this.appearance.selectedStyle, this.appearance.unselectedStyle);
    const layoutCss = generateWidgetButtonToggleBorderLayout(this.appearance.layout);
    this.appearanceCssClass = this.utils.applyCssToElement(this.renderer, this.elementRef.nativeElement,
      'tb-widget-button', appearanceCss + layoutCss);
    this.updateAutoScale();
  }

  private updateBorderRadius(): void {
    if (this.borderRadius?.length) {
      const validatedBorderRadius = validateCssSize(this.borderRadius);
      if (validatedBorderRadius) {
        this.computedBorderRadius = validatedBorderRadius;
      } else {
        this.computedBorderRadius = this.borderRadius;
      }
    } else {
      this.computedBorderRadius = segmentedButtonLayoutBorder.get(this.appearance.layout);
    }

  }

  private clearAppearanceCss(): void {
    if (this.appearanceCssClass) {
      this.utils.clearCssElement(this.renderer, this.appearanceCssClass, this.elementRef?.nativeElement);
      this.appearanceCssClass = null;
    }
  }

  private updateAutoScale() {
    if (this.buttonResize$) {
      this.buttonResize$.disconnect();
    }
    if (this.widgetButton && this.rightButtonContent && this.leftButtonContent) {
      const autoScale = isDefinedAndNotNull(this.autoScale) ? this.autoScale : this.appearance.autoScale;
      if (autoScale) {
        this.buttonResize$ = new ResizeObserver(() => {
          this.onResize();
        });
        this.buttonResize$.observe(this.widgetButton.nativeElement);
        this.onResize();
      } else {
        this.renderer.setStyle(this.widgetButton.nativeElement, 'transform', 'none');
        this.renderer.setStyle(this.widgetButton.nativeElement, 'width', '100%');
      }
    }
  }

  private onResize() {
    const height = this.widgetButton.nativeElement.getBoundingClientRect().height;
    const buttonScale = height / initialButtonHeight;
    const buttonWidth = this.widgetButton.nativeElement.getBoundingClientRect().width;
    const buttonHeight = this.widgetButton.nativeElement.getBoundingClientRect().height;
    this.renderer.setStyle(this.leftButtonContent.nativeElement, 'transform', `scale(1)`);
    this.renderer.setStyle(this.rightButtonContent.nativeElement, 'transform', `scale(1)`);
    const contentWidth = this.leftButtonContent.nativeElement.getBoundingClientRect().width;
    const contentHeight = this.leftButtonContent.nativeElement.getBoundingClientRect().height;
    const maxScale = Math.max(1, buttonScale);
    const scale = Math.min(Math.min((buttonWidth / 2 - horizontalLayoutPadding) / contentWidth, buttonHeight / contentHeight), maxScale);
    this.renderer.setStyle(this.leftButtonContent.nativeElement, 'transform', `scale(${scale})`);
    this.renderer.setStyle(this.rightButtonContent.nativeElement, 'transform', `scale(${scale})`);
  }
}
