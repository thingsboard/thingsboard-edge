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

import { ComponentRef, Directive, ElementRef, Input, ViewContainerRef } from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: '[tb-circular-progress]'
})
export class CircularProgressDirective {

  showProgressValue = false;

  children: JQuery<any>;

  cssWidth: any;

  @Input('tb-circular-progress')
  set showProgress(showProgress: boolean) {
    if (this.showProgressValue !== showProgress) {
      const element = this.elementRef.nativeElement;
      this.showProgressValue = showProgress;
      this.spinnerRef.instance._elementRef.nativeElement.style.display = showProgress ? 'block' : 'none';
      if (showProgress) {
        this.cssWidth = $(element).prop('style').width;
        if (!this.cssWidth) {
          $(element).css('width', '');
          const width = $(element).prop('offsetWidth');
          $(element).css('width', width + 'px');
        }
        this.children = $(element).children();
        $(element).empty();
        $(element).append($(this.spinnerRef.instance._elementRef.nativeElement));
      } else {
        $(element).empty();
        $(element).append(this.children);
        if (this.cssWidth) {
          $(element).css('width', this.cssWidth);
        } else {
          $(element).css('width', '');
        }
      }
    }
  }

  spinnerRef: ComponentRef<MatProgressSpinner>;

  constructor(private elementRef: ElementRef,
              private viewContainerRef: ViewContainerRef) {
    this.createCircularProgress();
  }

  createCircularProgress() {
    this.elementRef.nativeElement.style.position = 'relative';
    this.spinnerRef = this.viewContainerRef.createComponent(MatProgressSpinner, {index: 0});
    this.spinnerRef.instance.mode = 'indeterminate';
    this.spinnerRef.instance.diameter = 20;
    const el = this.spinnerRef.instance._elementRef.nativeElement;
    el.style.margin = 'auto';
    el.style.position = 'absolute';
    el.style.left = '0';
    el.style.right = '0';
    el.style.top = '0';
    el.style.bottom = '0';
    el.style.display = 'none';
  }
}
