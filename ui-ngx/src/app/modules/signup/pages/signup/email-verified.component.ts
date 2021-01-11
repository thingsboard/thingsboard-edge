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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { LoginResponse } from '@shared/models/login.models';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
  selector: 'tb-email-verified',
  templateUrl: './email-verified.component.html',
  styleUrls: ['./email-verified.component.scss']
})
export class EmailVerifiedComponent extends PageComponent implements OnInit, OnDestroy {

  emailCode = '';
  sub: Subscription;
  loginResponse: LoginResponse;
  activated: Subject<boolean> = new BehaviorSubject<boolean>(false);

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
        this.emailCode = params.emailCode || '';
        this.activateAndGetCredentials();
      });
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.sub.unsubscribe();
  }

  activateAndGetCredentials(): void {
    this.authService.activateByEmailCode(this.emailCode).subscribe(
      (loginResponse) => {
        this.loginResponse = loginResponse;
        this.activated.next(true);
      }
    );
  }

  login(): void {
    this.authService.setUserFromJwtToken(this.loginResponse.token, this.loginResponse.refreshToken, true);
  }

}
