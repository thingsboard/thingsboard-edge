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

import { Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetService } from '@core/http/widget.service';
import { isDefined } from '@core/utils';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

@Component({
  selector: 'tb-widgets-bundle-select',
  templateUrl: './widgets-bundle-select.component.html',
  styleUrls: ['./widgets-bundle-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => WidgetsBundleSelectComponent),
    multi: true
  }],
  encapsulation: ViewEncapsulation.None
})
export class WidgetsBundleSelectComponent implements ControlValueAccessor, OnInit, OnChanges {

  @Input()
  bundlesScope: 'system' | 'tenant';

  @Input()
  selectFirstBundle: boolean;

  @Input()
  selectBundleAlias: string;

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

  widgetsBundles$: Observable<Array<WidgetsBundle>>;

  widgetsBundles: Array<WidgetsBundle>;

  widgetsBundle: WidgetsBundle | null;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private widgetService: WidgetService) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.widgetsBundles$ = this.getWidgetsBundles().pipe(
      map((widgetsBundles) => {
        const authState = getCurrentAuthState(this.store);
        if (!authState.edgesSupportEnabled) {
          widgetsBundles = widgetsBundles.filter(widgetsBundle => widgetsBundle.alias !== 'edge_widgets');
        }
        return widgetsBundles;
      }),
      tap((widgetsBundles) => {
        this.widgetsBundles = widgetsBundles;
        if (this.selectFirstBundle) {
          if (widgetsBundles.length > 0) {
            if (this.widgetsBundle !== widgetsBundles[0]) {
              this.widgetsBundle = widgetsBundles[0];
              this.updateView();
            } else if (isDefined(this.selectBundleAlias)) {
              this.selectWidgetsBundleByAlias(this.selectBundleAlias);
            }
          }
        }
      }),
      share()
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'selectBundleAlias') {
          this.selectWidgetsBundleByAlias(this.selectBundleAlias);
        }
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: WidgetsBundle | null): void {
    this.widgetsBundle = value;
  }

  widgetsBundleChanged() {
    this.updateView();
  }

  isSystem(item: WidgetsBundle) {
    return item && item.tenantId.id === NULL_UUID;
  }

  private selectWidgetsBundleByAlias(alias: string) {
    if (this.widgetsBundles && alias) {
      const found = this.widgetsBundles.find((widgetsBundle) => widgetsBundle.alias === alias);
      if (found && this.widgetsBundle !== found) {
        this.widgetsBundle = found;
        this.updateView();
      }
    } else if (this.widgetsBundle) {
      this.widgetsBundle = null;
      this.updateView();
    }
  }

  private updateView() {
    this.propagateChange(this.widgetsBundle);
  }

  private getWidgetsBundles(): Observable<Array<WidgetsBundle>> {
    let widgetsBundlesObservable: Observable<Array<WidgetsBundle>>;
    if (this.bundlesScope) {
      if (this.bundlesScope === 'system') {
        widgetsBundlesObservable = this.widgetService.getSystemWidgetsBundles();
      } else if (this.bundlesScope === 'tenant') {
        widgetsBundlesObservable = this.widgetService.getTenantWidgetsBundles();
      }
    } else {
      widgetsBundlesObservable = this.widgetService.getAllWidgetsBundles();
    }
    return widgetsBundlesObservable;
  }

}
