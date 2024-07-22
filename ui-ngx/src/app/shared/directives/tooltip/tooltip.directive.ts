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
  Directive,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
} from '@angular/core';
import { fromEvent, Subject } from 'rxjs';
import { filter, takeUntil, tap } from 'rxjs/operators';
import { MatTooltip, TooltipPosition } from '@angular/material/tooltip';

@Directive({
  standalone: true,
  selector: '[tbTruncateTooltip]',
  providers: [MatTooltip],

})
export class TooltipDirective implements OnInit, AfterViewInit, OnDestroy {
  @Input('tbTruncateTooltip') text: string;
  @Input() tooltipEnabled = true;
  @Input() position: TooltipPosition = 'above';

  private destroy$ = new Subject<void>();

  constructor(
    private elementRef: ElementRef,
    private renderer: Renderer2,
    private tooltip: MatTooltip
  ) {}

  ngOnInit(): void {
    this.observeMouseEvents();
    this.applyTruncationStyles();
  }

  ngAfterViewInit(): void {
    if (!this.text) {
      this.text = this.elementRef.nativeElement.innerText;
    }

    this.tooltip.position = this.position;
  }

  ngOnDestroy(): void {
    if (this.tooltip._isTooltipVisible()) {
      this.hideTooltip();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private observeMouseEvents(): void {
    fromEvent(this.elementRef.nativeElement, 'mouseenter')
      .pipe(
        filter(() => this.tooltipEnabled),
        filter(() => this.isOverflown(this.elementRef.nativeElement)),
        tap(() => this.showTooltip()),
        takeUntil(this.destroy$),
      )
      .subscribe();
    fromEvent(this.elementRef.nativeElement, 'mouseleave')
      .pipe(
        filter(() => this.tooltipEnabled),
        filter(() => this.tooltip._isTooltipVisible()),
        tap(() => this.hideTooltip()),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  private applyTruncationStyles(): void {
    this.renderer.setStyle(this.elementRef.nativeElement, 'white-space', 'nowrap');
    this.renderer.setStyle(this.elementRef.nativeElement, 'overflow', 'hidden');
    this.renderer.setStyle(this.elementRef.nativeElement, 'text-overflow', 'ellipsis');
  }

  private isOverflown(element: HTMLElement): boolean {
    return element.clientWidth < element.scrollWidth;
  }

  private showTooltip(): void {
    this.tooltip.message = this.text;

    this.renderer.setAttribute(this.elementRef.nativeElement, 'matTooltip', this.text);
    this.tooltip.show();
  }

  private hideTooltip(): void {
    this.tooltip.hide();
  }
}
