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

import { Component, ElementRef, forwardRef, Inject, Input, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { map, share } from 'rxjs/operators';
import { PageData } from '@shared/models/page/page-data';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TooltipPosition } from '@angular/material/tooltip';
import { CdkOverlayOrigin, Overlay } from '@angular/cdk/overlay';
import { BreakpointObserver } from '@angular/cdk/layout';
import { DOCUMENT } from '@angular/common';
import { WINDOW } from '@core/services/window.service';
import { RuleChain, RuleChainType } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';

// @dynamic
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
export class RuleChainSelectComponent implements ControlValueAccessor, OnInit {

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

  @Input()
  ruleChainType: RuleChainType = RuleChainType.CORE;

  ruleChains$: Observable<Array<RuleChain>>;

  ruleChainId: string | null;

  @ViewChild('ruleChainSelectPanelOrigin') ruleChainSelectPanelOrigin: CdkOverlayOrigin;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private ruleChainService: RuleChainService,
              private overlay: Overlay,
              private breakpointObserver: BreakpointObserver,
              private viewContainerRef: ViewContainerRef,
              private nativeElement: ElementRef,
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

    this.ruleChains$ = this.getRuleChains(pageLink).pipe(
      map((pageData) => pageData.data),
      share()
    );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: string | null): void {
    this.ruleChainId = value;
  }

  ruleChainIdChanged() {
    this.updateView();
  }

  private updateView() {
    this.propagateChange(this.ruleChainId);
  }

  private getRuleChains(pageLink: PageLink): Observable<PageData<RuleChain>> {
    return this.ruleChainService.getRuleChains(pageLink, this.ruleChainType, {ignoreLoading: true});
  }

}
