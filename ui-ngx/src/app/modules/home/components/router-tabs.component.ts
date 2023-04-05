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

import { Component, ComponentRef, OnInit, Type, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { MenuService } from '@core/services/menu.service';
import { distinctUntilChanged, filter, map, mergeMap, take } from 'rxjs/operators';
import { merge } from 'rxjs';
import { MenuSection } from '@core/services/menu.models';
import { ActiveComponentService } from '@core/services/active-component.service';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';

@Component({
  selector: 'tb-router-tabs',
  templateUrl: './router-tabs.component.html',
  styleUrls: ['./router-tabs.component.scss']
})
export class RouterTabsComponent extends PageComponent implements OnInit {

  @ViewChild('tabsHeaderComponent', {static: true}) tabsHeaderComponentAnchor: TbAnchorComponent;

  tabsHeaderComponentRef: ComponentRef<any>;

  hideCurrentTabs = false;

  tabs$ = merge(this.menuService.menuSections(),
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd ),
      distinctUntilChanged())
  ).pipe(
    mergeMap(() => this.menuService.menuSections().pipe(take(1))),
    map((sections) => this.buildTabs(this.activatedRoute, sections))
  );

  constructor(protected store: Store<AppState>,
              private activatedRoute: ActivatedRoute,
              public router: Router,
              private menuService: MenuService,
              private activeComponentService: ActiveComponentService) {
    super(store);
  }

  ngOnInit() {
    this.activatedRoute.data.subscribe(
      (data) => this.buildTabsHeaderComponent(data)
    );
  }

  activeComponentChanged(activeComponent: any) {
    this.activeComponentService.setCurrentActiveComponent(activeComponent);
    let snapshot = this.router.routerState.snapshot.root;
    this.hideCurrentTabs = false;
    let found = false;
    while (snapshot.children.length) {
      if (snapshot.component && snapshot.component === RouterTabsComponent) {
        if (this.activatedRoute.snapshot === snapshot) {
          found = true;
        } else if (found) {
          this.hideCurrentTabs = true;
          break;
        }
      }
      snapshot = snapshot.children[0];
    }
  }

  private buildTabs(activatedRoute: ActivatedRoute, sections: MenuSection[]): Array<MenuSection> {
    const sectionPath = '/' + activatedRoute.pathFromRoot.map(r => r.snapshot.url)
      .filter(f => !!f[0]).map(f => f.map(f1 => f1.path).join('/')).join('/');
    const found = this.findRootSection(sections, sectionPath);
    if (found) {
      const rootPath = sectionPath.substring(0, sectionPath.length - found.path.length);
      const isRoot = rootPath === '';
      const tabs: Array<MenuSection> = found ? found.pages.filter(page => !page.disabled && (!page.rootOnly || isRoot)) : [];
      return tabs.map((tab) => ({...tab, path: rootPath + tab.path}));
    } else {
      return [];
    }
  }

  private findRootSection(sections: MenuSection[], sectionPath: string): MenuSection {
    for (const section of sections) {
      if (sectionPath.endsWith(section.path)) {
        return section;
      }
      if (section.pages?.length) {
        const found = this.findRootSection(section.pages, sectionPath);
        if (found) {
          return found;
        }
      }
    }
    return null;
  }

  private buildTabsHeaderComponent(snapshotData: any) {
    const viewContainerRef = this.tabsHeaderComponentAnchor.viewContainerRef;
    if (this.tabsHeaderComponentRef) {
      this.tabsHeaderComponentRef.destroy();
      this.tabsHeaderComponentRef = null;
      viewContainerRef.clear();
    }
    const tabsHeaderComponentType: Type<any> = snapshotData.routerTabsHeaderComponent;
    if (tabsHeaderComponentType) {
      this.tabsHeaderComponentRef = viewContainerRef.createComponent(tabsHeaderComponentType, {index: 0});
    }
  }

}
