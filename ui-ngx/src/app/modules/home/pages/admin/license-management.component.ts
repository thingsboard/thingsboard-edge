// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { createManageSubscriptionUrl, PlanUiType, SubscriptionInfo } from '@shared/models/subscription.models';
import { ActivatedRoute } from '@angular/router';
import { AdminService } from '@core/http/admin.service';
import { MatDialog } from '@angular/material/dialog';
import {
  AddLicenseItemDialogComponent,
  AddLicenseItemDialogData
} from '@home/pages/admin/add-license-item-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { ActionUpdateLicenseParams } from '@core/auth/auth.actions';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

@Component({
    selector: 'tb-license-management',
    templateUrl: './license-management.component.html',
    styleUrls: ['./license-management.component.scss', './settings-card.scss'],
    standalone: false
})
export class LicenseManagementComponent extends PageComponent implements OnInit {

  get planWithoutWhiteLabeling(): boolean {
    return this.subscriptionInfo?.planUiType === PlanUiType.TbMaker || this.subscriptionInfo?.planUiType === PlanUiType.TbPrototype
  }

  subscriptionInfo: SubscriptionInfo;
  aiEnabled = getCurrentAuthState(this.store).aiEnabled;

  constructor(private route: ActivatedRoute,
              private adminService: AdminService,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super();
  }

  ngOnInit() {
    this.subscriptionInfo = this.route.snapshot.data.subscriptionInfo;
  }

  manageSubscription($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    window.open(createManageSubscriptionUrl(this.subscriptionInfo), '_blank');
  }

  refreshLicenseInfo($event?: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.adminService.refreshLicense().subscribe(subscriptionInfo => {
      this.subscriptionInfo = subscriptionInfo;
      this.store.dispatch(new ActionUpdateLicenseParams(
        {
          edgeEnabled: subscriptionInfo.edgeEnabled,
          trendzEnabled: subscriptionInfo.trendzEnabled,
          communityGrantLicense: false
        }
      ));
    });
  }

  addDevices($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.addLicenseItem(this.translate.instant('subscription.devices'), true, { extraDeviceCount: 100 });
  }

  addEdges($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.addLicenseItem(this.translate.instant('subscription.edges'), true, { extraEdgeCount: 1 });
  }

  addAiCredits($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.addLicenseItem(this.translate.instant('subscription.ai-credits'), true, { extraAiCreditsCount: 5 });
  }

  choosePilotPlan($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = `${this.subscriptionInfo.licenseServerEndpoint}/?changeSubscriptionPlan=true&subscriptionId=${this.subscriptionInfo.subscriptionId}&planUiType=${PlanUiType.TbPilot}`;
    window.open(url, '_blank');
  }

  addEdge($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.addLicenseItem(this.translate.instant('subscription.edge'), false, { edgeEnabled: true });
  }

  addTrendz($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.addLicenseItem(this.translate.instant('subscription.trendz'), false, { trendzEnabled: true });
  }

  isItemCritical(value: number, max: number): boolean {
    if (max && value) {
      return (value / max) >= 0.85;
    } else {
      return false;
    }
  }

  private addLicenseItem(itemName: string, add: boolean, items: any) {
    this.dialog.open<AddLicenseItemDialogComponent, AddLicenseItemDialogData, boolean>(AddLicenseItemDialogComponent,
      {
        disableClose: true,
        autoFocus: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          itemName,
          add,
          isPerpetual: this.subscriptionInfo.perpetual,
          licensePortalUrl: createManageSubscriptionUrl(this.subscriptionInfo, items)
        }
      }).afterClosed().subscribe(
        () => this.refreshLicenseInfo()
      );
  }
}
