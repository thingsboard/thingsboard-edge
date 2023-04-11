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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserSettingsService } from '@core/http/user-settings.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetContext } from '@home/models/widget-component.models';
import { MatDialog } from '@angular/material/dialog';
import {
  GettingStartedCompletedDialogComponent
} from '@home/components/widget/lib/home-page/getting-started-completed-dialog.component';
import { GettingStarted } from '@shared/models/user-settings.models';
import { CdkStep, StepperSelectionEvent } from '@angular/cdk/stepper';
import { isUndefined } from '@core/utils';
import { MatStepper } from '@angular/material/stepper';
import { first } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
  selector: 'tb-getting-started-widget',
  templateUrl: './getting-started-widget.component.html',
  styleUrls: ['./getting-started-widget.component.scss']
})
export class GettingStartedWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('matStepper')
  matStepper: MatStepper;

  @Input()
  ctx: WidgetContext;

  authority = Authority;

  authUser = getCurrentAuthUser(this.store);
  gettingStarted: GettingStarted = {
    lastSelectedIndex: 0,
    maxSelectedIndex: 0
  };
  allCompleted = false;
  helpBaseUrl = this.wl.getHelpLinkBaseUrl();

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private userSettingsService: UserSettingsService,
              private wl: WhiteLabelingService,
              private dialog: MatDialog) {
    super(store);
  }

  ngOnInit() {
    this.userSettingsService.getGettingStarted().subscribe(
      (gettingStarted) => {
        if (gettingStarted) {
          this.gettingStarted = gettingStarted;
          if (isUndefined(this.gettingStarted.lastSelectedIndex)) {
            this.gettingStarted.lastSelectedIndex = 0;
          }
          if (isUndefined(this.gettingStarted.maxSelectedIndex)) {
            this.gettingStarted.maxSelectedIndex = 0;
          }
          if (this.gettingStarted.lastSelectedIndex > 0 && this.gettingStarted.lastSelectedIndex < this.matStepper.steps.length) {
            const animationDuration = this.matStepper.animationDuration;
            this.matStepper.animationDuration = '0';
            this.matStepper.selectedIndex = this.gettingStarted.lastSelectedIndex;
            this.matStepper.animationDone.pipe(first()).subscribe(() => {
              setTimeout(() => {this.matStepper.animationDuration = animationDuration;}, 0);
            });
          }
          if (this.matStepper.steps.length) {
            this.updateCompletedSteps();
            this.cd.markForCheck();
          } else {
            this.matStepper.steps.changes.subscribe(
              () => {
                this.updateCompletedSteps();
                this.cd.markForCheck();
              }
            );
          }
        }
      }
    );
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  isSelected(step: CdkStep) {
    return this.matStepper?.selected === step;
  }

  gettingStartedCompleted() {
    this.dialog.open<GettingStartedCompletedDialogComponent, any,
      void>(GettingStartedCompletedDialogComponent, {
      disableClose: true,
      autoFocus: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe();
  }

  updateSelectedIndex(event: StepperSelectionEvent) {
    if (this.gettingStarted.lastSelectedIndex !== event.selectedIndex) {
      this.gettingStarted.lastSelectedIndex = event.selectedIndex;
      if (event.selectedIndex > this.gettingStarted.maxSelectedIndex) {
        this.gettingStarted.maxSelectedIndex = event.selectedIndex;
        this.updateCompletedSteps();
      }
      this.userSettingsService
        .updateGettingStarted(this.gettingStarted, {ignoreLoading: true}).subscribe();
    }
  }

  updateCompletedSteps() {
    if (this.gettingStarted.maxSelectedIndex >= this.matStepper.steps.length-1) {
      this.allCompleted = true;
    }
  }

}
