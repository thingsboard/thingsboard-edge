///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { getCurrentOpenedMenuSections, selectAuth, selectIsAuthenticated } from '../auth/auth.selectors';
import { filter, map, take } from 'rxjs/operators';
import {
  buildUserHome,
  buildUserMenu,
  filterOpenedMenuSection,
  HomeSection,
  MenuSection
} from '@core/services/menu.models';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { ActivationEnd, NavigationEnd, Params, Router } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { AuthState } from '@core/auth/auth.models';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  private currentMenuSections: Array<MenuSection>;
  private menuSections$: Subject<Array<MenuSection>> = new ReplaySubject<Array<MenuSection>>(1);
  private homeSections$: Subject<Array<HomeSection>> = new ReplaySubject<Array<HomeSection>>(1);
  private availableMenuSections$: Subject<Array<MenuSection>> = new ReplaySubject<Array<MenuSection>>(1);
  private availableMenuLinks$ = this.menuSections$.pipe(
    map((items) => this.allMenuLinks(items))
  );

  private currentCustomSection: MenuSection = null;
  private currentCustomChildSection: MenuSection = null;

  constructor(private store: Store<AppState>,
              private router: Router,
              private customMenuService: CustomMenuService,
              private userPermissionsService: UserPermissionsService) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      (authenticated: boolean) => {
        if (authenticated) {
          this.buildMenu();
        }
      }
    );
    this.customMenuService.customMenuConfigChanged$.subscribe(() => {
      this.buildMenu();
    });
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(
      () => {
        this.updateOpenedMenuSections();
      }
    );
    this.router.events.pipe(filter(event => event instanceof ActivationEnd)).subscribe(() => {
      this.updateCurrentCustomSection();
    });
  }

  private buildMenu() {
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        if (authState.authUser) {
          const customMenu = this.customMenuService.getCustomMenu();
          this.currentMenuSections = buildUserMenu(authState, this.userPermissionsService, customMenu);
          this.updateCurrentCustomSection();
          this.updateOpenedMenuSections();
          this.menuSections$.next(this.currentMenuSections);
          const availableMenuSections = this.allMenuSections(this.currentMenuSections);
          this.availableMenuSections$.next(availableMenuSections);
          const homeSections = buildUserHome(authState, availableMenuSections);
          this.homeSections$.next(homeSections);
        }
      }
    );
  }

  private updateOpenedMenuSections() {
    const url = this.router.url;
    const openedMenuSections = getCurrentOpenedMenuSections(this.store);
    if (this.currentMenuSections?.length) {
      this.currentMenuSections.filter(section => filterOpenedMenuSection(section, url, openedMenuSections)).forEach(
        section => section.opened = true
      );
    }
  }

  private allMenuLinks(sections: Array<MenuSection>): Array<MenuSection> {
    const result: Array<MenuSection> = [];
    for (const section of sections) {
      if (section.type === 'link') {
        result.push(section);
      }
      if (section.pages && section.pages.length) {
        result.push(...this.allMenuLinks(section.pages));
      }
    }
    return result;
  }

  private allMenuSections(sections: Array<MenuSection>): Array<MenuSection> {
    const result: Array<MenuSection> = [];
    for (const section of sections) {
      result.push(section);
      if (section.pages && section.pages.length) {
        result.push(...this.allMenuSections(section.pages));
      }
    }
    return result;
  }

  public menuSections(): Observable<Array<MenuSection>> {
    return this.menuSections$;
  }

  public homeSections(): Observable<Array<HomeSection>> {
    return this.homeSections$;
  }

  public getCurrentCustomSection(): MenuSection {
    return this.currentCustomSection;
  }

  public getCurrentCustomChildSection(): MenuSection {
    return this.currentCustomChildSection;
  }

  private updateCurrentCustomSection() {
    const queryParams = this.extractQueryParams();
    this.currentCustomSection = this.detectCurrentCustomSection(queryParams);
    this.currentCustomChildSection = this.detectCurrentCustomChildSection(queryParams);
  }

  private detectCurrentCustomSection(queryParams: Params): MenuSection {
    if (queryParams && queryParams.stateId) {
      const stateId: string = queryParams.stateId;
      const found =
        this.currentMenuSections.find((section) => section.isCustom && section.stateId === stateId);
      if (found) {
        return found;
      }
    }
    return null;
  }

  private detectCurrentCustomChildSection(queryParams: Params): MenuSection {
    if (queryParams && queryParams.childStateId) {
      const stateId = queryParams.childStateId;
      for (const section of this.currentMenuSections) {
        if (section.isCustom && section.pages && section.pages.length) {
          const found =
            section.pages.find((childSection) => childSection.stateId === stateId);
          if (found) {
            return found;
          }
        }
      }
    }
    return null;
  }

  private extractQueryParams(): Params {
    const state = this.router.routerState;
    const snapshot =  state.snapshot;
    let lastChild = snapshot.root;
    while (lastChild.children.length) {
      lastChild = lastChild.children[0];
    }
    return lastChild.queryParams;
  }

  public getRedirectPath(parentPath: string, redirectPath: string): Observable<string> {
    parentPath = '/' + parentPath.replace(/\./g, '/');
    if (!redirectPath.startsWith('/')) {
      redirectPath = `${parentPath}/${redirectPath}`;
    }
    return this.menuSections$.pipe(
      map((sections) => {
        const parentSection = this.findSectionByPath(sections, parentPath);
        if (parentSection) {
          if (parentSection.pages) {
            const childPages = parentSection.pages;
            if (childPages && childPages.length) {
              const redirectPage = childPages.filter((page) => page.path === redirectPath);
              if (!redirectPage || !redirectPage.length) {
                return childPages[0].path;
              }
            }
            return redirectPath;
          }
        }
        return redirectPath;
      })
    );
  }

  private findSectionByPath(sections: MenuSection[], sectionPath: string): MenuSection {
    for (const section of sections) {
      if (sectionPath === section.path) {
        return section;
      }
      if (section.pages?.length) {
        const found = this.findSectionByPath(section.pages, sectionPath);
        if (found) {
          return found;
        }
      }
    }
    return null;
  }

  public availableMenuLinks(): Observable<Array<MenuSection>> {
    return this.availableMenuLinks$;
  }

  public availableMenuSections(): Observable<Array<MenuSection>> {
    return this.availableMenuSections$;
  }

  public menuLinkById(id: string): Observable<MenuSection | undefined> {
    return this.availableMenuLinks$.pipe(
      map((links) => links.find(link => link.id === id))
    );
  }

  public menuLinksByIds(ids: string[]): Observable<Array<MenuSection>> {
    return this.availableMenuLinks$.pipe(
      map((links) => links.filter(link => ids.includes(link.id)).sort((a, b) => {
        const i1 = ids.indexOf(a.id);
        const i2 = ids.indexOf(b.id);
        return i1 - i2;
      }))
    );
  }
}
