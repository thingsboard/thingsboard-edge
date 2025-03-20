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

import { ChangeDetectorRef, Component, Inject, Input, OnInit, Type } from '@angular/core';
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
  hashCode,
  isDefinedAndNotNull,
  isNotEmptyStr,
  parseTbFunction,
  safeExecuteTbFunction
} from '@core/utils';
import cssjs from '@core/css/css';
import { UtilsService } from '@core/services/utils.service';
import { HOME_COMPONENTS_MODULE_TOKEN, WIDGET_COMPONENTS_MODULE_TOKEN } from '@home/components/tokens';
import { EntityDataPageLink } from '@shared/models/query/query.models';
import { CompiledTbFunction, TbFunction } from '@shared/models/js-function.models';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

interface MarkdownWidgetSettings {
  markdownTextPattern: string;
  useMarkdownTextFunction: boolean;
  markdownTextFunction: TbFunction;
  applyDefaultMarkdownStyle: boolean;
  markdownCss: string;
}

type MarkdownTextFunction = (data: FormattedData[], ctx: WidgetContext) => string;

@Component({
  selector: 'tb-markdown-widget',
  templateUrl: './markdown-widget.component.html'
})
export class MarkdownWidgetComponent extends PageComponent implements OnInit {

  settings: MarkdownWidgetSettings;
  markdownTextFunction: Observable<CompiledTbFunction<MarkdownTextFunction>>;

  markdownClass: string;

  @Input()
  ctx: WidgetContext;

  data: FormattedData[];

  markdownText: string;

  additionalStyles: string[];

  applyDefaultMarkdownStyle = true;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              @Inject(HOME_COMPONENTS_MODULE_TOKEN) public homeComponentsModule: Type<any>,
              @Inject(WIDGET_COMPONENTS_MODULE_TOKEN) public widgetComponentsModule: Type<any>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.markdownWidget = this;
    this.settings = this.ctx.settings;
    this.markdownTextFunction = this.settings.useMarkdownTextFunction ?
      parseTbFunction(this.ctx.http, this.settings.markdownTextFunction, ['data', 'ctx']) : of(null);
    let cssString = this.settings.markdownCss;
    if (isNotEmptyStr(cssString)) {
      const cssParser = new cssjs();
      this.markdownClass = 'markdown-widget-' + hashCode(cssString);
      cssParser.cssPreviewNamespace = this.markdownClass;
      cssParser.testMode = false;
      cssString = cssParser.applyNamespacing(cssString);
      cssString = cssParser.getCSSForEditor(cssString);
      this.additionalStyles = [cssString];
    }
    if (isDefinedAndNotNull(this.settings.applyDefaultMarkdownStyle)) {
      this.applyDefaultMarkdownStyle = this.settings.applyDefaultMarkdownStyle;
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
    this.data = formattedDataFormDatasourceData(initialData);

    const markdownText = this.settings.useMarkdownTextFunction ?
      this.markdownTextFunction.pipe(map(markdownTextFunction => safeExecuteTbFunction(markdownTextFunction, [this.data, this.ctx]))) : this.settings.markdownTextPattern;
    if (typeof markdownText === 'string') {
      this.updateMarkdownText(markdownText, this.data);
    } else {
      markdownText.subscribe((text) => {
        this.updateMarkdownText(text, this.data);
      });
    }
  }

  private updateMarkdownText(markdownText: string, data: FormattedData[]) {
    const allData: FormattedData = flatDataWithoutOverride(data);
    markdownText = createLabelFromPattern(markdownText, allData);
    if (this.markdownText !== markdownText) {
      this.markdownText = this.utils.customTranslation(markdownText, markdownText);
    }
    this.cd.markForCheck();
  }

  markdownClick($event: MouseEvent) {
    this.ctx.actionsApi.elementClick($event);
  }

}
