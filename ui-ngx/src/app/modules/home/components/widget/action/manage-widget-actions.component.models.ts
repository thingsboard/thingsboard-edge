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

import {
  CustomActionDescriptor,
  WidgetActionDescriptor,
  WidgetActionSource,
  widgetActionTypeTranslationMap
} from '@app/shared/models/widget.models';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { TranslateService } from '@ngx-translate/core';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount } from 'rxjs/operators';
import { UtilsService } from '@core/services/utils.service';
import { deepClone, isDefined, isUndefined } from '@core/utils';

import customSampleJs from '!raw-loader!./custom-sample-js.raw';
import customSampleCss from '!raw-loader!./custom-sample-css.raw';
import customSampleHtml from '!raw-loader!./custom-sample-html.raw';

export interface WidgetActionCallbacks {
  fetchDashboardStates: (query: string) => Array<string>;
}

export interface WidgetActionsData {
  actionsMap: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  actionSources: {[actionSourceId: string]: WidgetActionSource};
}

export interface WidgetActionDescriptorInfo extends WidgetActionDescriptor {
  actionSourceId?: string;
  actionSourceName?: string;
  typeName?: string;
}

export function toWidgetActionDescriptor(action: WidgetActionDescriptorInfo): WidgetActionDescriptor {
  const copy = deepClone(action);
  delete copy.actionSourceId;
  delete copy.actionSourceName;
  delete copy.typeName;
  return copy;
}

export function toCustomAction(action: WidgetActionDescriptorInfo): CustomActionDescriptor {
  let result: CustomActionDescriptor;
  if (!action || (isUndefined(action.customFunction) && isUndefined(action.customHtml) && isUndefined(action.customCss))) {
    result = {
      customHtml: customSampleHtml,
      customCss: customSampleCss,
      customFunction: customSampleJs
    };
  } else {
    result = {
      customHtml: action.customHtml,
      customCss: action.customCss,
      customFunction: action.customFunction
    };
  }
  result.customResources = action && isDefined(action.customResources) ? deepClone(action.customResources) : [];
  return result;
}

export class WidgetActionsDatasource implements DataSource<WidgetActionDescriptorInfo> {

  private actionsSubject = new BehaviorSubject<WidgetActionDescriptorInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<WidgetActionDescriptorInfo>>(emptyPageData<WidgetActionDescriptorInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  private allActions: Observable<Array<WidgetActionDescriptorInfo>>;

  private actionsMap: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  private actionSources: {[actionSourceId: string]: WidgetActionSource};

  constructor(private translate: TranslateService,
              private utils: UtilsService) {}

  connect(collectionViewer: CollectionViewer): Observable<WidgetActionDescriptorInfo[] | ReadonlyArray<WidgetActionDescriptorInfo>> {
    return this.actionsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.actionsSubject.complete();
    this.pageDataSubject.complete();
  }

  setActions(actionsData: WidgetActionsData) {
    this.actionsMap = actionsData.actionsMap;
    this.actionSources = actionsData.actionSources;
  }

  loadActions(pageLink: PageLink, reload: boolean = false): Observable<PageData<WidgetActionDescriptorInfo>> {
    if (reload) {
      this.allActions = null;
    }
    const result = new ReplaySubject<PageData<WidgetActionDescriptorInfo>>();
    this.fetchActions(pageLink).pipe(
      catchError(() => of(emptyPageData<WidgetActionDescriptorInfo>())),
    ).subscribe(
      (pageData) => {
        this.actionsSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
      }
    );
    return result;
  }

  fetchActions(pageLink: PageLink): Observable<PageData<WidgetActionDescriptorInfo>> {
    return this.getAllActions().pipe(
      map((data) => pageLink.filterData(data))
    );
  }

  getAllActions(): Observable<Array<WidgetActionDescriptorInfo>> {
    if (!this.allActions) {
      const actions: WidgetActionDescriptorInfo[] = [];
      for (const actionSourceId of Object.keys(this.actionsMap)) {
        const descriptors = this.actionsMap[actionSourceId];
        descriptors.forEach((descriptor) => {
          actions.push(this.toWidgetActionDescriptorInfo(actionSourceId, descriptor));
        });
      }
      this.allActions = of(actions).pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.allActions;
  }

  private toWidgetActionDescriptorInfo(actionSourceId: string, action: WidgetActionDescriptor): WidgetActionDescriptorInfo {
    const actionSource = this.actionSources[actionSourceId];
    const actionSourceName = actionSource ? this.utils.customTranslation(actionSource.name, actionSource.name) : actionSourceId;
    const typeName = this.translate.instant(widgetActionTypeTranslationMap.get(action.type));
    return { actionSourceId, actionSourceName, typeName, ...action};
  }

  isEmpty(): Observable<boolean> {
    return this.actionsSubject.pipe(
      map((actions) => !actions.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

}
