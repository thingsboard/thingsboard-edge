///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, Inject, Input, OnDestroy, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { TooltipPosition } from '@angular/material/tooltip';
import { IAliasController } from '@core/api/widget-api.models';
import { CdkOverlayOrigin, ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { BreakpointObserver } from '@angular/cdk/layout';
import { DOCUMENT } from '@angular/common';
import { WINDOW } from '@core/services/window.service';
import { ComponentPortal, PortalInjector } from '@angular/cdk/portal';
import {
  ALIASES_ENTITY_SELECT_PANEL_DATA,
  AliasesEntitySelectPanelComponent,
  AliasesEntitySelectPanelData
} from './aliases-entity-select-panel.component';

// @dynamic
@Component({
  selector: 'tb-aliases-entity-select',
  templateUrl: './aliases-entity-select.component.html',
  styleUrls: ['./aliases-entity-select.component.scss']
})
export class AliasesEntitySelectComponent implements OnInit, OnDestroy {

  @Input()
  aliasController: IAliasController;

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  @Input() disabled: boolean;

  @ViewChild('aliasEntitySelectPanelOrigin') aliasEntitySelectPanelOrigin: CdkOverlayOrigin;

  displayValue: string;

  private rxSubscriptions = new Array<Subscription>();

  constructor(private translate: TranslateService,
              private overlay: Overlay,
              private breakpointObserver: BreakpointObserver,
              private viewContainerRef: ViewContainerRef,
              @Inject(DOCUMENT) private document: Document,
              @Inject(WINDOW) private window: Window) {
  }

  ngOnInit(): void {
    this.rxSubscriptions.push(this.aliasController.entityAliasesChanged.subscribe(
      () => {
        this.updateDisplayValue();
      }
    ));
    this.rxSubscriptions.push(this.aliasController.entityAliasResolved.subscribe(
      () => {
        this.updateDisplayValue();
      }
    ));
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  openEditMode() {
    if (this.disabled) {
      return;
    }
    const panelHeight = this.breakpointObserver.isMatched('min-height: 350px') ? 250 : 150;
    const panelWidth = 300;
    const position = this.overlay.position();
    const config = new OverlayConfig({
      panelClass: 'tb-aliases-entity-select-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
    });
    const el = this.aliasEntitySelectPanelOrigin.elementRef.nativeElement;
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
    config.positionStrategy = position.flexibleConnectedTo(this.aliasEntitySelectPanelOrigin.elementRef)
      .withPositions([connectedPosition]);
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const injector = this._createAliasesEntitySelectPanelInjector(
      overlayRef,
      {
        aliasController: this.aliasController
      }
    );
    overlayRef.attach(new ComponentPortal(AliasesEntitySelectPanelComponent, this.viewContainerRef, injector));
  }

  private _createAliasesEntitySelectPanelInjector(overlayRef: OverlayRef, data: AliasesEntitySelectPanelData): PortalInjector {
    const injectionTokens = new WeakMap<any, any>([
      [ALIASES_ENTITY_SELECT_PANEL_DATA, data],
      [OverlayRef, overlayRef]
    ]);
    return new PortalInjector(this.viewContainerRef.injector, injectionTokens);
  }

  private updateDisplayValue() {
    let displayValue;
    let singleValue = true;
    let currentAliasId;
    const entityAliases = this.aliasController.getEntityAliases();
    for (const aliasId of Object.keys(entityAliases)) {
      const entityAlias = entityAliases[aliasId];
      if (!entityAlias.filter.resolveMultiple) {
        const resolvedAlias = this.aliasController.getInstantAliasInfo(aliasId);
        if (resolvedAlias && resolvedAlias.currentEntity) {
          if (!currentAliasId) {
            currentAliasId = aliasId;
          } else {
            singleValue = false;
            break;
          }
        }
      }
    }
    if (singleValue && currentAliasId) {
      const aliasInfo = this.aliasController.getInstantAliasInfo(currentAliasId);
      displayValue = aliasInfo.currentEntity.name;
    } else {
      displayValue = this.translate.instant('entity.entities');
    }
    this.displayValue = displayValue;
  }

}
