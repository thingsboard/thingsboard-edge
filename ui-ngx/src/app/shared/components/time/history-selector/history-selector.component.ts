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

import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { interval } from 'rxjs';
import { filter } from 'rxjs/operators';
import { HistorySelectSettings } from '@app/modules/home/components/widget/lib/maps/map-models';

@Component({
  selector: 'tb-history-selector',
  templateUrl: './history-selector.component.html',
  styleUrls: ['./history-selector.component.scss']
})
export class HistorySelectorComponent implements OnChanges {

  @Input() settings: HistorySelectSettings;
  @Input() minTime: number;
  @Input() maxTime: number;
  @Input() step = 1000;
  @Input() anchors = [];
  @Input() useAnchors = false;

  @Output() timeUpdated: EventEmitter<number> = new EventEmitter();

  minTimeIndex = 0;
  maxTimeIndex = 0;
  speed = 1;
  index = 0;
  playing = false;
  interval;
  speeds = [1, 5, 10, 25];
  currentTime = null;


  constructor(private cd: ChangeDetectorRef) { }

  ngOnChanges() {
    this.maxTimeIndex =  Math.ceil((this.maxTime - this.minTime) / this.step);
    this.currentTime = this.minTime === Infinity ? null : this.minTime;
  }

  play() {
    this.playing = true;
    if (!this.interval) {
      this.interval = interval(1000 / this.speed)
        .pipe(
          filter(() => this.playing)
        ).subscribe(() => {
          this.index++;
          this.currentTime = this.minTime + this.index * this.step;
          if (this.index <= this.maxTimeIndex) {
            this.cd.detectChanges();
            this.timeUpdated.emit(this.currentTime);
          } else {
            this.playing = false;
            this.interval.complete();
            this.cd.detectChanges();
          }
        }, err => {
          console.error(err);
        }, () => {
          this.interval = null;
        });
    }
  }

  reInit() {
    if (this.interval) {
      this.interval.complete();
    }
    if (this.playing) {
      this.play();
    }
  }

  pause() {
    this.playing = false;
    this.currentTime = this.minTime + this.index * this.step;
    this.cd.detectChanges();
    this.timeUpdated.emit(this.currentTime);
  }

  moveNext() {
    if (this.index < this.maxTimeIndex) {
      if (this.useAnchors) {
        const anchorIndex = this.findIndex(this.currentTime, this.anchors) + 1;
        this.index = Math.floor((this.anchors[anchorIndex] - this.minTime) / this.step);
      } else {
        this.index++;
      }
    }
    this.pause();
  }

  movePrev() {
    if (this.index > this.minTimeIndex) {
      if (this.useAnchors) {
        const anchorIndex = this.findIndex(this.currentTime, this.anchors) - 1;
        this.index = Math.floor((this.anchors[anchorIndex] - this.minTime) / this.step);
      } else {
        this.index--;
      }
    }
    this.pause();
  }

  findIndex(value: number, array: number[]): number {
    let i = 0;
    while (array[i] < value) {
      i++;
    }
    return i;
  }

  moveStart() {
    this.index = this.minTimeIndex;
    this.pause();
  }

  moveEnd() {
    this.index = this.maxTimeIndex;
    this.pause();
  }

  changeIndex(index: number) {
    this.index = index;
    this.currentTime = this.minTime + index * this.step;
    this.timeUpdated.emit(this.currentTime);
  }
}
