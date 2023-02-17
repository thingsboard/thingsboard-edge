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

import { Observable } from 'rxjs/internal/Observable';
import { from, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';

let jsBeautifyModule: any;
let htmlBeautifyModule: any;
let cssBeautifyModule: any;

function loadJsBeautify(): Observable<any> {
  if (jsBeautifyModule) {
    return of(jsBeautifyModule);
  } else {
    return from(import('js-beautify/js/lib/beautify.js')).pipe(
      tap((module) => {
        jsBeautifyModule = module;
      })
    );
  }
}

function loadHtmlBeautify(): Observable<any> {
  if (htmlBeautifyModule) {
    return of(htmlBeautifyModule);
  } else {
    return from(import('js-beautify/js/lib/beautify-html.js')).pipe(
      tap((module) => {
        htmlBeautifyModule = module;
      })
    );
  }
}

function loadCssBeautify(): Observable<any> {
  if (cssBeautifyModule) {
    return of(cssBeautifyModule);
  } else {
    return from(import('js-beautify/js/lib/beautify-css.js')).pipe(
      tap((module) => {
        cssBeautifyModule = module;
      })
    );
  }
}

export function beautifyJs(source: string, options?: any): Observable<string> {
  return loadJsBeautify().pipe(
    map((mod) => {
      return mod.js_beautify(source, options);
    })
  );
}

export function beautifyCss(source: string, options?: any): Observable<string> {
  return loadCssBeautify().pipe(
    map((mod) => mod.css_beautify(source, options))
  );
}

export function beautifyHtml(source: string, options?: any): Observable<string> {
  return loadHtmlBeautify().pipe(
    map((mod) => mod.html_beautify(source, options))
  );
}
