///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, Injector, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { PageComponent } from '@shared/public-api';
import { Store } from '@ngrx/store';
import { AppState, deepClone } from '@core/public-api';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TranslateService } from '@ngx-translate/core';
import { AggLatestMapping } from './aggregate-latest-mapping.models';
import { MatDialog } from '@angular/material/dialog';
import {
  AggregateLatestMappingDialogComponent,
  AggregateLatestMappingDialogData
} from './aggregate-latest-mapping-dialog.component';
import { AggMathFunction, aggMathFunctionTranslations } from '@home/components/rule-node/rule-node-config.models';

@Component({
  selector: 'tb-agg-latest-mapping-table',
  templateUrl: './aggregate-latest-mapping-table.component.html',
  styleUrls: ['./aggregate-latest-mapping-table.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AggregateLatestMappingTableComponent),
      multi: true
    }
  ]
})
export class AggregateLatestMappingTableComponent extends PageComponent implements ControlValueAccessor, OnInit {

  @Input() tbelFilterFunctionOnly = false;

  @Input() disabled: boolean;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  mappings: Array<AggLatestMapping>;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              public translate: TranslateService,
              public injector: Injector,
              private dialog: MatDialog) {
    super(store);
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(mappings: Array<AggLatestMapping>): void {
    this.mappings = mappings || [];
  }

  public mappingDisplayValue(aggMapping: AggLatestMapping): string {
    let toDisplay = '';
    if (aggMapping.filter) {
      toDisplay += this.translate.instant('rule-node-config.filter-entities') + ' -> ';
    }
    toDisplay += this.translate.instant(aggMathFunctionTranslations.get(aggMapping.aggFunction));
    if (aggMapping.aggFunction !== AggMathFunction.COUNT) {
      toDisplay += '(' + aggMapping.source + ')';
    }
    toDisplay += ' -> ' + aggMapping.target;
    return toDisplay;
  }

  public removeAggMapping(index: number) {
    if (index > -1) {
      this.mappings.splice(index, 1);
      this.updateModel();
    }
  }

  public addAggMapping($event: Event) {
    this.openAggMappingDialog($event);
  }

  public editAggMapping($event: Event, aggMapping: AggLatestMapping) {
    this.openAggMappingDialog($event, aggMapping);
  }

  private openAggMappingDialog($event: Event, mapping?: AggLatestMapping) {
    if ($event) {
      $event.stopPropagation();
    }
    const isAdd = !mapping;
    let mappingIndex: number;
    if (!isAdd) {
      mappingIndex = this.mappings.indexOf(mapping);
    }
    this.dialog.open<AggregateLatestMappingDialogComponent,
      AggregateLatestMappingDialogData, AggLatestMapping>(AggregateLatestMappingDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        mapping: isAdd ? null : deepClone(mapping),
        tbelFilterFunctionOnly: this.tbelFilterFunctionOnly
      }
    }).afterClosed().subscribe(
      (newMapping) => {
        if (newMapping) {
          if (isAdd) {
            this.mappings.push(newMapping);
          } else {
            this.mappings[mappingIndex] = newMapping;
          }
          this.updateModel();
        }
      }
    );
  }

  private updateModel() {
    if (this.required && !this.mappings.length) {
      this.propagateChange(null);
    } else {
      this.propagateChange(this.mappings);
    }
  }
}
