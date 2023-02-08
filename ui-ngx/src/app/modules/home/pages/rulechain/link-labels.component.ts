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

import { Component, ElementRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { LinkLabel } from '@shared/models/rule-node.models';
import { Observable, of } from 'rxjs';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { deepClone } from '@core/utils';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipInputEvent, MatChipList } from '@angular/material/chips';
import { TranslateService } from '@ngx-translate/core';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { catchError, map, mergeMap, share, startWith } from 'rxjs/operators';
import { RuleChainService } from '@core/http/rule-chain.service';

@Component({
  selector: 'tb-link-labels',
  templateUrl: './link-labels.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => LinkLabelsComponent),
    multi: true
  }]
})
export class LinkLabelsComponent implements ControlValueAccessor, OnInit, OnChanges {

  @ViewChild('chipList') chipList: MatChipList;
  @ViewChild('labelAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('labelInput') labelInput: ElementRef<HTMLInputElement>;

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

  private allowCustomValue: boolean;
  get allowCustom(): boolean {
    return this.allowCustomValue;
  }
  @Input()
  set allowCustom(value: boolean) {
    this.allowCustomValue = coerceBooleanProperty(value);
    this.separatorKeysCodes = this.allowCustomValue ? [ENTER, COMMA, SEMICOLON] : [];
  }

  @Input()
  allowedLabels: {[label: string]: LinkLabel};

  @Input()
  sourceRuleChainId: string;

  linksFormGroup: UntypedFormGroup;

  modelValue: Array<string>;

  private labelsList: Array<LinkLabel> = [];

  separatorKeysCodes: number[] = [];

  filteredLabels: Observable<Array<LinkLabel>>;

  labels: Array<LinkLabel> = [];

  searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder,
              public truncate: TruncatePipe,
              public translate: TranslateService,
              private ruleChainService: RuleChainService) {
    this.linksFormGroup = this.fb.group({
      label: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }


  ngOnInit(): void {
    this.filteredLabels = this.linksFormGroup.get('label').valueChanges
      .pipe(
        startWith(''),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchLabels(name) ),
        share()
      );
  }

  ngOnChanges(changes: SimpleChanges): void {
    let reloadLabels = false;
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue) {
        if (['allowCustom', 'allowedLabels', 'sourceRuleChainId'].includes(propName)) {
          reloadLabels = true;
        }
      }
    }
    if (reloadLabels) {
      this.linksFormGroup.get('label').patchValue('', {emitEvent: true});
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.linksFormGroup.disable({emitEvent: false});
    } else {
      this.linksFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string[]): void {
    this.searchText = '';
    this.labelsList.length = 0;
    this.labels.length = 0;
    this.modelValue = value;
    this.prepareLabelsList().subscribe((labelsList) => {
      this.labelsList = labelsList;
      if (value) {
        value.forEach((label) => {
          if (this.allowedLabels[label]) {
            this.labels.push(deepClone(this.allowedLabels[label]));
          } else {
            this.labels.push({
              name: label,
              value: label
            });
          }
        });
      }
      if (this.chipList && this.required) {
        this.chipList.errorState = !this.labels.length;
      }
      this.linksFormGroup.get('label').patchValue('', {emitEvent: true});
    });
  }

  prepareLabelsList(): Observable<Array<LinkLabel>> {
    const labelsList: Array<LinkLabel> = [];
    if (this.sourceRuleChainId) {
      return this.ruleChainService.getRuleChainOutputLabels(this.sourceRuleChainId, {ignoreErrors: true}).pipe(
        map((labels) => {
          for (const label of labels) {
            labelsList.push({
              name: label,
              value: label
            });
          }
          return labelsList;
        }),
        catchError(() => {
          return of(labelsList);
        })
      );
    } else {
      for (const label of Object.keys(this.allowedLabels)) {
        labelsList.push({name: this.allowedLabels[label].name, value: this.allowedLabels[label].value});
      }
      return of(labelsList);
    }
  }

  displayLabelFn(label?: LinkLabel): string | undefined {
    return label ? label.name : undefined;
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  createLinkLabel($event: Event, value: string) {
    $event.preventDefault();
    this.transformLinkLabel(value);
  }

  add(event: MatChipInputEvent): void {
    if (!this.matAutocomplete.isOpen || this.allowCustom) {
      this.transformLinkLabel(event.value);
    }
  }

  private fetchLabels(searchText?: string): Observable<Array<LinkLabel>> {
    this.searchText = searchText;
    if (this.searchText && this.searchText.length) {
      const search = this.searchText.toUpperCase();
      return of(this.labelsList.filter(label => label.name.toUpperCase().includes(search)));
    } else {
      return of(this.labelsList);
    }
  }

  private transformLinkLabel(value: string) {
    if ((value || '').trim()) {
      let newLabel: LinkLabel = null;
      const labelName = value.trim();
      const existingLabel = this.labelsList.find(label => label.name === labelName);
      if (existingLabel) {
        newLabel = deepClone(existingLabel);
      } else if (this.allowCustom) {
        newLabel = {
          name: labelName,
          value: labelName
        };
      }
      if (newLabel) {
        this.addLabel(newLabel);
      }
    }
    this.clear('');
  }

  remove(label: LinkLabel) {
    const index = this.labels.indexOf(label);
    if (index >= 0) {
      this.labels.splice(index, 1);
      if (!this.labels.length) {
        if (this.required) {
          this.chipList.errorState = true;
        }
      }
      this.updateModel();
    }
  }

  selected(event: MatAutocompleteSelectedEvent): void {
    this.addLabel(event.option.value);
    this.clear('');
  }

  addLabel(label: LinkLabel): void {
    const index = this.labels.findIndex(existinglabel => existinglabel.value === label.value);
    if (index === -1) {
      this.labels.push(label);
      if (this.required) {
        this.chipList.errorState = false;
      }
      this.updateModel();
    }
  }

  onFocus() {
    this.linksFormGroup.get('label').updateValueAndValidity({onlySelf: true, emitEvent: true});
  }

  clear(value: string = '') {
    this.labelInput.nativeElement.value = value;
    this.linksFormGroup.get('label').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.labelInput.nativeElement.blur();
      this.labelInput.nativeElement.focus();
    }, 0);
  }

  private updateModel() {
    const labels = this.labels.map((label => label.value));
    this.propagateChange(labels);
  }
}

