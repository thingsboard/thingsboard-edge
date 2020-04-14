///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, Inject, Input, OnDestroy, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import {
  cloneSelectedTimewindow,
  HistoryWindowType,
  initModelFromDefaultTimewindow,
  Timewindow,
  TimewindowType
} from '@shared/models/time/time.models';
import { DatePipe, DOCUMENT } from '@angular/common';
import { CdkOverlayOrigin, ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import {
  TIMEWINDOW_PANEL_DATA,
  TimewindowPanelComponent,
  TimewindowPanelData
} from '@shared/components/time/timewindow-panel.component';
import { ComponentPortal, PortalInjector } from '@angular/cdk/portal';
import { MediaBreakpoints } from '@shared/models/constants';
import { BreakpointObserver } from '@angular/cdk/layout';
import { WINDOW } from '@core/services/window.service';
import { TimeService } from '@core/services/time.service';
import { TooltipPosition } from '@angular/material/tooltip';
import { deepClone } from '@core/utils';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

// @dynamic
@Component({
  selector: 'tb-timewindow',
  templateUrl: './timewindow.component.html',
  styleUrls: ['./timewindow.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimewindowComponent),
      multi: true
    }
  ]
})
export class TimewindowComponent implements OnInit, OnDestroy, ControlValueAccessor {

  historyOnlyValue = false;

  @Input()
  set historyOnly(val) {
    this.historyOnlyValue = coerceBooleanProperty(val);
  }

  get historyOnly() {
    return this.historyOnlyValue;
  }

  aggregationValue = false;

  @Input()
  set aggregation(val) {
    this.aggregationValue = coerceBooleanProperty(val);
  }

  get aggregation() {
    return this.aggregationValue;
  }

  isToolbarValue = false;

  @Input()
  set isToolbar(val) {
    this.isToolbarValue = coerceBooleanProperty(val);
  }

  get isToolbar() {
    return this.isToolbarValue;
  }

  asButtonValue = false;

  @Input()
  set asButton(val) {
    this.asButtonValue = coerceBooleanProperty(val);
  }

  get asButton() {
    return this.asButtonValue;
  }

  isEditValue = false;

  @Input()
  set isEdit(val) {
    this.isEditValue = coerceBooleanProperty(val);
    this.timewindowDisabled = this.isTimewindowDisabled();
  }

  get isEdit() {
    return this.isEditValue;
  }

  @Input()
  direction: 'left' | 'right' = 'left';

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  @Input() disabled: boolean;

  @ViewChild('timewindowPanelOrigin') timewindowPanelOrigin: CdkOverlayOrigin;

  innerValue: Timewindow;

  timewindowDisabled: boolean;

  private propagateChange = (_: any) => {};

