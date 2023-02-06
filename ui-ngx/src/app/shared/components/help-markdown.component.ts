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
  EventEmitter,
  Input, OnChanges,
  OnDestroy, OnInit,
  Output, SimpleChanges
} from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { share } from 'rxjs/operators';
import { HelpService } from '@core/services/help.service';

@Component({
  selector: 'tb-help-markdown',
  templateUrl: './help-markdown.component.html',
  styleUrls: ['./help-markdown.component.scss']
})
export class HelpMarkdownComponent implements OnDestroy, OnInit, OnChanges {

  @Input() helpId: string;

  @Input() helpContent: string;

  @Input() visible: boolean;

  @Input() style: { [klass: string]: any } = {};

  @Output() markdownReady = new EventEmitter<void>();

  markdownText = new BehaviorSubject<string>(null);

  markdownText$ = this.markdownText.pipe(
    share()
  );

  private loadHelpPending = false;

  constructor(private help: HelpService) {}

  ngOnInit(): void {
    this.loadHelpWhenVisible();
  }

  ngOnDestroy(): void {
    this.markdownText.complete();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'visible') {
          if (this.loadHelpPending) {
            this.loadHelpPending = false;
            this.loadHelp();
          }
        }
        if (propName === 'helpId' || propName === 'helpContent') {
          this.markdownText.next(null);
          this.loadHelpWhenVisible();
        }
      }
    }
  }

  private loadHelpWhenVisible() {
    if (this.visible) {
      this.loadHelp();
    } else {
      this.loadHelpPending = true;
    }
  }

  private loadHelp() {
    if (this.helpId) {
      this.help.getHelpContent(this.helpId).subscribe((content) => {
        this.markdownText.next(content);
      });
    } else if (this.helpContent) {
      this.markdownText.next(this.helpContent);
    }
  }

  onMarkdownReady() {
    this.markdownReady.next();
  }

  markdownClick($event: MouseEvent) {
  }

}
