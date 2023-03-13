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

import { AfterViewInit, Component, Inject, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WINDOW } from '@core/services/window.service';
import { BreakpointObserver } from '@angular/cdk/layout';
import { ActivatedRoute, Router } from '@angular/router';
import { MenuService } from '@core/services/menu.service';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { MenuSection } from '@core/services/menu.models';
import { instanceOfSearchableComponent } from '@home/models/searchable-component.models';
import { BroadcastService } from '@core/services/broadcast.service';
import { ActiveComponentService } from '@core/services/active-component.service';

@Component({
  selector: 'tb-router-tabs',
  templateUrl: './router-tabs.component.html',
  styleUrls: ['./route-tabs.component.scss']
})
export class RouterTabsComponent extends PageComponent implements AfterViewInit, OnInit {

  tabs$: Observable<Array<MenuSection>> = this.menuService.menuSections().pipe(
    map(sections => {
      const sectionPath = '/' + this.activatedRoute.pathFromRoot.map(r => r.snapshot.url)
        .filter(f => !!f[0]).map(f => f.map(f1 => f1.path).join('/')).join('/');
      const found = sections.find(section => sectionPath.endsWith(section.path));
      const rootPath = sectionPath.substring(0, sectionPath.length - found.path.length);
      const isRoot = rootPath === '';
      const tabs: Array<MenuSection> = found ? found.pages.filter(page => !page.disabled && (!page.rootOnly || isRoot)) : [];
      return tabs.map((tab) => ({...tab, path: rootPath + tab.path}));
    })
  );

  constructor(protected store: Store<AppState>,
              private activatedRoute: ActivatedRoute,
              public router: Router,
              private menuService: MenuService,
              private activeComponentService: ActiveComponentService,
              @Inject(WINDOW) private window: Window,
              public breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {
  }

  ngAfterViewInit() {}

  activeComponentChanged(activeComponent: any) {
    this.activeComponentService.setCurrentActiveComponent(activeComponent);
  }

}
