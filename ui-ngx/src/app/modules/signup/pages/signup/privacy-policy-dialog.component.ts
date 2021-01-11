///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { SelfRegistrationService } from '@core/http/self-register.service';

@Component({
  selector: 'tb-privacy-policy-dialog',
  templateUrl: './privacy-policy-dialog.component.html',
  styleUrls: []
})
export class PrivacyPolicyDialogComponent extends DialogComponent<PrivacyPolicyDialogComponent, boolean> implements OnInit {

  privacyPolicyText: SafeHtml;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private selfRegistrationService: SelfRegistrationService,
              private domSanitizer: DomSanitizer,
              public dialogRef: MatDialogRef<PrivacyPolicyDialogComponent, boolean>) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.selfRegistrationService.loadPrivacyPolicy().subscribe((privacyPolicy) => {
      this.privacyPolicyText = this.domSanitizer.bypassSecurityTrustHtml(privacyPolicy);
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  accept(): void {
    this.dialogRef.close(true);
  }
}
