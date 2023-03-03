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
  MESSAGE_FORMAT_CONFIG,
  MessageFormatConfig,
  TranslateMessageFormatCompiler
} from 'ngx-translate-messageformat-compiler';
import { Inject, Injectable, Optional } from '@angular/core';
import { parse } from '@messageformat/parser';

@Injectable({ providedIn: 'root' })
export class TranslateDefaultCompiler extends TranslateMessageFormatCompiler {

  constructor(
    @Optional()
    @Inject(MESSAGE_FORMAT_CONFIG)
      config?: MessageFormatConfig
  ) {
    super(config);
  }

  public compile(value: string, lang: string): (params: any) => string {
    return this.defaultCompile(value, lang);
  }

  public compileTranslations(translations: any, lang: string): any {
    return this.defaultCompile(translations, lang);
  }

  private defaultCompile(src: any, lang: string): any {
    if (typeof src !== 'object') {
      if (this.checkIsPlural(src)) {
        try {
          return super.compile(src, lang);
        } catch (e) {
          console.warn('Failed compile translate:', src, e);
          return src;
        }
      } else {
        return src;
      }
    } else {
      const result = {};
      for (const key of Object.keys(src)) {
        result[key] = this.defaultCompile(src[key], lang);
      }
      return result;
    }
  }

  private checkIsPlural(src: string): boolean {
    let tokens: any[];
    try {
      tokens = parse(src.replace(/\{\{/g, '{').replace(/\}\}/g, '}'),
        {cardinal: [], ordinal: []});
    } catch (e) {
      console.warn(`Failed to parse source: ${src}`);
      console.error(e);
      return false;
    }
    const res = tokens.filter(
      (value) => typeof value !== 'string' && value.type === 'plural'
    );
    return res.length > 0;
  }

}
