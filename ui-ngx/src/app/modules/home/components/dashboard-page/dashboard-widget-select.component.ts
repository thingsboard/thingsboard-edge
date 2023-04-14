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
import { WidgetInfo, widgetType } from '@shared/models/widget.models';
import { distinctUntilChanged, map, publishReplay, refCount, share, switchMap, tap } from 'rxjs/operators';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-dashboard-widget-select',
  templateUrl: './dashboard-widget-select.component.html',
  styleUrls: ['./dashboard-widget-select.component.scss']
})
export class DashboardWidgetSelectComponent implements OnInit {

  private search$ = new BehaviorSubject<string>('');
  private filterWidgetTypes$ = new BehaviorSubject<Array<widgetType>>(null);
  private widgetsInfo: Observable<Array<WidgetInfo>>;
  private widgetsBundleValue: WidgetsBundle;
  widgetTypes = new Set<widgetType>();

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
      }
      this.filterWidgetTypes$.next(null);
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

  @Output()
  widgetSelected: EventEmitter<WidgetInfo> = new EventEmitter<WidgetInfo>();

  @Output()
  widgetsBundleSelected: EventEmitter<WidgetsBundle> = new EventEmitter<WidgetsBundle>();

  constructor(private widgetsService: WidgetService,
              private sanitizer: DomSanitizer,
              private cd: ChangeDetectorRef) {
    this.widgetsBundles$ = this.search$.asObservable().pipe(
      distinctUntilChanged(),
      switchMap(search => this.fetchWidgetBundle(search))
    );
    this.widgets$ = combineLatest([this.search$.asObservable(), this.filterWidgetTypes$.asObservable()]).pipe(
      distinctUntilChanged((oldValue, newValue) => JSON.stringify(oldValue) === JSON.stringify(newValue)),
      switchMap(search => this.fetchWidget(...search))
    );
  }

  ngOnInit(): void {
  }

  private getWidgets(): Observable<Array<WidgetInfo>> {
    if (!this.widgetsInfo) {
      if (this.widgetsBundle !== null) {
        const bundleAlias = this.widgetsBundle.alias;
        const isSystem = this.widgetsBundle.tenantId.id === NULL_UUID;
        this.loadingWidgetsSubject.next(true);
        this.widgetsInfo = this.widgetsService.getBundleWidgetTypeInfos(bundleAlias, isSystem).pipe(
          map(widgets => {
            widgets = widgets.sort((a, b) => b.createdTime - a.createdTime);
            const widgetTypes = new Set<widgetType>();
            const widgetInfos = widgets.map((widgetTypeInfo) => {
                widgetTypes.add(widgetTypeInfo.widgetType);
                const widget: WidgetInfo = {
                  isSystemType: isSystem,
                  bundleAlias,
                  typeAlias: widgetTypeInfo.alias,
                  type: widgetTypeInfo.widgetType,
                  title: widgetTypeInfo.name,
                  image: widgetTypeInfo.image,
                  description: widgetTypeInfo.description
                };
                return widget;
              }
            );
            setTimeout(() => {
              this.widgetTypes = widgetTypes;
              this.cd.markForCheck();
            });
            return widgetInfos;
          }),
          tap(() => {
            this.loadingWidgetsSubject.next(false);
          }),
          publishReplay(1),
          refCount()
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

  private getWidgetsBundle(): Observable<Array<WidgetsBundle>> {
    return this.widgetsService.getAllWidgetsBundles().pipe(
      tap(() => this.loadingWidgetBundlesSubject.next(false)),
      publishReplay(1),
      refCount()
    );
  }

  private fetchWidgetBundle(search: string): Observable<Array<WidgetsBundle>> {
    return this.getWidgetsBundle().pipe(
      map(bundles => search ? bundles.filter(
        bundle => (
          bundle.title?.toLowerCase().includes(search.toLowerCase()) ||
          bundle.description?.toLowerCase().includes(search.toLowerCase())
        )) : bundles
      )
    );
  }

  private fetchWidget(search: string, filter: widgetType[]): Observable<Array<WidgetInfo>> {
    return this.getWidgets().pipe(
      map(widgets => filter ? widgets.filter((widget) => filter.includes(widget.type)) : widgets),
      map(widgets => search ? widgets.filter(
        widget => (
          widget.title?.toLowerCase().includes(search.toLowerCase()) ||
          widget.description?.toLowerCase().includes(search.toLowerCase())
        )) : widgets
      )
    );
  }
}
