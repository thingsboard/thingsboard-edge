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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { SecretStorageService } from '@core/http/secret-storage.service';

@Component({
  selector: 'tb-edit-secret-description-panel',
  templateUrl: './edit-secret-description-panel.component.html',
  styleUrls: ['./edit-secret-description-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class EditSecretDescriptionPanelComponent implements OnInit {

  @Input()
  secretId: string;

  @Input()
  description: string;

  @Output()
  descriptionApplied = new EventEmitter<string>();

  descriptionFormControl = this.fb.control<string>(null);

  constructor(private fb: FormBuilder,
              private popover: TbPopoverComponent<EditSecretDescriptionPanelComponent>,
              private secretStorageService: SecretStorageService) {}

  ngOnInit(): void {
    this.descriptionFormControl.setValue(this.description, {emitEvent: false});
  }

  cancel() {
    this.popover.hide();
  }

  applyDescription() {
    const description = this.descriptionFormControl.value;
    this.secretStorageService.updateSecretDescription(this.secretId, description).subscribe(() => {
      this.descriptionApplied.emit(description);
    });
  }

}
