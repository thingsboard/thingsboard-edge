///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { RaphaelElement, RaphaelPaper, RaphaelSet } from 'raphael';
import tinycolor from 'tinycolor2';

interface CircleElement extends RaphaelElement {
  theGlow?: RaphaelSet;
}

@Component({
  selector: 'tb-led-light',
  templateUrl: './led-light.component.html',
  styleUrls: []
})
export class LedLightComponent implements OnInit, AfterViewInit, OnChanges {

  @Input() size: number;

  @Input() colorOn: string;

  @Input() colorOff: string;

  @Input() offOpacity: number;

  private enabledValue: boolean;
  get enabled(): boolean {
    return this.enabledValue;
  }
  @Input()
  set enabled(value: boolean) {
    this.enabledValue = coerceBooleanProperty(value);
  }

  private canvasSize: number;
  private radius: number;
  private glowSize: number;
  private glowColor: string;

  private paper: RaphaelPaper;
  private circleElement: CircleElement;

  constructor(private elementRef: ElementRef<HTMLElement>) {
  }

  ngOnInit(): void {
    this.offOpacity = this.offOpacity || 0.4;
    this.glowColor = tinycolor(this.colorOn).lighten().toHexString();
  }

  ngAfterViewInit(): void {
    this.update();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'enabled' && this.circleElement) {
          this.draw();
        } else if (propName === 'size') {
          this.update();
        }
      }
    }
  }

  private update() {
    this.size = this.size || 50;
    this.canvasSize = this.size;
    this.radius = this.canvasSize / 4;
    this.glowSize = this.radius / 5;
    if (this.paper) {
      this.paper.remove();
    }
    import('raphael').then(
      (raphael) => {
        this.paper = raphael.default($('#canvas_container', this.elementRef.nativeElement)[0], this.canvasSize, this.canvasSize);
        const center = this.canvasSize / 2;
        this.circleElement = this.paper.circle(center, center, this.radius);
        this.draw();
      }
    );
  }

  private draw() {
    if (this.enabled) {
      this.circleElement.attr('fill', this.colorOn);
      this.circleElement.attr('stroke', this.colorOn);
      this.circleElement.attr('opacity', 1);
      if (this.circleElement.theGlow) {
        this.circleElement.theGlow.remove();
      }
      this.circleElement.theGlow = this.circleElement.glow(
        {
          color: this.glowColor,
          width: this.radius + this.glowSize,
          opacity: 0.8,
          fill: true
        });
    } else {
      if (this.circleElement.theGlow) {
        this.circleElement.theGlow.remove();
      }
      this.circleElement.attr('fill', this.colorOff);
      this.circleElement.attr('stroke', this.colorOff);
      this.circleElement.attr('opacity', this.offOpacity);
    }
  }

}
