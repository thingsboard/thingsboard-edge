///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, Injector, Input, StaticProvider, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TooltipPosition } from '@angular/material/tooltip';
import { RuleChain, RuleChainType } from '@shared/models/rule-chain.models';
import { isDefinedAndNotNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import {
  RULE_CHAIN_SELECT_PANEL_DATA, RuleChainSelectPanelComponent,
  RuleChainSelectPanelData
} from '@shared/components/rule-chain/rule-chain-select-panel.component';
import { ComponentPortal } from '@angular/cdk/portal';
import { POSITION_MAP } from '@shared/models/overlay.models';

@Component({
  selector: 'tb-rule-chain-select',
  templateUrl: './rule-chain-select.component.html',
  styleUrls: ['./rule-chain-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RuleChainSelectComponent),
    multi: true
  }]
})
export class RuleChainSelectComponent implements ControlValueAccessor {

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  ruleChainType: RuleChainType = RuleChainType.CORE;

  ruleChain: RuleChain | null;

  panelOpened = false;

  private propagateChange = (v: any) => { };

  constructor(private overlay: Overlay,
              private viewContainerRef: ViewContainerRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: RuleChain): void {
    if (isDefinedAndNotNull(value)) {
      this.ruleChain = value;
    }
  }

  ruleChainIdChanged() {
    this.updateView();
  }

  openRuleChainSelectPanel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.disabled) {
      const target = $event.currentTarget;
      const config = new OverlayConfig({
        panelClass: 'tb-filter-panel',
        backdropClass: 'cdk-overlay-transparent-backdrop',
        hasBackdrop: true,
        width: (target as HTMLElement).offsetWidth
      });
      config.positionStrategy = this.overlay.position()
        .flexibleConnectedTo(target as HTMLElement)
        .withPositions([POSITION_MAP.bottom]);
      const overlayRef = this.overlay.create(config);
      overlayRef.backdropClick().subscribe(() => {
        overlayRef.dispose();
      });
      const providers: StaticProvider[] = [
        {
          provide: RULE_CHAIN_SELECT_PANEL_DATA,
          useValue: {
            ruleChainId: this.ruleChain.id?.id,
            ruleChainType: this.ruleChainType
          } as RuleChainSelectPanelData
        },
        {
          provide: OverlayRef,
          useValue: overlayRef
        }
      ];
      const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
      const componentRef = overlayRef.attach(new ComponentPortal(RuleChainSelectPanelComponent,
        this.viewContainerRef, injector));
      this.panelOpened = true;
      componentRef.onDestroy(() => {
        this.panelOpened = false;
        if (componentRef.instance.ruleChainSelected) {
          this.ruleChain = componentRef.instance.result;
          this.updateView();
        }
      });
    }
  }

  private updateView() {
    this.propagateChange(this.ruleChain);
  }

}
