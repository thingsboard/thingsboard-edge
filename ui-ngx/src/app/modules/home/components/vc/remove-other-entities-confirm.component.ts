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

import { Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'tb-remove-other-entities-confirm',
  templateUrl: './remove-other-entities-confirm.component.html',
  styleUrls: []
})
export class RemoveOtherEntitiesConfirmComponent extends PageComponent implements OnInit {

  @Input()
  onClose: (result: boolean | null) => void;

  confirmFormGroup: UntypedFormGroup;

  removeOtherEntitiesConfirmText: SafeHtml;

  removeOtherEntitiesVerificationText = 'remove other entities';

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private fb: UntypedFormBuilder) {
    super(store);
    this.removeOtherEntitiesConfirmText = this.sanitizer.bypassSecurityTrustHtml(this.translate.instant('version-control.remove-other-entities-confirm-text'));
  }

  ngOnInit(): void {
    this.confirmFormGroup = this.fb.group({
      verification: [null, []]
    });
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(null);
    }
  }

  confirm(): void {
    if (this.onClose) {
      this.onClose(true);
    }
  }
}
