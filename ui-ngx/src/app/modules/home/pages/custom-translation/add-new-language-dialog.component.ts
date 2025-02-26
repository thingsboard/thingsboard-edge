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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, Validators } from '@angular/forms';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export interface AddNewLanguageDialogData {
  langs: string[];
}

@Component({
  selector: 'tb-add-new-language-dialog',
  templateUrl: './add-new-language-dialog.component.html',
  styleUrls: ['./add-new-language-dialog.component.scss']
})
export class AddNewLanguageDialogComponent extends
  DialogComponent<AddNewLanguageDialogComponent> implements OnInit, OnDestroy{

  languageForm = this.fb.group({
    language: ['', {nonNullable: true, validators: Validators.required}],
    upload: [false],
    translation: [{value: null, disabled: true}, {nonNullable: true, validators: Validators.required}]
  });

  langs: string[];

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<AddNewLanguageDialogComponent>,
              @Inject(MAT_DIALOG_DATA) private data: AddNewLanguageDialogData,
              private fb: FormBuilder,
              private customTranslationService: CustomTranslationService) {
    super(store, router, dialogRef);
    this.langs = this.data.langs;
  }


  ngOnInit() {
    this.languageForm.get('upload').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.languageForm.get('translation').enable({emitEvent: false});
      } else {
        this.languageForm.get('translation').disable({emitEvent: false});
      }
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  addLanguage($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const formValue = this.languageForm.value;
    this.customTranslationService.saveCustomTranslation(
      formValue.language, formValue.upload ? formValue.translation : {}
    ).subscribe(() => {
      this.dialogRef.close(true);
    });
  }

}
