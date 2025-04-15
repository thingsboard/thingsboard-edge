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
  DestroyRef,
  ElementRef,
  EventEmitter,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import { TripTimelineSettings } from '@shared/models/widget/maps/map.models';
import { DateFormatProcessor } from '@shared/models/widget-settings.models';
import { interval, Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-map-timeline-panel',
  templateUrl: './map-timeline-panel.component.html',
  styleUrls: ['./map-timeline-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MapTimelinePanelComponent implements OnInit, OnDestroy {

  @Input()
  settings: TripTimelineSettings;

  @Input()
  disabled = false;

  @Input()
  set min(value: number) {
    if (this.minValue !== value) {
      this.minValue = value;
      this.maxTimeIndex = Math.ceil((this.maxValue - this.minValue) / this.settings.timeStep);
      this.cd.markForCheck();
    }
  }

  get min(): number {
    return this.minValue;
  }

  @Input()
  set max(value: number) {
    if (this.maxValue !== value) {
      this.maxValue = value;
      this.maxTimeIndex = Math.ceil((this.maxValue - this.minValue) / this.settings.timeStep);
      this.cd.markForCheck();
    }
  }

  get max(): number {
    return this.maxValue;
  }

  @Input()
  set currentTime(time: number) {
    if (this.currentTimeValue !== time) {
      this.currentTimeValue = time;
      if (this.hasData) {
        this.index = Math.ceil((this.currentTimeValue - this.minValue) / this.settings.timeStep);
      } else {
        this.index = 0;
      }
      this.updateTimestampDisplayValue();
      this.cd.markForCheck();
    }
  }

  get currentTime(): number {
    return this.currentTimeValue;
  }

  get hasData(): boolean {
    return !!this.currentTimeValue && this.currentTimeValue !== Infinity;
  }

  set panelElement(element: Element) {
    this.panelElementVal = element;
    this.panelResize$ = new ResizeObserver(() => {
      this.resize();
    });
    this.panelResize$.observe(element);
  }

  get panelElement(): Element {
    return this.panelElementVal;
  }

  @Input()
  anchors: number[] = [];

  @Output()
  timeChanged = new EventEmitter<number>();

  column = false;

  timestampFormat: DateFormatProcessor;

  minTimeIndex = 0;
  maxTimeIndex = 0;
  index = 0;
  playing = false;
  interval: Subscription;
  speed: number;

  private minValue: number;
  private maxValue: number;
  private currentTimeValue: number = null;

  private panelResize$: ResizeObserver;
  private panelElementVal: Element;

  constructor(public element: ElementRef<HTMLElement>,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef,
              private injector: Injector) {
  }

  ngOnInit() {
    if (this.settings.showTimestamp) {
      this.timestampFormat = DateFormatProcessor.fromSettings(this.injector, this.settings.timestampFormat);
      this.timestampFormat.update(this.currentTime);
    }
    this.speed = this.settings.speedOptions[0];
  }

  ngOnDestroy() {
    if (this.panelResize$) {
      this.panelResize$.disconnect();
    }
  }

  public onIndexChange(index: number) {
    this.index = index;
    this.updateCurrentTime();
  }

  public play() {
    this.playing = true;
    if (!this.interval) {
      this.interval = interval(1000 / this.speed)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        filter(() => this.playing)
      ).subscribe(
        {
          next: () => {
            if (this.index < this.maxTimeIndex) {
              this.index++;
              this.updateCurrentTime();
            } else {
              this.playing = false;
              this.cd.markForCheck();
              this.interval.unsubscribe();
              this.interval = null;
            }
          },
          error: (err) => {
            console.error(err);
          }
        }
      );
    }
  }

  public pause() {
    this.playing = false;
    this.updateCurrentTime();
  }

  public fastRewind() {
    this.index = this.minTimeIndex;
    this.pause();
  }

  public fastForward() {
    this.index = this.maxTimeIndex;
    this.pause();
  }

  public moveNext() {
    if (this.index < this.maxTimeIndex) {
      if (this.settings.snapToRealLocation) {
        let anchorIndex = this.findIndex(this.currentTime, this.anchors) + 1;
        if (anchorIndex >= this.anchors.length) {
          anchorIndex = this.anchors.length - 1;
        }
        this.index = Math.floor((this.anchors[anchorIndex] - this.minValue) / this.settings.timeStep);
      } else {
        this.index++;
      }
    }
    this.pause();
  }

  public movePrev() {
    if (this.index > this.minTimeIndex) {
      if (this.settings.snapToRealLocation) {
        let anchorIndex = this.findIndex(this.currentTime, this.anchors) - 1;
        if (anchorIndex < 0) {
          anchorIndex = 0;
        }
        this.index = Math.floor((this.anchors[anchorIndex] - this.minValue) / this.settings.timeStep);
      } else {
        this.index--;
      }
    }
    this.pause();
  }

  public speedUpdated() {
    if (this.interval) {
      this.interval.unsubscribe();
      this.interval = null;
    }
    if (this.playing) {
      this.play();
    }
  }

  private resize(): void {
    const width = this.panelElement.getBoundingClientRect().width;
    const column = width <= 400;
    if (this.column !== column) {
      this.column = column;
      this.cd.markForCheck();
    }
  }

  private updateCurrentTime() {
    const newTime = this.minValue + this.index * this.settings.timeStep;
    if (this.currentTime !== newTime) {
      this.currentTime = newTime;
      this.timeChanged.emit(this.currentTime);
      this.updateTimestampDisplayValue();
    }
  }

  private updateTimestampDisplayValue() {
    if (this.settings.showTimestamp && this.hasData) {
      this.timestampFormat.update(this.currentTime);
      this.cd.markForCheck();
    }
  }

  private findIndex(value: number, array: number[]): number {
    let i = 0;
    while (array[i] < value) {
      i++;
    }
    return i;
  }

}
