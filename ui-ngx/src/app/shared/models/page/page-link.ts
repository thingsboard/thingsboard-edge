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

import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { getDescendantProp, isObject } from '@core/utils';
import { SortDirection } from '@angular/material/sort';

export const MAX_SAFE_PAGE_SIZE = 2147483647;

export type PageLinkSearchFunction<T> = (entity: T, textSearch: string) => boolean;

const defaultPageLinkSearchFunction: PageLinkSearchFunction<any> =
  (entity: any, textSearch: string) => {
    if (textSearch === null || !textSearch.length) {
      return true;
    }
    const expected = ('' + textSearch).toLowerCase();
    for (const key of Object.keys(entity)) {
      const val = entity[key];
      if (val !== null) {
        if (val !== Object(val)) {
          const actual = ('' + val).toLowerCase();
          if (actual.indexOf(expected) !== -1) {
            return true;
          }
        } else if (isObject(val)) {
          if (defaultPageLinkSearchFunction(val, textSearch)) {
            return true;
          }
        }
      }
    }
    return false;
  };

export class PageLink {

  textSearch: string;
  pageSize: number;
  page: number;
  sortOrder: SortOrder;

  constructor(pageSize: number, page: number = 0, textSearch: string = null, sortOrder: SortOrder = null) {
    this.textSearch = textSearch;
    this.pageSize = pageSize;
    this.page = page;
    this.sortOrder = sortOrder;
  }

  public nextPageLink(): PageLink {
    return new PageLink(this.pageSize, this.page + 1, this.textSearch, this.sortOrder);
  }

  public toQuery(): string {
    let query = `?pageSize=${this.pageSize}&page=${this.page}`;
    if (this.textSearch && this.textSearch.length) {
      query += `&textSearch=${this.textSearch}`;
    }
    if (this.sortOrder) {
      query += `&sortProperty=${this.sortOrder.property}&sortOrder=${this.sortOrder.direction}`;
    }
    return query;
  }

  public sort(item1: any, item2: any): number {
    if (this.sortOrder) {
      const property = this.sortOrder.property;
      const item1Value = getDescendantProp(item1, property);
      const item2Value = getDescendantProp(item2, property);
      let result = 0;
      if (item1Value !== item2Value) {
        if (typeof item1Value === 'number' && typeof item2Value === 'number') {
          result = item1Value - item2Value;
        } else if (typeof item1Value === 'string' && typeof item2Value === 'string') {
          result = item1Value.localeCompare(item2Value);
        } else if (typeof item1Value === 'boolean' && typeof item2Value === 'boolean') {
          if (item1Value && !item2Value) {
            result = 1;
          } else if (!item1Value && item2Value) {
            result = -1;
          }
        } else if (typeof item1Value !== typeof item2Value) {
          result = 1;
        }
      }
      return this.sortOrder.direction === Direction.ASC ? result : result * -1;
    }
    return 0;
  }

  public filterData<T>(data: Array<T>,
                       searchFunction: PageLinkSearchFunction<T> = defaultPageLinkSearchFunction): PageData<T> {
    const pageData = emptyPageData<T>();
    pageData.data = [...data];
    if (this.textSearch && this.textSearch.length) {
      pageData.data = pageData.data.filter((entity) => searchFunction(entity, this.textSearch));
    }
    pageData.totalElements = pageData.data.length;
    pageData.totalPages = this.pageSize === Number.POSITIVE_INFINITY ? 1 : Math.ceil(pageData.totalElements / this.pageSize);
    if (this.sortOrder) {
      pageData.data = pageData.data.sort((a, b) => this.sort(a, b));
    }
    if (this.pageSize !== Number.POSITIVE_INFINITY) {
      const startIndex = this.pageSize * this.page;
      pageData.data = pageData.data.slice(startIndex, startIndex + this.pageSize);
      pageData.hasNext = pageData.totalElements > startIndex + pageData.data.length;
    }
    return pageData;
  }

  public sortDirection(): SortDirection {
    if (this.sortOrder) {
      return (this.sortOrder.direction + '').toLowerCase() as SortDirection;
    } else {
      return '' as SortDirection;
    }
  }

}

export class TimePageLink extends PageLink {

  startTime: number;
  endTime: number;

  constructor(pageSize: number, page: number = 0, textSearch: string = null, sortOrder: SortOrder = null,
              startTime: number = null, endTime: number = null) {
    super(pageSize, page, textSearch, sortOrder);
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public nextPageLink(): TimePageLink {
    return new TimePageLink(this.pageSize, this.page + 1, this.textSearch, this.sortOrder, this.startTime, this.endTime);
  }

  public toQuery(): string {
    let query = super.toQuery();
    if (this.startTime) {
      query += `&startTime=${this.startTime}`;
    }
    if (this.endTime) {
      query += `&endTime=${this.endTime}`;
    }
    return query;
  }
}
