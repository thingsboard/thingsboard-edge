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
  AfterContentChecked,
  AfterContentInit,
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ContentChildren,
  Directive,
  ElementRef,
  EventEmitter,
  HostBinding,
  Input,
  OnDestroy,
  OnInit,
  Output,
  QueryList,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Subject, Subscription } from 'rxjs';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { coerceBoolean } from '@shared/decorators/coercion';
import { startWith, takeUntil } from 'rxjs/operators';
import { Platform } from '@angular/cdk/platform';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';

export interface ToggleHeaderOption {
  name: string;
  value: any;
}

export type ToggleHeaderAppearance = 'fill' | 'fill-invert' | 'stroked';

export type ScrollDirection = 'after' | 'before';

@Directive(
  {
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'tb-toggle-option',
  }
)
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class ToggleOption {

  @Input() value: any;

  get viewValue(): string {
    return (this._element?.nativeElement.textContent || '').trim();
  }

  constructor(
    private _element: ElementRef<HTMLElement>
  ) {}
}

@Directive()
export abstract class _ToggleBase extends PageComponent implements AfterContentInit, OnDestroy {

  @ContentChildren(ToggleOption) toggleOptions: QueryList<ToggleOption>;

  @Input()
  options: ToggleHeaderOption[] = [];

  protected _destroyed = new Subject<void>();

  protected constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngAfterContentInit(): void {
    this.toggleOptions.changes.pipe(startWith(null), takeUntil(this._destroyed)).subscribe(() => {
      this.syncToggleHeaderOptions();
    });
  }

  ngOnDestroy() {
    this._destroyed.next();
    this._destroyed.complete();
  }

  private syncToggleHeaderOptions() {
    if (this.toggleOptions?.length) {
      this.options.length = 0;
      this.toggleOptions.forEach(option => {
        this.options.push(
          { name: option.viewValue,
            value: option.value
          }
        );
      });
    }
  }

}

@Component({
  selector: 'tb-toggle-header',
  templateUrl: './toggle-header.component.html',
  styleUrls: ['./toggle-header.component.scss']
})
export class ToggleHeaderComponent extends _ToggleBase implements OnInit, AfterViewInit, AfterContentInit, AfterContentChecked, OnDestroy {

  @ViewChild('toggleGroup', {static: false})
  toggleGroup: ElementRef<HTMLElement>;

  @ViewChild(MatButtonToggleGroup, {static: false})
  buttonToggleGroup: MatButtonToggleGroup;

  @ViewChild('toggleGroupContainer', {static: false})
  toggleGroupContainer: ElementRef<HTMLElement>;

  @HostBinding('class.tb-toggle-header-pagination-controls-enabled')
  private showPaginationControls = false;

  private toggleGroupResize$: ResizeObserver;

  leftPaginationEnabled = false;
  rightPaginationEnabled = false;

  private _scrollDistance = 0;
  private _scrollDistanceChanged: boolean;

  get scrollDistance(): number {
    return this._scrollDistance;
  }
  set scrollDistance(value: number) {
    this._scrollTo(value);
  }

  @Input()
  value: any;

  @Output()
  valueChange = new EventEmitter<any>();

  @Input()
  name: string;

  @Input()
  @coerceBoolean()
  disablePagination = false;

  @Input()
  @coerceBoolean()
  useSelectOnMdLg = true;

  @Input()
  @coerceBoolean()
  ignoreMdLgSize = false;

  @Input()
  appearance: ToggleHeaderAppearance = 'stroked';

  @Input()
  @coerceBoolean()
  disabled = false;

  isMdLg: boolean;

  private observeBreakpointSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private platform: Platform,
              private breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {
    this.isMdLg = this.breakpointObserver.isMatched(MediaBreakpoints['md-lg']);
    this.observeBreakpointSubscription = this.breakpointObserver
      .observe(MediaBreakpoints['md-lg'])
      .subscribe((state: BreakpointState) => {
          this.isMdLg = state.matches;
          this.cd.markForCheck();
        }
      );
    if (!this.disablePagination) {
      this.valueChange.pipe(takeUntil(this._destroyed)).subscribe(() => {
        this.scrollToToggleOptionValue();
      });
    }
  }

  ngOnDestroy() {
    if (this.toggleGroupResize$) {
      this.toggleGroupResize$.disconnect();
    }
    super.ngOnDestroy();
  }

  ngAfterViewInit() {
    if (!this.disablePagination && !this.useSelectOnMdLg) {
      this.toggleGroupResize$ = new ResizeObserver(() => {
        this.updatePagination();
      });
      this.toggleGroupResize$.observe(this.toggleGroupContainer.nativeElement);
    }
  }

