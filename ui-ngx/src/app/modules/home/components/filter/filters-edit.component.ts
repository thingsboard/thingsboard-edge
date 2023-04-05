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

import {
  Component,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  StaticProvider,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { TooltipPosition } from '@angular/material/tooltip';
import { IAliasController } from '@core/api/widget-api.models';
import { CdkOverlayOrigin, ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { BreakpointObserver } from '@angular/cdk/layout';
import { deepClone } from '@core/utils';
import { Filter, FilterInfo, isFilterEditable } from '@shared/models/query/query.models';
import {
  FILTER_EDIT_PANEL_DATA,
  FiltersEditPanelComponent,
  FiltersEditPanelData
} from '@home/components/filter/filters-edit-panel.component';
import { ComponentPortal } from '@angular/cdk/portal';
import { UserFilterDialogComponent, UserFilterDialogData } from '@home/components/filter/user-filter-dialog.component';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'tb-filters-edit',
  templateUrl: './filters-edit.component.html',
  styleUrls: ['./filters-edit.component.scss']
})
export class FiltersEditComponent implements OnInit, OnDestroy {

  aliasControllerValue: IAliasController;

  @Input()
  set aliasController(aliasController: IAliasController) {
    this.aliasControllerValue = aliasController;
    this.setupAliasController(this.aliasControllerValue);
  }

  get aliasController(): IAliasController {
    return this.aliasControllerValue;
  }

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  @Input() disabled: boolean;

  @ViewChild('filtersEditPanelOrigin') filtersEditPanelOrigin: CdkOverlayOrigin;

  displayValue: string;
  filtersInfo: {[filterId: string]: FilterInfo} = {};
  hasEditableFilters = false;

  private rxSubscriptions = new Array<Subscription>();

  constructor(private translate: TranslateService,
              private overlay: Overlay,
              private breakpointObserver: BreakpointObserver,
              private viewContainerRef: ViewContainerRef,
              private dialog: MatDialog) {
  }

  private setupAliasController(aliasController: IAliasController) {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
    if (aliasController) {
      this.rxSubscriptions.push(aliasController.filtersChanged.subscribe(
        () => {
          setTimeout(() => {
            this.updateFiltersInfo();
          }, 0);
        }
      ));
      setTimeout(() => {
        this.updateFiltersInfo();
      }, 0);
    }
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  openEditMode() {
    if (this.disabled || !this.hasEditableFilters) {
      return;
    }
    const filteredArray = Object.entries(this.filtersInfo);

    if (filteredArray.length === 1) {
      const singleFilter: Filter = {id: filteredArray[0][0], ...filteredArray[0][1]};
      this.dialog.open<UserFilterDialogComponent, UserFilterDialogData,
        Filter>(UserFilterDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          filter: singleFilter
        }
      }).afterClosed().subscribe(
        (result) => {
          if (result) {
            this.filtersInfo[result.id] = result;
            this.aliasController.updateUserFilter(result);
          }
        });
    } else {
      const position = this.overlay.position();
      const config = new OverlayConfig({
        panelClass: 'tb-filters-edit-panel',
        backdropClass: 'cdk-overlay-transparent-backdrop',
        hasBackdrop: true,
      });
      const connectedPosition: ConnectedPosition = {
        originX: 'start',
        originY: 'bottom',
        overlayX: 'start',
        overlayY: 'top'
      };
      config.positionStrategy = position.flexibleConnectedTo(this.filtersEditPanelOrigin.elementRef)
        .withPositions([connectedPosition]);
      const overlayRef = this.overlay.create(config);
      overlayRef.backdropClick().subscribe(() => {
        overlayRef.dispose();
      });

      const injector = this._createFiltersEditPanelInjector(
        overlayRef,
        {
          aliasController: this.aliasController,
          filtersInfo: deepClone(this.filtersInfo)
        }
      );
      overlayRef.attach(new ComponentPortal(FiltersEditPanelComponent, this.viewContainerRef, injector));
    }
  }

  private _createFiltersEditPanelInjector(overlayRef: OverlayRef, data: FiltersEditPanelData): Injector {
    const providers: StaticProvider[] = [
      {provide: FILTER_EDIT_PANEL_DATA, useValue: data},
      {provide: OverlayRef, useValue: overlayRef}
    ];
    return Injector.create({parent: this.viewContainerRef.injector, providers});
  }

  private updateFiltersInfo() {
    const allFilters = this.aliasController.getFilters();
    this.filtersInfo = {};
    this.hasEditableFilters = false;
    for (const filterId of Object.keys(allFilters)) {
      const filterInfo = this.aliasController.getFilterInfo(filterId);
      if (filterInfo && isFilterEditable(filterInfo)) {
        this.filtersInfo[filterId] = deepClone(filterInfo);
        this.hasEditableFilters = true;
      }
    }
  }

}
