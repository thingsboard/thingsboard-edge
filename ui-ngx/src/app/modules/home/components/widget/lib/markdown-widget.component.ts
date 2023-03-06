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

import { ChangeDetectorRef, Component, HostBinding, Inject, Input, OnInit, Type } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DatasourceData, FormattedData } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  createLabelFromPattern,
  flatDataWithoutOverride,
  formattedDataFormDatasourceData,
  hashCode, isDefinedAndNotNull,
  isNotEmptyStr,
  parseFunction,
  safeExecute
} from '@core/utils';
import cssjs from '@core/css/css';
import { UtilsService } from '@core/services/utils.service';
import { HOME_COMPONENTS_MODULE_TOKEN } from '@home/components/tokens';
import { EntityDataPageLink } from '@shared/models/query/query.models';

interface MarkdownWidgetSettings {
  markdownTextPattern: string;
  useMarkdownTextFunction: boolean;
  markdownTextFunction: string;
  markdownCss: string;
}

type MarkdownTextFunction = (data: FormattedData[], ctx: WidgetContext) => string;

@Component({
  selector: 'tb-markdown-widget ',
  templateUrl: './markdown-widget.component.html'
})
export class MarkdownWidgetComponent extends PageComponent implements OnInit {

  settings: MarkdownWidgetSettings;
  markdownTextFunction: MarkdownTextFunction;

  @HostBinding('class')
  markdownClass: string;

  @Input()
  ctx: WidgetContext;

  markdownText: string;


  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              @Inject(HOME_COMPONENTS_MODULE_TOKEN) public homeComponentsModule: Type<any>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.markdownWidget = this;
    this.settings = this.ctx.settings;
    this.markdownTextFunction = this.settings.useMarkdownTextFunction ?
      parseFunction(this.settings.markdownTextFunction, ['data', 'ctx']) : null;
    this.markdownClass = 'markdown-widget';
    const cssString = this.settings.markdownCss;
    if (isNotEmptyStr(cssString)) {
      const cssParser = new cssjs();
      cssParser.testMode = false;
      this.markdownClass += '-' + hashCode(cssString);
      cssParser.cssPreviewNamespace = 'tb-default .' + this.markdownClass;
      cssParser.createStyleElement(this.markdownClass, cssString);
    }
    const pageSize = isDefinedAndNotNull(this.ctx.widgetConfig.pageSize) &&
                      this.ctx.widgetConfig.pageSize > 0 ? this.ctx.widgetConfig.pageSize : 16384;
    const pageLink: EntityDataPageLink = {
      page: 0,
      pageSize,
      textSearch: null,
      dynamic: true
    };
    if (this.ctx.widgetConfig.datasources.length) {
      this.ctx.defaultSubscription.subscribeAllForPaginatedData(pageLink, null);
    } else {
      this.onDataUpdated();
    }
  }

  public onDataUpdated() {
    let initialData: DatasourceData[];
    if (this.ctx.data?.length) {
      initialData = this.ctx.data;
    } else if (this.ctx.datasources?.length) {
      initialData = [
        {
          datasource: this.ctx.datasources[0],
          dataKey: {
            type: DataKeyType.attribute,
            name: 'empty'
          },
          data: []
        }
      ];
    } else {
      initialData = [];
    }
    const data = formattedDataFormDatasourceData(initialData);
    let markdownText = this.settings.useMarkdownTextFunction ?
      safeExecute(this.markdownTextFunction, [data, this.ctx]) : this.settings.markdownTextPattern;
    const allData: FormattedData = flatDataWithoutOverride(data);
    markdownText = createLabelFromPattern(markdownText, allData);
    if (this.markdownText !== markdownText) {
      this.markdownText = this.utils.customTranslation(markdownText, markdownText);
      this.cd.detectChanges();
    }
  }

  markdownClick($event: MouseEvent) {
    this.ctx.actionsApi.elementClick($event);
  }

}
