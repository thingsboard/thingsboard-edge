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

// tslint:disable-next-line:no-reference
/// <reference path="../../../../../../../src/typings/split.js.typings.d.ts" />

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild, ViewEncapsulation, ViewChildren, QueryList, ComponentFactoryResolver
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { combineLatest, fromEvent, merge } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import {
  WidgetActionDescriptorInfo,
  WidgetActionsData,
  WidgetActionsDatasource,
  WidgetActionCallbacks, toWidgetActionDescriptor
} from '@home/components/widget/action/manage-widget-actions.component.models';
import { UtilsService } from '@core/services/utils.service';
import { EntityRelation, EntitySearchDirection, RelationTypeGroup } from '@shared/models/relation.models';
import { RelationDialogComponent, RelationDialogData } from '@home/components/relation/relation-dialog.component';
import { CustomActionDescriptor, WidgetActionDescriptor, WidgetActionSource } from '@shared/models/widget.models';
import {
  WidgetActionDialogComponent,
  WidgetActionDialogData
} from '@home/components/widget/action/widget-action-dialog.component';
import { deepClone } from '@core/utils';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { CustomActionPrettyResourcesTabsComponent } from '@home/components/widget/action/custom-action-pretty-resources-tabs.component';
import { MatTab, MatTabGroup } from '@angular/material/tabs';

@Component({
  selector: 'tb-custom-action-pretty-editor',
  templateUrl: './custom-action-pretty-editor.component.html',
  styleUrls: ['./custom-action-pretty-editor.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CustomActionPrettyEditorComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class CustomActionPrettyEditorComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy, ControlValueAccessor {

  @Input() disabled: boolean;

  action: CustomActionDescriptor;

  fullscreen = false;

  @ViewChildren('leftPanel')
  leftPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('rightPanel')
  rightPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  private propagateChange = (_: any) => {};

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    combineLatest(this.leftPanelElmRef.changes, this.rightPanelElmRef.changes).subscribe(() => {
      if (this.leftPanelElmRef.length && this.rightPanelElmRef.length) {
        this.initSplitLayout(this.leftPanelElmRef.first.nativeElement,
          this.rightPanelElmRef.first.nativeElement);
      }
    });
  }

  private initSplitLayout(leftPanel: any, rightPanel: any) {
    Split([leftPanel, rightPanel], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'col-resize'
    });
  }

  ngOnDestroy(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(obj: CustomActionDescriptor): void {
    this.action = obj;
  }

  public onActionUpdated(valid: boolean = true) {
    if (!valid) {
      this.propagateChange(null);
    } else {
      this.propagateChange(this.action);
    }
  }
}
