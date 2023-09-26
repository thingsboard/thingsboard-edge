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

import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { TenantSolutionTemplateInfo } from '@shared/models/solution-template.models';
import { Router } from '@angular/router';
import { SolutionsService } from '@core/http/solutions.service';
import { MatDialog } from '@angular/material/dialog';
import {
  SolutionInstallDialogComponent,
  SolutionInstallDialogData
} from '@home/components/solution/solution-install-dialog.component';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { ReplaySubject, Subject } from 'rxjs';

@Component({
  selector: 'tb-solution-template-card',
  templateUrl: './solution-template-card.component.html',
  styleUrls: ['./solution-template-card.component.scss']
})
export class SolutionTemplateCardComponent extends PageComponent implements OnInit {

  @Input()
  solutionTemplate: TenantSolutionTemplateInfo;

  @Output()
  solutionTemplateChanged = new EventEmitter();

  constructor(protected store: Store<AppState>,
              private solutionsService: SolutionsService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private translate: TranslateService,
              private router: Router) {
    super(store);
  }

  ngOnInit() {
  }

  openSolutionTemplateDetails() {
    this.router.navigateByUrl(`solutionTemplates/${this.solutionTemplate.id}`);
  }

  installSolutionTemplate() {
    const progressSubject = new Subject<void>();
    this.dialogService.progress(progressSubject.asObservable(), this.translate.instant('solution-template.installing'));
    this.solutionsService.installSolutionTemplate(this.solutionTemplate.id).subscribe(
      (response) => {
        if (response.success) {
          const url = this.router.createUrlTree(['dashboards', 'groups', response.dashboardGroupId.id, response.dashboardId.id],
            {
              queryParams: {
                solutionTemplateId: this.solutionTemplate.id
              }
            }
          );
          setTimeout(() => {
            progressSubject.next();
            progressSubject.complete();
            this.router.navigateByUrl(url);
          }, this.solutionTemplate.installTimeoutMs);
        } else {
          progressSubject.next();
          progressSubject.complete();
          this.dialog.open<SolutionInstallDialogComponent, SolutionInstallDialogData>(SolutionInstallDialogComponent, {
            disableClose: true,
            panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
            data: {
              solutionInstallResponse: response,
              instructions: false,
              showMainDashboardButton: false
            }
          }).afterClosed().subscribe(
            () => {
              this.solutionTemplateChanged.emit();
            }
          );
        }
      },
      () => {
        progressSubject.next();
        progressSubject.complete();
      }
    );
  }

  openInstructions() {
    this.solutionsService.getSolutionTemplateInstructions(this.solutionTemplate.id).subscribe(
      (solutionTemplateInstructions) => {
        this.dialog.open<SolutionInstallDialogComponent, SolutionInstallDialogData>(SolutionInstallDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            solutionInstallResponse: solutionTemplateInstructions,
            instructions: true,
            showMainDashboardButton: true
          }
        });
      }
    );
  }

  deleteSolutionTemplate() {
    this.dialogService.confirm(
      this.translate.instant('solution-template.delete-solution-title', {solutionTitle: this.solutionTemplate.title}),
      this.translate.instant('solution-template.delete-solution-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.solutionsService.deleteSolutionTemplate(this.solutionTemplate.id).subscribe(
          () => {
            this.solutionTemplateChanged.emit();
          }
        );
      }
    });
  }

}
