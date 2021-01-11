///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, OnInit } from '@angular/core';
import { MenuService } from '@core/services/menu.service';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { HomeSection, HomeSectionPlace } from '@core/services/menu.models';
import { map } from 'rxjs/operators';

@Component({
  selector: 'tb-home-links',
  templateUrl: './home-links.component.html',
  styleUrls: ['./home-links.component.scss']
})
export class HomeLinksComponent implements OnInit {

  homeSections$ = this.menuService.homeSections();
  showHomeSections$ = this.homeSections$.pipe(
    map((sections) => {
      return sections.filter((section) => this.sectionPlaces(section).length > 0);
    })
  );

  cols = 2;

  constructor(private menuService: MenuService,
              public breakpointObserver: BreakpointObserver) {
  }

  ngOnInit() {
    this.updateColumnCount();
    this.breakpointObserver
      .observe([MediaBreakpoints.lg, MediaBreakpoints['gt-lg']])
      .subscribe((state: BreakpointState) => this.updateColumnCount());
  }

  private updateColumnCount() {
    this.cols = 2;
    if (this.breakpointObserver.isMatched(MediaBreakpoints.lg)) {
      this.cols = 3;
    }
    if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-lg'])) {
      this.cols = 4;
    }
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
