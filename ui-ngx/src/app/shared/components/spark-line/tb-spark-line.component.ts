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

import { AfterViewInit, Component, ElementRef, Input } from '@angular/core';

@Component({
  selector: 'tb-spark-line',
  template: '',
  styleUrls: []
})
export class TbSparkLineComponent implements AfterViewInit {

  private viewInit = false;
  private libraryLoad = false;
  private scheduleUpdateLine = false;

  private chartDataValue: number[];
  @Input()
  set chartData(value: number[]) {
    this.chartDataValue = value;
    this.updatedChartData();
  }

  get chartData(): number[] {
    return this.chartDataValue;
  }

  @Input()
  chartProperty: object;

  constructor(private elementRef: ElementRef) {
    import('jquery-sparkline/jquery.sparkline.js').then(() => {
      this.libraryLoad = true;
      if (this.viewInit && this.scheduleUpdateLine) {
        this.updatedChartData();
      }
    });
  }

  ngAfterViewInit(): void {
    this.viewInit = true;
    if (this.libraryLoad && this.scheduleUpdateLine) {
      this.updatedChartData();
    }
  }

  private updatedChartData() {
    if (this.viewInit && this.libraryLoad) {
      ($(this.elementRef.nativeElement) as any).sparkline(this.chartData, this.chartProperty);
    } else {
      this.scheduleUpdateLine = true;
    }
  }
}
