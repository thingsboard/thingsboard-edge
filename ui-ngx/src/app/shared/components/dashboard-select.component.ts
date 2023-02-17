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
  forwardRef,
  Inject,
  Injector,
  Input,
  OnInit,
  StaticProvider,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { map, share } from 'rxjs/operators';
import { PageData } from '@shared/models/page/page-data';
import { DashboardInfo } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TooltipPosition } from '@angular/material/tooltip';
import { CdkOverlayOrigin, ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { BreakpointObserver } from '@angular/cdk/layout';
import { DOCUMENT } from '@angular/common';
import { WINDOW } from '@core/services/window.service';
import { ComponentPortal } from '@angular/cdk/portal';
import {
  DASHBOARD_SELECT_PANEL_DATA,
  DashboardSelectPanelComponent,
  DashboardSelectPanelData
} from './dashboard-select-panel.component';
import { Operation } from '@shared/models/security.models';
import { UtilsService } from '@core/services/utils.service';

// @dynamic
@Component({
  selector: 'tb-dashboard-select',
  templateUrl: './dashboard-select.component.html',
  styleUrls: ['./dashboard-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DashboardSelectComponent),
    multi: true
  }]
})
export class DashboardSelectComponent implements ControlValueAccessor, OnInit {

  @Input()
  groupId: string;

  @Input()
  operation: Operation;

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  dashboards$: Observable<Array<DashboardInfo>>;

  dashboardId: string | null;

  @ViewChild('dashboardSelectPanelOrigin') dashboardSelectPanelOrigin: CdkOverlayOrigin;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private dashboardService: DashboardService,
              private utils: UtilsService,
              private overlay: Overlay,
              private breakpointObserver: BreakpointObserver,
              private viewContainerRef: ViewContainerRef,
              @Inject(DOCUMENT) private document: Document,
              @Inject(WINDOW) private window: Window) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {

    const pageLink = new PageLink(100);

    this.dashboards$ = this.getDashboards(pageLink).pipe(
      map((pageData) => pageData.data),
      share()
    );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: string | null): void {
    this.dashboardId = value;
  }

  dashboardIdChanged() {
    this.updateView();
  }

  openDashboardSelectPanel() {
    if (this.disabled) {
      return;
    }
    const panelHeight = this.breakpointObserver.isMatched('min-height: 350px') ? 250 : 150;
    const panelWidth = 300;
    const position = this.overlay.position();
    const config = new OverlayConfig({
      panelClass: 'tb-dashboard-select-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
    });
    const el = this.dashboardSelectPanelOrigin.elementRef.nativeElement;
    const offset = el.getBoundingClientRect();
    const scrollTop = this.window.pageYOffset || this.document.documentElement.scrollTop || this.document.body.scrollTop || 0;
    const scrollLeft = this.window.pageXOffset || this.document.documentElement.scrollLeft || this.document.body.scrollLeft || 0;
    const bottomY = offset.bottom - scrollTop;
    const leftX = offset.left - scrollLeft;
    let originX;
    let originY;
    let overlayX;
    let overlayY;
    const wHeight = this.document.documentElement.clientHeight;
    const wWidth = this.document.documentElement.clientWidth;
    if (bottomY + panelHeight > wHeight) {
      originY = 'top';
      overlayY = 'bottom';
    } else {
      originY = 'bottom';
      overlayY = 'top';
    }
    if (leftX + panelWidth > wWidth) {
      originX = 'end';
      overlayX = 'end';
    } else {
      originX = 'start';
      overlayX = 'start';
    }
    const connectedPosition: ConnectedPosition = {
      originX,
      originY,
      overlayX,
      overlayY
    };
    config.positionStrategy = position.flexibleConnectedTo(this.dashboardSelectPanelOrigin.elementRef)
      .withPositions([connectedPosition]);
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const injector = this._createDashboardSelectPanelInjector(
      overlayRef,
      {
        dashboards$: this.dashboards$,
        dashboardId: this.dashboardId,
        onDashboardSelected: (dashboardId) => {
          overlayRef.dispose();
          this.dashboardId = dashboardId;
          this.updateView();
        }
      }
    );
    overlayRef.attach(new ComponentPortal(DashboardSelectPanelComponent, this.viewContainerRef, injector));
  }

  public getDashboardTitle(title: string): string {
    return this.utils.customTranslation(title, title);
  }

  private _createDashboardSelectPanelInjector(overlayRef: OverlayRef, data: DashboardSelectPanelData): Injector {
    const providers: StaticProvider[] = [
      {provide: DASHBOARD_SELECT_PANEL_DATA, useValue: data},
      {provide: OverlayRef, useValue: overlayRef}
    ];
    return Injector.create({parent: this.viewContainerRef.injector, providers});
  }

  private updateView() {
    this.propagateChange(this.dashboardId);
  }

  private getDashboards(pageLink: PageLink): Observable<PageData<DashboardInfo>> {
    let dashboardsObservable: Observable<PageData<DashboardInfo>>;
    if (this.groupId) {
      dashboardsObservable = this.dashboardService.getGroupDashboards(this.groupId, pageLink, {ignoreLoading: true});
    } else {
      dashboardsObservable = this.dashboardService.getUserDashboards(null, this.operation, pageLink, {ignoreLoading: true});
    }
    return dashboardsObservable;
  }

}
