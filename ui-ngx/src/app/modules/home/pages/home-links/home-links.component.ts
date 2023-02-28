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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { MenuService } from '@core/services/menu.service';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { HomeSection, HomeSectionPlace } from '@core/services/menu.models';
import { map } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { HomeDashboard } from '@shared/models/dashboard.models';

@Component({
  selector: 'tb-home-links',
  templateUrl: './home-links.component.html',
  styleUrls: ['./home-links.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeLinksComponent implements OnInit {

  homeSections$ = this.menuService.homeSections();
  showHomeSections$ = this.homeSections$.pipe(
    map((sections) => {
      return sections.filter((section) => this.sectionPlaces(section).length > 0);
    })
  );

  cols = 2;

  homeDashboard: HomeDashboard = this.route.snapshot.data.homeDashboard;

  constructor(private menuService: MenuService,
              public breakpointObserver: BreakpointObserver,
              private cd: ChangeDetectorRef,
              private route: ActivatedRoute) {
  }

  ngOnInit() {
    if (!this.homeDashboard) {
      this.updateColumnCount();
      this.breakpointObserver
        .observe([MediaBreakpoints.lg, MediaBreakpoints['gt-lg']])
        .subscribe((state: BreakpointState) => this.updateColumnCount());
    }
  }

  private updateColumnCount() {
    this.cols = 2;
    if (this.breakpointObserver.isMatched(MediaBreakpoints.lg)) {
      this.cols = 3;
    }
    if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-lg'])) {
      this.cols = 4;
    }
    this.cd.detectChanges();
  }

  sectionPlaces(section: HomeSection): HomeSectionPlace[] {
    return section && section.places ? section.places.filter((place) => !place.disabled) : [];
  }

  sectionColspan(section: HomeSection): number {
    if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm'])) {
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