  ngAfterContentChecked() {
    if (this._scrollDistanceChanged) {
      this.updateToggleHeaderScrollPosition();
      this._scrollDistanceChanged = false;
      this.cd.markForCheck();
    }
  }

  trackByHeaderOption(index: number, option: ToggleHeaderOption){
    return option.value;
  }

  handlePaginatorClick(direction: ScrollDirection, $event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.scrollHeader(direction);
  }

  handlePaginatorTouchStart(direction: ScrollDirection, $event: Event) {
    if (direction === 'before' && !this.leftPaginationEnabled ||
        direction === 'after' && !this.rightPaginationEnabled) {
      $event.preventDefault();
    }
  }

  private scrollHeader(direction: ScrollDirection) {
    const viewLength = this.toggleGroup.nativeElement.offsetWidth;
    // Move the scroll distance one-third the length of the tab list's viewport.
    const scrollAmount = ((direction === 'before' ? -1 : 1) * viewLength) / 3;
    return this._scrollTo(this._scrollDistance + scrollAmount);
  }

  private scrollToToggleOptionValue() {
    if (this.buttonToggleGroup && this.buttonToggleGroup.selected) {
      const selectedToggleButton = this.buttonToggleGroup.selected as MatButtonToggle;
      const viewLength = this.toggleGroupContainer.nativeElement.offsetWidth;
      const {offsetLeft, offsetWidth} = (selectedToggleButton._buttonElement.nativeElement.offsetParent as HTMLElement);
      const labelBeforePos = offsetLeft; // this.toggleGroup.nativeElement.offsetWidth - offsetLeft;
      const labelAfterPos = labelBeforePos + offsetWidth;
      const beforeVisiblePos = this.scrollDistance;
      const afterVisiblePos = this.scrollDistance + viewLength;
      if (labelBeforePos < beforeVisiblePos) {
        this.scrollDistance -= beforeVisiblePos - labelBeforePos;
      } else if (labelAfterPos > afterVisiblePos) {
        this.scrollDistance += Math.min(
          labelAfterPos - afterVisiblePos,
          labelBeforePos - beforeVisiblePos,
        );
      }
    }
  }

  private updatePagination() {
    this.checkPaginationEnabled();
    this.checkPaginationControls();
    this.updateToggleHeaderScrollPosition();
  }

  private checkPaginationEnabled() {
    if (this.toggleGroupContainer) {
      const isEnabled = this.toggleGroup.nativeElement.scrollWidth > this.toggleGroupContainer.nativeElement.offsetWidth;
      if (isEnabled !== this.showPaginationControls) {
        if (!isEnabled) {
          this.scrollDistance = 0;
        } else {
          setTimeout(() => {
            this.scrollToToggleOptionValue();
          }, 0);
        }
        this.cd.markForCheck();
        this.showPaginationControls = isEnabled;
      }
    } else {
      this.showPaginationControls = false;
    }
  }

  private checkPaginationControls() {
    if (!this.showPaginationControls) {
      this.leftPaginationEnabled = this.rightPaginationEnabled = false;
    } else {
      // Check if the pagination arrows should be activated.
      this.leftPaginationEnabled = this.scrollDistance > 0;
      this.rightPaginationEnabled = this.scrollDistance < this.getMaxScrollDistance();
      this.cd.markForCheck();
    }
  }

  private getMaxScrollDistance(): number {
    const lengthOfToggleGroup = this.toggleGroup.nativeElement.scrollWidth;
    const viewLength = this.toggleGroupContainer.nativeElement.offsetWidth;
    return lengthOfToggleGroup - viewLength || 0;
  }

  private _scrollTo(position: number) {
    if (!this.showPaginationControls) {
      return {maxScrollDistance: 0, distance: 0};
    } else {
      const maxScrollDistance = this.getMaxScrollDistance();
      this._scrollDistance = Math.max(0, Math.min(maxScrollDistance, position));
      this._scrollDistanceChanged = true;
      this.checkPaginationControls();
      return {maxScrollDistance, distance: this._scrollDistance};
    }
  }

  private updateToggleHeaderScrollPosition() {
    const scrollDistance = this.scrollDistance;
    const translateX = -scrollDistance;
    this.toggleGroup.nativeElement.style.transform = `translateX(${Math.round(translateX)}px)`;
    if (this.platform.TRIDENT || this.platform.EDGE) {
      this.toggleGroupContainer.nativeElement.scrollLeft = 0;
    }
  }
}