  constructor(private translate: TranslateService,
              private timeService: TimeService,
              private millisecondsToTimeStringPipe: MillisecondsToTimeStringPipe,
              private datePipe: DatePipe,
              private overlay: Overlay,
              public viewContainerRef: ViewContainerRef,
              public breakpointObserver: BreakpointObserver,
              @Inject(DOCUMENT) private document: Document,
              @Inject(WINDOW) private window: Window) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  openEditMode() {
    if (this.timewindowDisabled) {
      return;
    }
    const isGtXs = this.breakpointObserver.isMatched(MediaBreakpoints['gt-xs']);
    const position = this.overlay.position();
    const config = new OverlayConfig({
      panelClass: 'tb-timewindow-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: isGtXs,
    });
    if (isGtXs) {
      config.minWidth = '417px';
      config.maxHeight = '440px';
      const panelHeight = 375;
      const panelWidth = 417;
      const el = this.timewindowPanelOrigin.elementRef.nativeElement;
      const offset = el.getBoundingClientRect();
      const scrollTop = this.window.pageYOffset || this.document.documentElement.scrollTop || this.document.body.scrollTop || 0;
      const scrollLeft = this.window.pageXOffset || this.document.documentElement.scrollLeft || this.document.body.scrollLeft || 0;
      const bottomY = offset.bottom - scrollTop;
      const leftX = offset.left - scrollLeft;
      let originX;
      let originY;
      let overlayX;
      let overlayY;
      const wHeight = this.document.documentElement.clientHeight;
      const wWidth = this.document.documentElement.clientWidth;
      if (bottomY + panelHeight > wHeight) {
        originY = 'top';
        overlayY = 'bottom';
      } else {
        originY = 'bottom';
        overlayY = 'top';
      }
      if (leftX + panelWidth > wWidth) {
        originX = 'end';
        overlayX = 'end';
      } else {
        originX = 'start';
        overlayX = 'start';
      }
      const connectedPosition: ConnectedPosition = {
        originX,
        originY,
        overlayX,
        overlayY
      };
      config.positionStrategy = position.flexibleConnectedTo(this.timewindowPanelOrigin.elementRef)
        .withPositions([connectedPosition]);
    } else {
      config.minWidth = '100%';
      config.minHeight = '100%';
      config.positionStrategy = position.global().top('0%').left('0%')
        .right('0%').bottom('0%');
    }

    const overlayRef = this.overlay.create(config);

    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const injector = this._createTimewindowPanelInjector(
      overlayRef,
      {
        timewindow: deepClone(this.innerValue),
        historyOnly: this.historyOnly,
        aggregation: this.aggregation,
        isEdit: this.isEdit
      }
    );

    const componentRef = overlayRef.attach(new ComponentPortal(TimewindowPanelComponent, this.viewContainerRef, injector));
    componentRef.onDestroy(() => {
      if (componentRef.instance.result) {
        this.innerValue = componentRef.instance.result;
        this.timewindowDisabled = this.isTimewindowDisabled();
        this.updateDisplayValue();
        this.notifyChanged();
      }
    });
  }

  private _createTimewindowPanelInjector(overlayRef: OverlayRef, data: TimewindowPanelData): PortalInjector {
    const injectionTokens = new WeakMap<any, any>([
      [TIMEWINDOW_PANEL_DATA, data],
      [OverlayRef, overlayRef]
    ]);
    return new PortalInjector(this.viewContainerRef.injector, injectionTokens);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.timewindowDisabled = this.isTimewindowDisabled();
  }

  writeValue(obj: Timewindow): void {
    this.innerValue = initModelFromDefaultTimewindow(obj, this.timeService);
    this.timewindowDisabled = this.isTimewindowDisabled();
    this.updateDisplayValue();
  }

  notifyChanged() {
    this.propagateChange(cloneSelectedTimewindow(this.innerValue));
  }

  updateDisplayValue() {
    if (this.innerValue.selectedTab === TimewindowType.REALTIME && !this.historyOnly) {
      this.innerValue.displayValue = this.translate.instant('timewindow.realtime') + ' - ' +
        this.translate.instant('timewindow.last-prefix') + ' ' +
        this.millisecondsToTimeStringPipe.transform(this.innerValue.realtime.timewindowMs);
    } else {
      this.innerValue.displayValue = !this.historyOnly ? (this.translate.instant('timewindow.history') + ' - ') : '';
      if (this.innerValue.history.historyType === HistoryWindowType.LAST_INTERVAL) {
        this.innerValue.displayValue += this.translate.instant('timewindow.last-prefix') + ' ' +
          this.millisecondsToTimeStringPipe.transform(this.innerValue.history.timewindowMs);
      } else {
        const startString = this.datePipe.transform(this.innerValue.history.fixedTimewindow.startTimeMs, 'yyyy-MM-dd HH:mm:ss');
        const endString = this.datePipe.transform(this.innerValue.history.fixedTimewindow.endTimeMs, 'yyyy-MM-dd HH:mm:ss');
        this.innerValue.displayValue += this.translate.instant('timewindow.period', {startTime: startString, endTime: endString});
      }
    }
  }

  hideLabel() {
    return this.isToolbar && !this.breakpointObserver.isMatched(MediaBreakpoints['gt-md']);
  }

  private isTimewindowDisabled(): boolean {
    return this.disabled ||
      (!this.isEdit && (!this.innerValue || this.innerValue.hideInterval &&
        (!this.aggregation || this.innerValue.hideAggregation && this.innerValue.hideAggInterval)));
  }

}
