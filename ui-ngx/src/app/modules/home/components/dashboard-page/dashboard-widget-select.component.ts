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

import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { IAliasController } from '@core/api/widget-api.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetService } from '@core/http/widget.service';
import { fullWidgetTypeFqn, WidgetInfo, widgetType, WidgetTypeInfo } from '@shared/models/widget.models';
import { debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { BehaviorSubject, combineLatest, Observable, of, ReplaySubject } from 'rxjs';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { isDefinedAndNotNull } from '@core/utils';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';

type widgetsListMode = 'all' | 'actual' | 'deprecated';

type selectWidgetMode = 'bundles' | 'allWidgets';

@Component({
  selector: 'tb-dashboard-widget-select',
  templateUrl: './dashboard-widget-select.component.html',
  styleUrls: ['./dashboard-widget-select.component.scss']
})
export class DashboardWidgetSelectComponent implements OnInit {

  private search$ = new BehaviorSubject<string>('');
  private filterWidgetTypes$ = new BehaviorSubject<Array<widgetType>>(null);
  private widgetsListMode$ = new BehaviorSubject<widgetsListMode>('actual');
  private selectWidgetMode$ = new BehaviorSubject<selectWidgetMode>('bundles');
  private widgetsInfo: Observable<Array<WidgetInfo>>;
  private widgetsBundleValue: WidgetsBundle;
  widgetTypes = new Set<widgetType>();
  hasDeprecated = false;

  allWidgets$: Observable<Array<WidgetInfo>>;
  widgets$: Observable<Array<WidgetInfo>>;
  loadingWidgetsSubject: BehaviorSubject<boolean> = new BehaviorSubject(false);
  loadingWidgets$ = this.loadingWidgetsSubject.pipe(
    share()
  );
  widgetsBundles$: Observable<Array<WidgetsBundle>>;
  loadingWidgetBundlesSubject: BehaviorSubject<boolean> = new BehaviorSubject(true);
  loadingWidgetBundles$ = this.loadingWidgetBundlesSubject.pipe(
    share()
  );

  set widgetsBundle(widgetBundle: WidgetsBundle) {
    if (this.widgetsBundleValue !== widgetBundle) {
      this.widgetsBundleValue = widgetBundle;
      if (widgetBundle === null) {
        this.widgetTypes.clear();
        this.hasDeprecated = false;
      }
      this.filterWidgetTypes$.next(null);
      this.widgetsListMode$.next('actual');
      this.widgetsInfo = null;
    }
  }

  get widgetsBundle(): WidgetsBundle {
    return this.widgetsBundleValue;
  }

  @Input()
  aliasController: IAliasController;

  @Input()
  set searchBundle(search: string) {
    this.search$.next(search);
  }

  @Input()
  set filterWidgetTypes(widgetTypes: Array<widgetType>) {
    this.filterWidgetTypes$.next(widgetTypes);
  }

  get filterWidgetTypes(): Array<widgetType> {
    return this.filterWidgetTypes$.value;
  }

  @Input()
  set selectWidgetMode(mode: selectWidgetMode) {
    this.selectWidgetMode$.next(mode);
  }

  get selectWidgetMode(): selectWidgetMode {
    return this.selectWidgetMode$.value;
  }

  @Input()
  set widgetsListMode(mode: widgetsListMode) {
    this.widgetsListMode$.next(mode);
  }

  get widgetsListMode(): widgetsListMode {
    return this.widgetsListMode$.value;
  }

  @Output()
  widgetSelected: EventEmitter<WidgetInfo> = new EventEmitter<WidgetInfo>();

  @Output()
  widgetsBundleSelected: EventEmitter<WidgetsBundle> = new EventEmitter<WidgetsBundle>();

  constructor(private widgetsService: WidgetService,
              private sanitizer: DomSanitizer,
              private cd: ChangeDetectorRef) {
    this.widgetsBundles$ = combineLatest([this.search$.asObservable(), this.selectWidgetMode$.asObservable()]).pipe(
      distinctUntilChanged((oldValue, newValue) => JSON.stringify(oldValue) === JSON.stringify(newValue)),
      switchMap(search => this.fetchWidgetBundle(...search))
    );
    this.allWidgets$ = combineLatest([this.search$.asObservable().pipe(
        debounceTime(150)
    ), this.selectWidgetMode$.asObservable()]).pipe(
        distinctUntilChanged((oldValue, newValue) => JSON.stringify(oldValue) === JSON.stringify(newValue)),
        switchMap(search => this.fetchAllWidgets(...search)),
        share({ connector: () => new ReplaySubject(1), resetOnError: false, resetOnComplete: false, resetOnRefCountZero: false })
    );
    this.widgets$ = combineLatest([this.search$.asObservable(), this.filterWidgetTypes$.asObservable(), this.widgetsListMode$]).pipe(
      distinctUntilChanged((oldValue, newValue) => JSON.stringify(oldValue) === JSON.stringify(newValue)),
      switchMap(search => this.fetchWidgets(...search))
    );
  }

  ngOnInit(): void {
  }

  private getWidgets(): Observable<Array<WidgetInfo>> {
    if (!this.widgetsInfo) {
      if (this.widgetsBundle !== null) {
        this.loadingWidgetsSubject.next(true);
        this.widgetsInfo = this.widgetsService.getBundleWidgetTypeInfos(this.widgetsBundle.id.id).pipe(
          map(widgets => {
            const widgetTypes = new Set<widgetType>();
            const hasDeprecated = widgets.some(w => w.deprecated);
            const widgetInfos = widgets.map((widgetTypeInfo) => {
                widgetTypes.add(widgetTypeInfo.widgetType);
                return this.toWidgetInfo(widgetTypeInfo);
              }
            );
            setTimeout(() => {
              this.widgetTypes = widgetTypes;
              this.hasDeprecated = hasDeprecated;
              this.cd.markForCheck();
            });
            return widgetInfos;
          }),
          tap(() => {
            this.loadingWidgetsSubject.next(false);
          }),
          share({ connector: () => new ReplaySubject(1), resetOnError: false, resetOnComplete: false, resetOnRefCountZero: false })
        );
      } else {
        this.widgetsInfo = of([]);
      }
    }
    return this.widgetsInfo;
  }

  onWidgetClicked($event: Event, widget: WidgetInfo): void {
    this.widgetSelected.emit(widget);
  }

  isSystem(item: WidgetsBundle): boolean {
    return item && item.tenantId.id === NULL_UUID;
  }

  selectBundle($event: Event, bundle: WidgetsBundle) {
    $event.preventDefault();
    this.widgetsBundle = bundle;
    this.search$.next('');
    this.widgetsBundleSelected.emit(bundle);
  }

  getPreviewImage(imageUrl: string | null): SafeUrl | string {
    if (isDefinedAndNotNull(imageUrl)) {
      return this.sanitizer.bypassSecurityTrustUrl(imageUrl);
    }
    return '/assets/widget-preview-empty.svg';
  }

  private getWidgetsBundles(): Observable<Array<WidgetsBundle>> {
    return this.widgetsService.getAllWidgetsBundles().pipe(
      tap(() => this.loadingWidgetBundlesSubject.next(false)),
      share({ connector: () => new ReplaySubject(1), resetOnError: false, resetOnComplete: false, resetOnRefCountZero: false })
    );
  }

  private fetchWidgetBundle(search: string, mode: selectWidgetMode): Observable<Array<WidgetsBundle>> {
    if (mode === 'bundles') {
      return this.getWidgetsBundles().pipe(
          map(bundles => search ? bundles.filter(
              bundle => (
                  bundle.title?.toLowerCase().includes(search.toLowerCase()) ||
                  bundle.description?.toLowerCase().includes(search.toLowerCase())
              )) : bundles
          )
      );
    } else {
      return of([]);
    }
  }

  private fetchWidgets(search: string, filter: widgetType[], listMode: widgetsListMode): Observable<Array<WidgetInfo>> {
    return this.getWidgets().pipe(
      map(widgets => (listMode && listMode !== 'all') ?
        widgets.filter((widget) => listMode === 'actual' ? !widget.deprecated : widget.deprecated) : widgets),
      map(widgets => filter ? widgets.filter((widget) => filter.includes(widget.type)) : widgets),
      map(widgets => search ? widgets.filter(
        widget => (
          widget.title?.toLowerCase().includes(search.toLowerCase()) ||
          widget.description?.toLowerCase().includes(search.toLowerCase())
        )) : widgets
      )
    );
  }

  private fetchAllWidgets(search: string, mode: selectWidgetMode): Observable<Array<WidgetInfo>> {
    if (mode === 'allWidgets') {
      const pageLink = new PageLink(1024, 0, search, {
        property: 'name',
        direction: Direction.ASC
      });
      return this.getAllWidgets(pageLink);
    } else {
      return of([]);
    }
  }

  private getAllWidgets(pageLink: PageLink): Observable<Array<WidgetInfo>> {
    this.loadingWidgetsSubject.next(true);
    return this.widgetsService.getWidgetTypes(pageLink, false, true).pipe(
      map(data => data.data.map(w => this.toWidgetInfo(w))),
      tap(() => {
        this.loadingWidgetsSubject.next(false);
      })
    );
  }

  private toWidgetInfo(widgetTypeInfo: WidgetTypeInfo): WidgetInfo {
    return {
      typeFullFqn: fullWidgetTypeFqn(widgetTypeInfo),
      type: widgetTypeInfo.widgetType,
      title: widgetTypeInfo.name,
      image: widgetTypeInfo.image,
      description: widgetTypeInfo.description,
      deprecated: widgetTypeInfo.deprecated
    };
  }
}
