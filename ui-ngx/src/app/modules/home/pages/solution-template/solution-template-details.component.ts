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

import { AfterViewInit, Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { ActivatedRoute, Router } from '@angular/router';
import { TenantSolutionTemplateDetails } from '@shared/models/solution-template.models';
import { SolutionsService } from '@core/http/solutions.service';
import {
  SolutionInstallDialogComponent,
  SolutionInstallDialogData
} from '@home/components/solution/solution-install-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-solution-template-details',
  templateUrl: './solution-template-details.component.html',
  styleUrls: ['./solution-template-details.component.scss']
})
export class SolutionTemplateDetailsComponent extends PageComponent implements OnInit {

  imageCarouselIndex = 0;

  solutionTemplateDetails: TenantSolutionTemplateDetails = this.route.snapshot.data.solutionTemplateDetails;

  images = this.solutionTemplateDetails.imageUrls.map((url) => {
    return {
      url
    }
  });

  constructor(protected store: Store<AppState>,
              private solutionsService: SolutionsService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private translate: TranslateService,
              private solutionService: SolutionsService,
              private route: ActivatedRoute,
              private router: Router) {
    super(store);
  }

  ngOnInit() {
  }

  installSolutionTemplate() {
    const progressSubject = new Subject();
    this.dialogService.progress(progressSubject.asObservable(), this.translate.instant('solution-template.installing'));
    this.solutionsService.installSolutionTemplate(this.solutionTemplateDetails.id).subscribe(
      (response) => {
        if (response.success) {
          const url = this.router.createUrlTree(['dashboardGroups', response.dashboardGroupId.id, response.dashboardId.id],
            {
              queryParams: {
                solutionTemplateId: this.solutionTemplateDetails.id
              }
            }
          );
          setTimeout(() => {
            progressSubject.next();
            progressSubject.complete();
            this.router.navigateByUrl(url);
          }, this.solutionTemplateDetails.installTimeoutMs);
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
              this.reload();
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

  deleteSolutionTemplate() {
    this.dialogService.confirm(
      this.translate.instant('solution-template.delete-solution-title', {solutionTitle: this.solutionTemplateDetails.title}),
      this.translate.instant('solution-template.delete-solution-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.solutionsService.deleteSolutionTemplate(this.solutionTemplateDetails.id).subscribe(
          () => {
            this.reload();
          }
        );
      }
    });
  }

  openInstructions() {
    this.solutionsService.getSolutionTemplateInstructions(this.solutionTemplateDetails.id).subscribe(
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

  private reload() {
    this.solutionService.getSolutionTemplateDetails(this.solutionTemplateDetails.id).subscribe(
      (solutionTemplateDetails) => {
        this.solutionTemplateDetails = solutionTemplateDetails;
      }
    );
  }
}
