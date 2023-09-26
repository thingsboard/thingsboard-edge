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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetContext } from '@home/models/widget-component.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { SolutionTemplateLevel, TenantSolutionTemplateInfo } from '@shared/models/solution-template.models';
import { SolutionsService } from '@core/http/solutions.service';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { Subscription } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-solution-templates-widget',
  templateUrl: './solution-templates-widget.component.html',
  styleUrls: ['./home-page-widget.scss', './solution-templates-widget.component.scss']
})
export class SolutionTemplatesWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  solutionTemplateLevel = SolutionTemplateLevel;

  authUser = getCurrentAuthUser(this.store);

  solutionTemplates: Array<TenantSolutionTemplateInfo>;

  items = 2;

  hasSolutionTemplatesAccess = true;

  selectedIndex = 0;

  private observeBreakpointSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              private solutionService: SolutionsService,
              private breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {
    if (this.breakpointObserver.isMatched(MediaBreakpoints.xs)) {
      this.items = 2;
    } else if (this.breakpointObserver.isMatched(MediaBreakpoints.sm)) {
      this.items = 4;
    } else if (this.breakpointObserver.isMatched(MediaBreakpoints.md)) {
      this.items = 3;
    } else if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-md'])) {
      this.items = 5;
    }
    this.observeBreakpointSubscription = this.breakpointObserver
      .observe([MediaBreakpoints.xs, MediaBreakpoints.sm, MediaBreakpoints.md, MediaBreakpoints['gt-md']])
      .subscribe((state: BreakpointState) => {
          if (state.breakpoints[MediaBreakpoints.xs]) {
            this.items = 2;
          } else if (state.breakpoints[MediaBreakpoints.sm]) {
            this.items = 4;
          } else if (state.breakpoints[MediaBreakpoints.md]) {
            this.items = 3;
          } else if (state.breakpoints[MediaBreakpoints['gt-md']]) {
            this.items = 5;
          }
          this.cd.markForCheck();
        }
      );

    this.hasSolutionTemplatesAccess = this.authUser.authority === Authority.TENANT_ADMIN &&
      this.userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL);
   if (this.hasSolutionTemplatesAccess) {
      this.reload();
    }
  }

  ngOnDestroy() {
    if (this.observeBreakpointSubscription) {
      this.observeBreakpointSubscription.unsubscribe();
    }
    super.ngOnDestroy();
  }

  reload() {
    this.solutionService.getSolutionTemplateInfos().subscribe(
      (solutionTemplates) => {
        this.solutionTemplates = solutionTemplates;
        this.cd.markForCheck();
      }
    );
  }

  planText(level: SolutionTemplateLevel): string {
    if (level === SolutionTemplateLevel.PROTOTYPE) {
      return this.translate.instant('widgets.solution-templates.prototype-plan');
    } else if (level === SolutionTemplateLevel.STARTUP) {
      return this.translate.instant('widgets.solution-templates.startup-plan');
    } else {
      return '';
    }
  }
}
