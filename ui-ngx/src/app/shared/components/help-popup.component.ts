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

import {
  Component,
  ElementRef,
  Input, OnChanges,
  OnDestroy,
  Renderer2, SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { TbPopoverService } from '@shared/components/popover.service';
import { PopoverPlacement } from '@shared/components/popover.models';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { isDefinedAndNotNull } from '@core/utils';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: '[tb-help-popup], [tb-help-popup-content]',
  templateUrl: './help-popup.component.html',
  styleUrls: ['./help-popup.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class HelpPopupComponent implements OnChanges, OnDestroy {

  @ViewChild('toggleHelpButton', {read: ElementRef, static: false}) toggleHelpButton: ElementRef;
  @ViewChild('toggleHelpTextButton', {read: ElementRef, static: false}) toggleHelpTextButton: ElementRef;

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('tb-help-popup') helpId: string;

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('tb-help-popup-content') helpContent: string;

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('trigger-text') triggerText: string;

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('trigger-style') triggerStyle: string;

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('tb-help-popup-placement') helpPopupPlacement: PopoverPlacement;

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('tb-help-popup-style') helpPopupStyle: { [klass: string]: any } = {};

  popoverVisible = false;
  popoverReady = true;

  triggerSafeHtml: SafeHtml = null;
  textMode = false;

  constructor(private viewContainerRef: ViewContainerRef,
              private element: ElementRef<HTMLElement>,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private popoverService: TbPopoverService,
              public wl: WhiteLabelingService) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (isDefinedAndNotNull(this.triggerText)) {
      this.triggerSafeHtml = this.sanitizer.bypassSecurityTrustHtml(this.triggerText);
    } else {
      this.triggerSafeHtml = null;
    }
    this.textMode = this.triggerSafeHtml != null;
  }

  toggleHelp() {
    const trigger = this.textMode ? this.toggleHelpTextButton.nativeElement : this.toggleHelpButton.nativeElement;
    this.popoverService.toggleHelpPopover(trigger, this.renderer, this.viewContainerRef,
      this.helpId,
      this.helpContent,
      (visible) => {
        this.popoverVisible = visible;
      }, (ready => {
        this.popoverReady = ready;
      }),
      this.helpPopupPlacement,
      {},
      this.helpPopupStyle);
  }

  ngOnDestroy(): void {
  }

}
