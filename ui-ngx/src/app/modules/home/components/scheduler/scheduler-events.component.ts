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

import { AfterViewInit, Component, ElementRef, Inject, Input, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetContext } from '@home/models/widget-component.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { SchedulerEvent, SchedulerEventWithCustomerInfo } from '@shared/models/scheduler-event.models';
import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { BehaviorSubject, forkJoin, fromEvent, merge, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  publishReplay,
  refCount,
  take,
  tap
} from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { TranslateService } from '@ngx-translate/core';
import { deepClone } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import {
  SchedulerEventDialogComponent,
  SchedulerEventDialogData
} from '@home/components/scheduler/scheduler-event-dialog.component';
import {
  defaultSchedulerEventConfigTypes,
  SchedulerEventConfigType
} from '@home/components/scheduler/scheduler-event-config.models';
import { DialogService } from '@core/services/dialog.service';
import dayGridPlugin from '@fullcalendar/daygrid';
import listPlugin from '@fullcalendar/list';
import timeGridPlugin from '@fullcalendar/timegrid';
import { FullCalendarComponent } from '@fullcalendar/angular';
import {
  schedulerCalendarView, schedulerCalendarViewTranslationMap,
  schedulerCalendarViewValueMap
} from '@home/components/scheduler/scheduler-events.models';
import { Calendar } from '@fullcalendar/core/Calendar';
import { rangeContainsMarker } from '@fullcalendar/core';

@Component({
  selector: 'tb-scheduler-events',
  templateUrl: './scheduler-events.component.html',
  styleUrls: ['./scheduler-events.component.scss']
})
export class SchedulerEventsComponent extends PageComponent implements OnInit, AfterViewInit {

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  @ViewChild('calendar') calendarComponent: FullCalendarComponent;

  @Input()
  widgetMode: boolean;

  @Input()
  ctx: WidgetContext;

