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
import { Component, Input, NgZone, OnInit } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MenuService } from '@core/services/menu.service';
import { HomeSection, HomeSectionPlace } from '@core/services/menu.models';
import { Router } from '@angular/router';
import { map } from 'rxjs/operators';

interface NavigationCardsWidgetSettings {
  filterType: 'all' | 'include' | 'exclude';
  filter: string[];
}

@Component({
  selector: 'tb-navigation-cards-widget',
  templateUrl: './navigation-cards-widget.component.html',
  styleUrls: ['./navigation-cards-widget.component.scss']
})
export class NavigationCardsWidgetComponent extends PageComponent implements OnInit {

  homeSections$ = this.menuService.homeSections();
  showHomeSections$ = this.homeSections$.pipe(
    map((sections) => {
      return sections.filter((section) => this.sectionPlaces(section).length > 0);
    })
  );

  cols = null;

  settings: NavigationCardsWidgetSettings;

  @Input()
  ctx: WidgetContext;

  constructor(protected store: Store<AppState>,
              private menuService: MenuService,
              private ngZone: NgZone,
              private router: Router) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.navigationCardsWidget = this;
    this.settings = this.ctx.settings;
  }

  resize() {
    this.updateColumnCount();
  }

  private updateColumnCount() {
    this.cols = 2;
    const width = this.ctx.width;
    if (width >= 1280) {
      this.cols = 3;
      if (width >= 1920) {
        this.cols = 4;
      }
    }
    this.ctx.detectChanges();
  }

  navigate($event: Event, path: string) {
    $event.preventDefault();
    this.ngZone.run(() => {
      this.router.navigateByUrl(path);
    });
  }

  sectionPlaces(section: HomeSection): HomeSectionPlace[] {
    return section && section.places ? section.places.filter((place) => this.filterPlace(place)) : [];
  }

  private filterPlace(place: HomeSectionPlace): boolean {
    if (place.disabled) {
      return false;
    } else if (this.settings.filterType === 'include') {
      return this.settings.filter.includes(place.path);
    } else if (this.settings.filterType === 'exclude') {
      return !this.settings.filter.includes(place.path);
    }
    return true;
  }

  sectionColspan(section: HomeSection): number {
    if (this.ctx.width >= 960) {
      let colspan = this.cols;
      const places = this.sectionPlaces(section);
      if (places.length <= colspan) {
        colspan = places.length;
      }
      return colspan;
    } else {
      return 2;
    }
  }

}
