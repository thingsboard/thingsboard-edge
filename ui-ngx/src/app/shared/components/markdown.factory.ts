///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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


import { MarkedOptions, MarkedRenderer } from 'ngx-markdown';

export function markedOptionsFactory(): MarkedOptions {
  const renderer = new MarkedRenderer();
  const renderer2 = new MarkedRenderer();

  const copyCodeBlock = '{:copy-code}';

  let id = 1;

  renderer.code = (code: string, language: string | undefined, isEscaped: boolean) => {
    if (code.endsWith(copyCodeBlock)) {
      code = code.substring(0, code.length - copyCodeBlock.length);
      const content = renderer2.code(code, language, isEscaped);
      id++;
      return wrapCopyCode(id, content, code);
    } else {
      return renderer2.code(code, language, isEscaped);
    }
  };

  renderer.tablecell = (content: string, flags: {
    header: boolean;
    align: 'center' | 'left' | 'right' | null;
    }) => {
    if (content.endsWith(copyCodeBlock)) {
      content = content.substring(0, content.length - copyCodeBlock.length);
      id++;
      content = wrapCopyCode(id, content, content);
    }
    return renderer2.tablecell(content, flags);
  };

  return {
    renderer,
    headerIds: true,
    gfm: true,
    breaks: false,
    pedantic: false,
    smartLists: true,
    smartypants: false,
  };
}

function wrapCopyCode(id: number, content: string, code: string): string {
  return '<div class="code-wrapper">' + content + '<span id="copyCodeId' + id + '" style="display: none;">' + code + '</span>' +
  '<button id="copyCodeBtn' + id + '" onClick="markdownCopyCode(' + id + ')" ' +
  'class="clipboard-btn"><img src="https://clipboardjs.com/assets/images/clippy.svg" alt="Copy to clipboard">' +
  '</button></div>';
}

(window as any).markdownCopyCode = (id: number) => {
  const text = $('#copyCodeId' + id).text();
  navigator.clipboard.writeText(text).then(() => {
    import('tooltipster').then(
      () => {
        const copyBtn = $('#copyCodeBtn' + id);
        if (!copyBtn.hasClass('tooltipstered')) {
          copyBtn.tooltipster(
            {
              content: 'Copied!',
              theme: 'tooltipster-shadow',
              delay: 0,
              trigger: 'custom',
              triggerClose: {
                click: true,
                tap: true,
                scroll: true,
                mouseleave: true
              },
              side: 'bottom',
              distance: 12,
              trackOrigin: true
            }
          );
        }
        const tooltip = copyBtn.tooltipster('instance');
        tooltip.open();
      }
    );
  });
};
