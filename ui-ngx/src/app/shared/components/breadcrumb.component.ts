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

import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { BreadCrumb, BreadCrumbConfig } from './breadcrumb';
import { ActivatedRoute, ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { distinctUntilChanged, filter, map } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { MenuSection } from '@core/services/menu.models';
import { MenuService } from '@core/services/menu.service';
import { UtilsService } from '@core/services/utils.service';

@Component({
  selector: 'tb-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.scss']
})
export class BreadcrumbComponent implements OnInit, OnDestroy {

  activeComponentValue: any;

  @Input()
  set activeComponent(activeComponent: any) {
    this.activeComponentValue = activeComponent;
  }

  breadcrumbs$: Subject<Array<BreadCrumb>> = new BehaviorSubject<Array<BreadCrumb>>(this.buildBreadCrumbs(this.activatedRoute.snapshot));

  routerEventsSubscription = this.router.events.pipe(
    filter((event) => event instanceof NavigationEnd ),
    distinctUntilChanged(),
    map( () => this.buildBreadCrumbs(this.activatedRoute.snapshot) )
  ).subscribe(breadcrumns => this.breadcrumbs$.next(breadcrumns) );

  lastBreadcrumb$ = this.breadcrumbs$.pipe(
    map( breadcrumbs => breadcrumbs[breadcrumbs.length - 1])
  );

  constructor(private router: Router,
              private activatedRoute: ActivatedRoute,
              private translate: TranslateService,
              private menuService: MenuService,
              public utils: UtilsService) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
    if (this.routerEventsSubscription) {
      this.routerEventsSubscription.unsubscribe();
    }
  }


  buildBreadCrumbs(route: ActivatedRouteSnapshot, breadcrumbs: Array<BreadCrumb> = []): Array<BreadCrumb> {
    let newBreadcrumbs = breadcrumbs;
    if (route.routeConfig && route.routeConfig.data) {
      const breadcrumbConfig = route.routeConfig.data.breadcrumb as BreadCrumbConfig;
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
          section = breadcrumbConfig.custom ? this.menuService.getCurrentCustomSection()
            : this.menuService.getCurrentCustomChildSection();
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
            labelFunction = () => {
              return breadcrumbConfig.labelFunction(route, this.translate, this.activeComponentValue);
            };
            ignoreTranslate = true;
          } else {
            label = breadcrumbConfig.label || 'home.home';
            ignoreTranslate = false;
          }
          icon = breadcrumbConfig.icon || 'home';
          isMdiIcon = icon.startsWith('mdi:');
          link = [route.pathFromRoot.map(v => v.url.map(segment => segment.toString()).join('/')).join('/')];
          queryParams = route.queryParams;
        }
        const breadcrumb = {
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
      return this.buildBreadCrumbs(route.firstChild, newBreadcrumbs);
    }
    return newBreadcrumbs;
  }
}
