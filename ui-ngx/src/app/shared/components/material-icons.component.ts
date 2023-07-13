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

import { PageComponent } from '@shared/components/page.component';
import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormControl } from '@angular/forms';
import { BehaviorSubject, combineLatest, debounce, Observable, of, timer } from 'rxjs';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { getMaterialIcons, MaterialIcon } from '@shared/models/icon.models';
import { distinctUntilChanged, map, mergeMap, share, startWith, tap } from 'rxjs/operators';
import { ResourcesService } from '@core/services/resources.service';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';

@Component({
  selector: 'tb-material-icons',
  templateUrl: './material-icons.component.html',
  providers: [],
  styleUrls: ['./material-icons.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MaterialIconsComponent extends PageComponent implements OnInit {

  @ViewChild('iconsPanel')
  iconsPanel: CdkVirtualScrollViewport;

  @Input()
  selectedIcon: string;

  @Input()
  popover: TbPopoverComponent<MaterialIconsComponent>;

  @Output()
  iconSelected = new EventEmitter<string>();

  iconRows$: Observable<MaterialIcon[][]>;
  showAllSubject = new BehaviorSubject<boolean>(false);
  searchIconControl: UntypedFormControl;

  iconsRowHeight = 48;

  iconsPanelHeight: string;
  iconsPanelWidth: string;

  notFound = false;

  constructor(protected store: Store<AppState>,
              private resourcesService: ResourcesService,
              private breakpointObserver: BreakpointObserver,
              private cd: ChangeDetectorRef) {
    super(store);
    this.searchIconControl = new UntypedFormControl('');
  }

  ngOnInit(): void {
    const iconsRowSize = this.breakpointObserver.isMatched(MediaBreakpoints['lt-md']) ? 8 : 11;
    this.calculatePanelSize(iconsRowSize);
    const iconsRowSizeObservable = this.breakpointObserver
      .observe(MediaBreakpoints['lt-md']).pipe(
        map((state) => state.matches ? 8 : 11),
        startWith(iconsRowSize),
    );
    this.iconRows$ = combineLatest({showAll: this.showAllSubject.asObservable(),
      rowSize: iconsRowSizeObservable,
      searchText: this.searchIconControl.valueChanges.pipe(
        startWith(''),
        debounce((searchText) => searchText ? timer(150) : of({})),
      )}).pipe(
      map((data) => {
        if (data.searchText && !data.showAll) {
          data.showAll = true;
          this.showAllSubject.next(true);
        }
        return data;
      }),
      distinctUntilChanged((p, c) => c.showAll === p.showAll && c.searchText === p.searchText && c.rowSize === p.rowSize),
      mergeMap((data) => getMaterialIcons(this.resourcesService, data.rowSize, data.showAll, data.searchText).pipe(
        map(iconRows => ({iconRows, iconsRowSize: data.rowSize}))
      )),
      tap((data) => {
        this.notFound = !data.iconRows.length;
        this.calculatePanelSize(data.iconsRowSize, data.iconRows.length);
        this.cd.markForCheck();
        setTimeout(() => {
          this.checkSize();
        }, 0);
      }),
      map((data) => data.iconRows),
      share()
    );
  }

  clearSearch() {
    this.searchIconControl.patchValue('', {emitEvent: true});
  }

  selectIcon(icon: MaterialIcon) {
    this.iconSelected.emit(icon.name);
  }

  private calculatePanelSize(iconsRowSize: number, iconRows = 4) {
    this.iconsPanelHeight = Math.min(iconRows * this.iconsRowHeight, 10 * this.iconsRowHeight) + 'px';
    this.iconsPanelWidth = (iconsRowSize * 36 + (iconsRowSize - 1) * 12 + 6) + 'px';
  }

  private checkSize() {
    this.iconsPanel?.checkViewportSize();
    this.popover?.updatePosition();
  }
}
