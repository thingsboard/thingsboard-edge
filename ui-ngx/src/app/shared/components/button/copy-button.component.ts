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

import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { ClipboardService } from 'ngx-clipboard';
import { TooltipPosition } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';
import { ThemePalette } from '@angular/material/core';

@Component({
  selector: 'tb-copy-button',
  styleUrls: ['copy-button.component.scss'],
  templateUrl: './copy-button.component.html'
})
export class CopyButtonComponent {

  private timer;

  copied = false;

  @Input()
  copyText: string;

  @Input()
  disabled = false;

  @Input()
  mdiIcon: string;

  @Input()
  icon: string;

  @Input()
  tooltipText: string;

  @Input()
  tooltipPosition: TooltipPosition;

  @Input()
  style: {[key: string]: any} = {};

  @Input()
  color: ThemePalette;

  @Output()
  successCopied = new EventEmitter<string>();

  constructor(private clipboardService: ClipboardService,
              private translate: TranslateService,
              private cd: ChangeDetectorRef) {
  }

  copy($event: Event): void {
    $event.stopPropagation();
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.clipboardService.copy(this.copyText);
    this.successCopied.emit(this.copyText);
    this.copied = true;
    this.timer = setTimeout(() => {
      this.copied = false;
      this.cd.detectChanges();
    }, 1500);
  }

  get matTooltipText(): string {
    return this.copied ? this.translate.instant('ota-update.copied') : this.tooltipText;
  }

  get matTooltipPosition(): TooltipPosition {
    return this.copied ? 'below' : this.tooltipPosition;
  }

}