  editEnabled = this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.WRITE);
  addEnabled = this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.CREATE);
  deleteEnabled = this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.DELETE);

  authUser = getCurrentAuthUser(this.store);

  showData = (this.authUser.authority === Authority.TENANT_ADMIN ||
    this.authUser.authority === Authority.CUSTOMER_USER) &&
    this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.READ);

  mode = 'list';

  displayCreatedTime = true;
  displayType = true;
  displayCustomer = true;

  schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType};

  displayPagination = true;
  defaultPageSize = 10;
  defaultSortOrder = 'createdTime';
  defaultEventType: string;

  displayedColumns: string[];
  pageLink: PageLink;

  textSearchMode = false;

  dataSource: SchedulerEventsDatasource;

  calendarPlugins = [dayGridPlugin, listPlugin, timeGridPlugin];

  currentCalendarView = schedulerCalendarView.month;

  currentCalendarViewValue = schedulerCalendarViewValueMap.get(this.currentCalendarView);

  schedulerCalendarViews = Object.keys(schedulerCalendarView);
  schedulerCalendarViewTranslations = schedulerCalendarViewTranslationMap;

  calendarApi: Calendar;

  constructor(protected store: Store<AppState>,
              public translate: TranslateService,
              private schedulerEventService: SchedulerEventService,
              private userPermissionsService: UserPermissionsService,
              private dialogService: DialogService,
              private dialog: MatDialog) {
    super(store);
  }

  ngOnInit(): void {
    if (this.showData && this.widgetMode) {
      // TODO:
      this.displayedColumns = ['select', 'createdTime', 'name', 'typeName', 'customerTitle', 'actions'];
      const sortOrder: SortOrder = { property: this.defaultSortOrder, direction: Direction.ASC };
      this.pageLink = new PageLink(this.defaultPageSize, 0, null, sortOrder);
      this.schedulerEventConfigTypes = deepClone(defaultSchedulerEventConfigTypes);
      this.dataSource = new SchedulerEventsDatasource(this.schedulerEventService, this.schedulerEventConfigTypes);
    } else {
      this.displayedColumns = ['select', 'createdTime', 'name', 'typeName', 'customerTitle', 'actions'];
      const sortOrder: SortOrder = { property: this.defaultSortOrder, direction: Direction.ASC };
      this.pageLink = new PageLink(this.defaultPageSize, 0, null, sortOrder);
      this.schedulerEventConfigTypes = deepClone(defaultSchedulerEventConfigTypes);
      this.dataSource = new SchedulerEventsDatasource(this.schedulerEventService, this.schedulerEventConfigTypes);
    }
  }

  ngAfterViewInit() {

    setTimeout(() => {
      this.calendarApi = this.calendarComponent.getApi();
    }, 0);

    fromEvent(this.searchInputField.nativeElement, 'keyup')
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(() => {
          this.paginator.pageIndex = 0;
          this.updateData();
        })
      )
      .subscribe();

    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(this.sort.sortChange, this.paginator.page)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();

    this.updateData(true);
  }

  updateData(reload: boolean = false) {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadEntities(this.pageLink, this.defaultEventType, reload);
  }

  enterFilterMode() {
    this.textSearchMode = true;
    this.pageLink.textSearch = '';
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    this.paginator.pageIndex = 0;
    this.updateData();
  }

  reloadSchedulerEvents() {
    this.updateData(true);
  }

  deleteSchedulerEvent($event: Event, schedulerEvent: SchedulerEventWithCustomerInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('scheduler.delete-scheduler-event-title', {schedulerEventName: schedulerEvent.name});
    const content = this.translate.instant('scheduler.delete-scheduler-event-text');
    this.dialogService.confirm(title, content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')).subscribe((result) => {
      if (result) {
        this.schedulerEventService.deleteSchedulerEvent(schedulerEvent.id.id).subscribe(
          () => {
            this.reloadSchedulerEvents();
          }
        );
      }
    });
  }

  deleteSchedulerEvents($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const selectedSchedulerEvents = this.dataSource.selection.selected;
    if (selectedSchedulerEvents && selectedSchedulerEvents.length) {
      const title = this.translate.instant('scheduler.delete-scheduler-events-title', {count: selectedSchedulerEvents.length});
      const content = this.translate.instant('scheduler.delete-scheduler-events-text');
      this.dialogService.confirm(title, content,
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')).subscribe((result) => {
        if (result) {
          const tasks = selectedSchedulerEvents.map((schedulerEvent) =>
            this.schedulerEventService.deleteSchedulerEvent(schedulerEvent.id.id));
          forkJoin(tasks).subscribe(
            () => {
              this.reloadSchedulerEvents();
            }
          );
        }
      });
    }
  }

  addSchedulerEvent($event: Event) {
    this.openSchedulerEventDialog($event);
  }

  editSchedulerEvent($event, schedulerEventWithCustomerInfo: SchedulerEventWithCustomerInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.schedulerEventService.getSchedulerEvent(schedulerEventWithCustomerInfo.id.id)
      .subscribe((schedulerEvent) => {
      this.openSchedulerEventDialog($event, schedulerEvent);
    });
  }

  viewSchedulerEvent($event, schedulerEventWithCustomerInfo: SchedulerEventWithCustomerInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.schedulerEventService.getSchedulerEvent(schedulerEventWithCustomerInfo.id.id)
      .subscribe((schedulerEvent) => {
        this.openSchedulerEventDialog($event, schedulerEvent, true);
      });
  }

  openSchedulerEventDialog($event: Event, schedulerEvent?: SchedulerEvent, readonly = false) {
    if ($event) {
      $event.stopPropagation();
    }
    let isAdd = false;
    if (!schedulerEvent || !schedulerEvent.id) {
      isAdd = true;
      if (!schedulerEvent) {
        schedulerEvent = {
          name: null,
          type: null,
          schedule: null,
          configuration: {
            originatorId: null,
            msgType: null,
            msgBody: {},
            metadata: {}
          }
        };
      }
    }
    this.dialog.open<SchedulerEventDialogComponent, SchedulerEventDialogData, boolean>(SchedulerEventDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        schedulerEventConfigTypes: this.schedulerEventConfigTypes,
        isAdd,
        readonly,
        schedulerEvent,
        defaultEventType: this.defaultEventType
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.reloadSchedulerEvents();
        }
      }
    );
  }

  triggerResize() {
    setTimeout(() => {
      this.calendarComponent.getApi().updateSize();
    }, 0);
  }

  changeCalendarView() {
    this.currentCalendarViewValue = schedulerCalendarViewValueMap.get(this.currentCalendarView);
    this.calendarApi.changeView(this.currentCalendarViewValue);
  }

  calendarViewTitle(): string {
    if (this.calendarApi) {
      return this.calendarApi.view.title;
    } else {
      return '';
    }
  }

  gotoCalendarToday() {
    this.calendarApi.today();
  }

  isCalendarToday(): boolean {
    if (this.calendarApi) {
      const now = this.calendarApi.getNow();
      const view = this.calendarApi.view;
      return rangeContainsMarker(view.props.dateProfile.currentRange, now);
    } else {
      return false;
    }
  }

  gotoCalendarPrev() {
    this.calendarApi.prev();
  }

  gotoCalendarNext() {
    this.calendarApi.next();
  }

  onEventClick(event) {
    // this.calendarComponent.getApi().changeView()
    console.log(event);
  }

  onDayClick(event) {
    console.log(event);
  }

  onEventDrop(event) {
    console.log(event);
  }

  eventRender(event) {
    console.log(event);
  }

}

