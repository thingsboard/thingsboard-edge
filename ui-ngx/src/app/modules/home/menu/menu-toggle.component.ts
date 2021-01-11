///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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

import { Component, Input, OnInit } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';
import { Observable, combineLatest } from 'rxjs';
import { distinctUntilChanged, filter, map, share, startWith } from 'rxjs/operators';
import { MenuService } from '@core/services/menu.service';
import { UtilsService } from '@core/services/utils.service';
import { ActivationEnd, Router } from '@angular/router';

@Component({
  selector: 'tb-menu-toggle',
  templateUrl: './menu-toggle.component.html',
  styleUrls: ['./menu-toggle.component.scss']
})
export class MenuToggleComponent implements OnInit {

  @Input() section: MenuSection;

  sectionPages$: Observable<Array<MenuSection>>;
  sectionHeight$: Observable<string>;

  constructor(public utils: UtilsService,
              private menuService: MenuService,
              private router: Router) {
  }

  ngOnInit() {
    this.sectionPages$ = this.section.asyncPages.pipe(
      map((pages) => {
          return pages.filter((page) => !page.disabled);
        }
      ),
      share()
    );

    this.sectionHeight$ = combineLatest([
      this.sectionPages$,
      this.router.events.pipe(filter(event => event instanceof ActivationEnd), startWith(ActivationEnd))
    ]).pipe(
      map((pages) => {
        if (this.sectionActive()) {
          return pages[0].length * 40 + 'px';
        }
        return '0px';
      }),
      distinctUntilChanged(),
      share()
    );
  }

  sectionActive(): boolean {
    return  this.menuService.sectionActive(this.section);
  }

  trackBySectionPages(index: number, section: MenuSection){
    return section.id;
  }
}
