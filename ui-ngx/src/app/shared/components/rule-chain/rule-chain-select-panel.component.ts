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

import { AfterViewInit, Component, ElementRef, Inject, InjectionToken, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, startWith, switchMap } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { OverlayRef } from '@angular/cdk/overlay';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { RuleChain, RuleChainType } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';

export const RULE_CHAIN_SELECT_PANEL_DATA = new InjectionToken<any>('RuleChainSelectPanelData');

export interface RuleChainSelectPanelData {
  ruleChainId: string | null;
  ruleChainType: RuleChainType;
}

@Component({
  selector: 'tb-rule-chain-select-panel',
  templateUrl: './rule-chain-select-panel.component.html',
  styleUrls: ['./rule-chain-select-panel.component.scss']
})
export class RuleChainSelectPanelComponent implements AfterViewInit {

  ruleChainId: string;
  private readonly ruleChainType: RuleChainType;

  selectRuleChainGroup: FormGroup;

  @ViewChild('ruleChainInput', {static: true}) userInput: ElementRef;

  filteredRuleChains: Observable<Array<RuleChain>>;

  searchText = '';

  ruleChainSelected = false;

  result?: RuleChain;

  private dirty = false;

  constructor(@Inject(RULE_CHAIN_SELECT_PANEL_DATA) public data: RuleChainSelectPanelData,
              private overlayRef: OverlayRef,
              private fb: FormBuilder,
              private ruleChainService: RuleChainService) {
    this.ruleChainId = data.ruleChainId;
    this.ruleChainType = data.ruleChainType;
    this.selectRuleChainGroup = this.fb.group({
      ruleChainInput: ['', {nonNullable: true}]
    });
    this.filteredRuleChains = this.selectRuleChainGroup.get('ruleChainInput').valueChanges
      .pipe(
        debounceTime(150),
        startWith(''),
        distinctUntilChanged((a: string, b: string) => a.trim() === b.trim()),
        switchMap(name => this.fetchRuleChains(name)),
        share()
      );
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.userInput.nativeElement.focus();
    }, 0);
  }

  selected(event: MatAutocompleteSelectedEvent): void {
    this.clear();
    this.ruleChainSelected = true;
    if (event.option.value?.id) {
      this.result = event.option.value;
    }
    this.overlayRef.dispose();
  }

  private fetchRuleChains(searchText?: string): Observable<Array<RuleChain>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.getRuleChains(pageLink)
      .pipe(
        catchError(() => of(emptyPageData<RuleChain>())),
        map(pageData => pageData.data)
      );
  }

  onFocus(): void {
    if (!this.dirty) {
      this.selectRuleChainGroup.get('ruleChainInput').updateValueAndValidity({onlySelf: true});
      this.dirty = true;
    }
  }

  clear() {
    this.selectRuleChainGroup.get('ruleChainInput').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.userInput.nativeElement.blur();
      this.userInput.nativeElement.focus();
    }, 0);
  }

  private getRuleChains(pageLink: PageLink): Observable<PageData<RuleChain>> {
    return this.ruleChainService.getRuleChains(pageLink, this.ruleChainType, {ignoreLoading: true});
  }

}
