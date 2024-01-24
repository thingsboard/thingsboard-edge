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
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  Renderer2,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { BasicRpcStateWidgetComponent } from '@home/components/widget/lib/rpc/rpc-widget.models';
import {
  singleSwitchDefaultSettings,
  SingleSwitchLayout,
  SingleSwitchWidgetSettings
} from '@home/components/widget/lib/rpc/single-switch-widget.models';
import { ComponentStyle, iconStyle, textStyle } from '@shared/models/widget-settings.models';
import { Observable } from 'rxjs';
import { ResizeObserver } from '@juggle/resize-observer';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import cssjs from '@core/css/css';
import { hashCode } from '@core/utils';
import { RpcInitialStateSettings, RpcUpdateStateSettings } from '@shared/models/rpc-widget-settings.models';
import { ValueType } from '@shared/models/constants';

const horizontalLayoutPadding = 48;
const verticalLayoutPadding = 36;

@Component({
  selector: 'tb-single-switch-widget',
  templateUrl: './single-switch-widget.component.html',
  styleUrls: ['./single-switch-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class SingleSwitchWidgetComponent extends
  BasicRpcStateWidgetComponent<boolean, SingleSwitchWidgetSettings> implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('singleSwitchPanel', {static: false})
  singleSwitchPanel: ElementRef<HTMLElement>;

  @ViewChild('singleSwitchContent', {static: false})
  singleSwitchContent: ElementRef<HTMLElement>;

  @ViewChild('singleSwitchLabelRow', {static: false})
  singleSwitchLabelRow: ElementRef<HTMLElement>;

  @ViewChild('singleSwitchToggleRow', {static: false})
  singleSwitchToggleRow: ElementRef<HTMLElement>;

  layout: SingleSwitchLayout;

  showIcon = false;
  icon = '';
  iconStyle: ComponentStyle = {};

  showLabel = true;
  label$: Observable<string>;
  labelStyle: ComponentStyle = {};

  showOnLabel = false;
  onLabel = '';
  onLabelStyle: ComponentStyle = {};

  showOffLabel = false;
  offLabel = '';
  offLabelStyle: ComponentStyle = {};

  autoScale = false;

  private panelResize$: ResizeObserver;

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              private renderer: Renderer2,
              protected cd: ChangeDetectorRef,
              private elementRef: ElementRef) {
    super(imagePipe, sanitizer, cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.layout = this.settings.layout;

    this.autoScale = this.settings.autoScale;

    this.showLabel = this.settings.showLabel;
    this.label$ = this.ctx.registerLabelPattern(this.settings.label, this.label$);
    this.labelStyle = textStyle(this.settings.labelFont);
    this.labelStyle.color = this.settings.labelColor;

    this.showIcon = this.settings.showIcon;
    this.icon = this.settings.icon;
    this.iconStyle = iconStyle(this.settings.iconSize, this.settings.iconSizeUnit );
    this.iconStyle.color = this.settings.iconColor;

    this.showOnLabel = this.settings.showOnLabel;
    this.onLabel = this.settings.onLabel;
    this.onLabelStyle = textStyle(this.settings.onLabelFont);
    this.onLabelStyle.color = this.settings.onLabelColor;

    this.showOffLabel = this.settings.showOffLabel;
    this.offLabel = this.settings.offLabel;
    this.offLabelStyle = textStyle(this.settings.offLabelFont);
    this.offLabelStyle.color = this.settings.offLabelColor;
    const switchVariablesCss = `.tb-single-switch-panel {\n`+
                                           `--tb-single-switch-tumbler-color-on: ${this.settings.tumblerColorOn};\n`+
                                           `--tb-single-switch-tumbler-color-off: ${this.settings.tumblerColorOff};\n`+
                                           `--tb-single-switch-tumbler-color-disabled: ${this.settings.tumblerColorDisabled};\n`+
                                           `--tb-single-switch-color-on: ${this.settings.switchColorOn};\n`+
                                           `--tb-single-switch-color-off: ${this.settings.switchColorOff};\n`+
                                           `--tb-single-switch-color-disabled: ${this.settings.switchColorDisabled};\n`+
                                      `}`;
    const cssParser = new cssjs();
    cssParser.testMode = false;
    const namespace = 'single-switch-' + hashCode(switchVariablesCss);
    cssParser.cssPreviewNamespace = namespace;
    cssParser.createStyleElement(namespace, switchVariablesCss);
    this.renderer.addClass(this.elementRef.nativeElement, namespace);
  }

  ngAfterViewInit(): void {
    if (this.autoScale) {
      this.renderer.setStyle(this.singleSwitchContent.nativeElement, 'overflow', 'visible');
      this.renderer.setStyle(this.singleSwitchContent.nativeElement, 'position', 'absolute');
      this.panelResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.panelResize$.observe(this.singleSwitchPanel.nativeElement);
      if (this.showLabel) {
        this.panelResize$.observe(this.singleSwitchLabelRow.nativeElement);
      }
      this.onResize();
    }
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    if (this.panelResize$) {
      this.panelResize$.disconnect();
    }
  }

  protected stateValueType(): ValueType {
    return ValueType.BOOLEAN;
  }

  protected defaultValue(): boolean {
    return false;
  }

  protected defaultSettings(): SingleSwitchWidgetSettings {
    return {...singleSwitchDefaultSettings};
  }

  protected initialState(): RpcInitialStateSettings<boolean> {
    return {...this.settings.initialState, actionLabel: this.ctx.translate.instant('widgets.rpc-state.initial-state')};
  }

  protected getUpdateStateSettingsForValue(value: boolean): RpcUpdateStateSettings {
    const targetSettings = value ? this.settings.onUpdateState : this.settings.offUpdateState;
    return {...targetSettings, actionLabel: this.ctx.translate.instant(value ? 'widgets.rpc-state.turn-on' : 'widgets.rpc-state.turn-off')};
  }

  protected validateValue(value: any): boolean {
    return !!value;
  }

  private onResize() {
    const panelWidth = this.singleSwitchPanel.nativeElement.getBoundingClientRect().width - horizontalLayoutPadding;
    const panelHeight = this.singleSwitchPanel.nativeElement.getBoundingClientRect().height - verticalLayoutPadding;
    this.renderer.setStyle(this.singleSwitchContent.nativeElement, 'transform', `scale(1)`);
    let contentWidth = this.singleSwitchToggleRow.nativeElement.getBoundingClientRect().width;
    let contentHeight = this.singleSwitchToggleRow.nativeElement.getBoundingClientRect().height;
    if (this.showIcon || this.showLabel) {
      contentWidth += (8 + this.singleSwitchLabelRow.nativeElement.getBoundingClientRect().width);
      contentHeight = Math.max(contentHeight, this.singleSwitchLabelRow.nativeElement.getBoundingClientRect().height);
    }
    const scale = Math.min(panelWidth / contentWidth, panelHeight / contentHeight);
    const width = panelWidth / scale;
    this.renderer.setStyle(this.singleSwitchContent.nativeElement, 'width', width + 'px');
    this.renderer.setStyle(this.singleSwitchContent.nativeElement, 'transform', `scale(${scale})`);
  }

}
