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
  AfterViewInit, ChangeDetectorRef,
  Component,
  Input,
  OnChanges, OnDestroy,
  OnInit,
  Renderer2,
  SimpleChanges,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  GridEntitiesFetchFunction,
  ScrollGridColumns,
  ScrollGridDatasource
} from '@shared/components/grid/scroll-grid-datasource';
import { BreakpointObserver } from '@angular/cdk/layout';
import { isObject } from '@app/core/utils';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { ResizeObserver } from '@juggle/resize-observer';

export type ItemSizeFunction = (itemWidth: number) => number;

export interface ItemSizeStrategy {
  defaultItemSize: number;
  itemSizeFunction: ItemSizeFunction;
}

@Component({
  selector: 'tb-scroll-grid',
  templateUrl: './scroll-grid.component.html',
  styleUrls: ['./scroll-grid.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScrollGridComponent<T, F> implements OnInit, AfterViewInit, OnChanges, OnDestroy {

  @ViewChild('viewport')
  viewport: CdkVirtualScrollViewport;

  @Input()
  columns: ScrollGridColumns | number = 1;

  @Input()
  fetchFunction: GridEntitiesFetchFunction<T, F>;

  @Input()
  filter: F;

  @Input()
  itemSize: number | ItemSizeStrategy = 200;

  @Input()
  gap = 12;

  @Input()
  itemCard: TemplateRef<{item: T}>;

  @Input()
  loadingCell: TemplateRef<any>;

  @Input()
  dataLoading: TemplateRef<any>;

  @Input()
  noData: TemplateRef<any>;

  dataSource: ScrollGridDatasource<T, F>;

  calculatedItemSize: number;
  minBuffer: number;
  maxBuffer: number;

  private contentResize$: ResizeObserver;

  constructor(private breakpointObserver: BreakpointObserver,
              private cd: ChangeDetectorRef,
              private renderer: Renderer2) {
  }

  ngOnInit(): void {
    if (typeof this.itemSize === 'number') {
      this.calculatedItemSize = this.itemSize;
    } else {
      this.calculatedItemSize = this.itemSize.defaultItemSize;
    }
    this.minBuffer = this.calculatedItemSize;
    this.maxBuffer = this.calculatedItemSize * 2;
    this.dataSource = new ScrollGridDatasource<T, F>(this.breakpointObserver, this.columns, this.fetchFunction, this.filter);
  }

  ngAfterViewInit() {
    this.renderer.setStyle(this.viewport._contentWrapper.nativeElement, 'gap', this.gap + 'px');
    this.renderer.setStyle(this.viewport._contentWrapper.nativeElement, 'padding', this.gap + 'px');
    if (!(typeof this.itemSize === 'number')) {
      this.contentResize$ = new ResizeObserver(() => {
        this.onContentResize();
      });
      this.contentResize$.observe(this.viewport._contentWrapper.nativeElement);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue && propName === 'filter') {
        this.dataSource.updateFilter(this.filter);
      }
    }
  }

  ngOnDestroy() {
    if (this.contentResize$) {
      this.contentResize$.disconnect();
    }
  }

  isObject(value: any): boolean {
    return isObject(value);
  }

  trackByItemsRow(index: number, itemsRow: T[]): number {
    return index;
  }

  trackByItem(index: number, item: T): T {
    return item;
  }

  public update() {
    this.dataSource.update();
  }

  public updateItem(index: number, item: T) {
    this.dataSource.updateItem(index, item);
  }

  public deleteItem(index: number) {
    this.dataSource.deleteItem(index);
  }

  private onContentResize() {
    const contentWidth = this.viewport._contentWrapper.nativeElement.getBoundingClientRect().width;
    const columns = this.dataSource.currentColumns;
    const itemWidth = (contentWidth - this.gap * (columns + 1)) / columns;
    this.calculatedItemSize = (this.itemSize as ItemSizeStrategy).itemSizeFunction(itemWidth);
    this.minBuffer = this.calculatedItemSize;
    this.maxBuffer = this.calculatedItemSize * 2;
    this.cd.markForCheck();
  }
}