class SchedulerEventsDatasource implements DataSource<SchedulerEventWithCustomerInfo> {

  private entitiesSubject = new BehaviorSubject<SchedulerEventWithCustomerInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<SchedulerEventWithCustomerInfo>>(emptyPageData<SchedulerEventWithCustomerInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<SchedulerEventWithCustomerInfo>(true, []);

  private allEntities: Observable<Array<SchedulerEventWithCustomerInfo>>;

  constructor(private schedulerEventService: SchedulerEventService,
              private schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType}) {
  }

  connect(collectionViewer: CollectionViewer):
    Observable<SchedulerEventWithCustomerInfo[] | ReadonlyArray<SchedulerEventWithCustomerInfo>> {
    return this.entitiesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.entitiesSubject.complete();
    this.pageDataSubject.complete();
  }

  reset() {
    const pageData = emptyPageData<SchedulerEventWithCustomerInfo>();
    this.entitiesSubject.next(pageData.data);
    this.pageDataSubject.next(pageData);
  }

  loadEntities(pageLink: PageLink, eventType: string,
               reload: boolean = false): Observable<PageData<SchedulerEventWithCustomerInfo>> {
    if (reload) {
      this.allEntities = null;
    }
    const result = new ReplaySubject<PageData<SchedulerEventWithCustomerInfo>>();
    this.fetchEntities(eventType, pageLink).pipe(
      tap(() => {
        this.selection.clear();
      }),
      catchError(() => of(emptyPageData<SchedulerEventWithCustomerInfo>())),
    ).subscribe(
      (pageData) => {
        this.entitiesSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
      }
    );
    return result;
  }

  fetchEntities(eventType: string,
                pageLink: PageLink): Observable<PageData<SchedulerEventWithCustomerInfo>> {
    return this.getAllEntities(eventType).pipe(
      map((data) => pageLink.filterData(data))
    );
  }

  getAllEntities(eventType: string): Observable<Array<SchedulerEventWithCustomerInfo>> {
    if (!this.allEntities) {
      this.allEntities = this.schedulerEventService.getSchedulerEvents(eventType).pipe(
        map((schedulerEvents) => {
          schedulerEvents.forEach((schedulerEvent) => {
            let typeName = schedulerEvent.type;
            if (this.schedulerEventConfigTypes[typeName]) {
              typeName = this.schedulerEventConfigTypes[typeName].name;
            }
            schedulerEvent.typeName = typeName;
          });
          return schedulerEvents;
        }),
        publishReplay(1),
        refCount()
      );
    }
    return this.allEntities;
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.entitiesSubject.pipe(
      map((entities) => numSelected === entities.length)
    );
  }

  isEmpty(): Observable<boolean> {
    return this.entitiesSubject.pipe(
      map((entities) => !entities.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

  masterToggle() {
    this.entitiesSubject.pipe(
      tap((entities) => {
        const numSelected = this.selection.selected.length;
        if (numSelected === entities.length) {
          this.selection.clear();
        } else {
          entities.forEach(row => {
            this.selection.select(row);
          });
        }
      }),
      take(1)
    ).subscribe();
  }
}
