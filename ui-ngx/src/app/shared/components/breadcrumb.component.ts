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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { BreadCrumb, BreadCrumbConfig } from './breadcrumb';
import { ActivatedRoute, ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { distinctUntilChanged, filter, map, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { MenuSection } from '@core/services/menu.models';
import { MenuService } from '@core/services/menu.service';
import { UtilsService } from '@core/services/utils.service';
import { guid } from '@core/utils';
import { BroadcastService } from '@core/services/broadcast.service';
import { ActiveComponentService } from '@core/services/active-component.service';

@Component({
  selector: 'tb-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BreadcrumbComponent implements OnInit, OnDestroy {

  activeComponentValue: any;
  updateBreadcrumbsSubscription: Subscription = null;

  setActiveComponent(activeComponent: any) {
    if (this.updateBreadcrumbsSubscription) {
      this.updateBreadcrumbsSubscription.unsubscribe();
      this.updateBreadcrumbsSubscription = null;
    }
    this.activeComponentValue = activeComponent;
    if (this.activeComponentValue && this.activeComponentValue.updateBreadcrumbs) {
      this.updateBreadcrumbsSubscription = this.activeComponentValue.updateBreadcrumbs.subscribe(() => {
        this.breadcrumbs$.next(this.buildBreadCrumbs(this.activatedRoute.snapshot));
      });
    }
  }

  breadcrumbs$: Subject<Array<BreadCrumb>> = new BehaviorSubject<Array<BreadCrumb>>([]);

  routerEventsSubscription = this.router.events.pipe(
    filter((event) => event instanceof NavigationEnd ),
    distinctUntilChanged(),
    map( () => this.buildBreadCrumbs(this.activatedRoute.snapshot) )
  ).subscribe(breadcrumns => this.breadcrumbs$.next(breadcrumns) );

  activeComponentSubscription = this.activeComponentService.onActiveComponentChanged().subscribe(comp => this.setActiveComponent(comp));

  lastBreadcrumb$ = this.breadcrumbs$.pipe(
    map( breadcrumbs => breadcrumbs[breadcrumbs.length - 1])
  );

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private broadcast: BroadcastService,
              private activeComponentService: ActiveComponentService,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private menuService: MenuService,
              public utils: UtilsService) {
  }

  ngOnInit(): void {
    this.broadcast.on('updateBreadcrumb', () => {
      this.cd.markForCheck();
    });
    this.setActiveComponent(this.activeComponentService.getCurrentActiveComponent());
  }

  ngOnDestroy(): void {
    if (this.routerEventsSubscription) {
      this.routerEventsSubscription.unsubscribe();
    }
    if (this.activeComponentSubscription) {
      this.activeComponentSubscription.unsubscribe();
    }
  }

  private lastChild(route: ActivatedRouteSnapshot) {
    let child = route;
    while (child.firstChild !== null) {
      child = child.firstChild;
    }
    return child;
  }

  buildBreadCrumbs(route: ActivatedRouteSnapshot, breadcrumbs: Array<BreadCrumb> = [],
                   lastChild?: ActivatedRouteSnapshot): Array<BreadCrumb> {
    if (!lastChild) {
      lastChild = this.lastChild(route);
    }
    let newBreadcrumbs = breadcrumbs;
    if (route.routeConfig && route.routeConfig.data) {
      const breadcrumbConfig = route.routeConfig.data.breadcrumb as BreadCrumbConfig<any>;
      if (breadcrumbConfig && !breadcrumbConfig.skip) {
        let label;
        let labelFunction;
        let ignoreTranslate;
        let icon;
        let iconUrl;
        let isMdiIcon;
        let link;
        let queryParams;
        let section: MenuSection = null;
        if (breadcrumbConfig.custom || breadcrumbConfig.customChild) {
          section = breadcrumbConfig.customChild ? this.menuService.getCurrentCustomChildSection()
            : this.menuService.getCurrentCustomSection();
        }
        if (section) {
          ignoreTranslate = true;
          label = section.name;
          icon = section.icon;
          if (icon) {
            isMdiIcon = icon.startsWith('mdi:');
          }
          iconUrl = section.iconUrl;
          link = section.path;
          queryParams = section.queryParams;
        } else {
          if (breadcrumbConfig.labelFunction) {
            labelFunction = () => this.activeComponentValue ?
              breadcrumbConfig.labelFunction(route, this.translate, this.activeComponentValue, lastChild.data, this.utils) :
              breadcrumbConfig.label;
            ignoreTranslate = true;
          } else {
            label = breadcrumbConfig.label || 'home.home';
            ignoreTranslate = false;
          }
          icon = breadcrumbConfig.icon || 'home';
          isMdiIcon = icon.startsWith('mdi:');
          link = [route.pathFromRoot.map(v => v.url.map(segment => segment.toString()).join('/')).join('/')];
        }
        const breadcrumb = {
          id: guid(),
          label,
          labelFunction,
          ignoreTranslate,
          icon,
          iconUrl,
          isMdiIcon,
          link,
          queryParams
        };
        newBreadcrumbs = [...breadcrumbs, breadcrumb];
      }
    }
    if (route.firstChild) {
      return this.buildBreadCrumbs(route.firstChild, newBreadcrumbs, lastChild);
    }
    return newBreadcrumbs;
  }

  trackByBreadcrumbs(index: number, breadcrumb: BreadCrumb){
    return breadcrumb.id;
  }
}
