///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
  selector: 'tb-email-verification',
  templateUrl: './email-verification.component.html',
  styleUrls: ['./email-verification.component.scss']
})
export class EmailVerificationComponent extends PageComponent implements OnInit, OnDestroy {

  email = '';
  sub: Subscription;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private router: Router,
              public wl: WhiteLabelingService,
              private authService: AuthService) {
    super(store);
  }

  ngOnInit() {
    this.sub = this.route
      .queryParams
      .subscribe(params => {
        this.email = params.email || '';
      });
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.sub.unsubscribe();
  }

  resendEmail(): void {
    this.authService.resendEmailActivation(this.email).subscribe(
      () => {
        this.router.navigateByUrl('/signup/emailVerification?email=' + this.email);
      }
    );
  }
}
