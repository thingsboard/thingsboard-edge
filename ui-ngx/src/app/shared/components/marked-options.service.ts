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

import { MarkedOptions, MarkedRenderer } from 'ngx-markdown';
import { Inject, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DOCUMENT } from '@angular/common';
import { WINDOW } from '@core/services/window.service';
import { Tokenizer, marked } from 'marked';
import { Clipboard } from '@angular/cdk/clipboard';

const copyCodeBlock = '{:copy-code}';
const codeStyleRegex = '^{:code-style="(.*)"}\n';
const autoBlock = '{:auto}';
const targetBlankBlock = '{:target=&quot;_blank&quot;}';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class MarkedOptionsService extends MarkedOptions {

  renderer = new MarkedRenderer();
  headerIds = true;
  gfm = true;
  breaks = false;
  pedantic = false;
  smartLists = true;
  smartypants = false;
  mangle = false;

  private renderer2 = new MarkedRenderer();

  private id = 1;

  constructor(private translate: TranslateService,
              private clipboardService: Clipboard,
              @Inject(WINDOW) private readonly window: Window,
              @Inject(DOCUMENT) private readonly document: Document) {
    super();
    // @ts-ignore
    const tokenizer: Tokenizer = {
      autolink(src: string, mangle: (cap: string) => string): marked.Tokens.Link {
        if (src.endsWith(copyCodeBlock)) {
          return undefined;
        } else {
          // @ts-ignore
          return false;
        }
      },
      url(src: string, mangle: (cap: string) => string): marked.Tokens.Link {
        if (src.endsWith(copyCodeBlock)) {
          return undefined;
        } else {
          // @ts-ignore
          return false;
        }
      }
    };
    marked.use({tokenizer});
    this.renderer.code = (code: string, language: string | undefined, isEscaped: boolean) => {
      const codeContext = processCode(code);
      if (codeContext.copyCode) {
        const content = postProcessCodeContent(this.renderer2.code(codeContext.code, language, isEscaped), codeContext);
        this.id++;
        return this.wrapCopyCode(this.id, content, codeContext);
      } else {
        return this.wrapDiv(postProcessCodeContent(this.renderer2.code(codeContext.code, language, isEscaped), codeContext));
      }
    };
    this.renderer.table = (header: string, body: string) => {
      let autoLayout = false;
      if (header.includes(autoBlock)) {
        autoLayout = true;
        header = header.replace(autoBlock, '');
      }
      let table = this.renderer2.table(header, body);
      if (autoLayout) {
        table = table.replace('<table', '<table class="auto"');
      }
      return table;
    };
    this.renderer.tablecell = (content: string, flags: {
      header: boolean;
      align: 'center' | 'left' | 'right' | null;
    }) => {
      const codeContext = processCode(content);
      codeContext.multiline = false;
      if (codeContext.copyCode) {
        this.id++;
        content = this.wrapCopyCode(this.id, codeContext.code, codeContext);
      }
      return this.renderer2.tablecell(content, flags);
    };
    this.renderer.link = (href: string | null, title: string | null, text: string) => {
      if (text.endsWith(targetBlankBlock)) {
        text = text.substring(0, text.length - targetBlankBlock.length);
        const content = this.renderer2.link(href, title, text);
        return content.replace('<a href=', '<a target="_blank" href=');
      } else {
        return this.renderer2.link(href, title, text);
      }
    };
    this.document.addEventListener('selectionchange', this.onSelectionChange.bind(this));
    (this.window as any).markdownCopyCode = this.markdownCopyCode.bind(this);
  }

  private wrapDiv(content: string): string {
    return `<div>${content}</div>`;
  }

  private wrapCopyCode(id: number, content: string, context: CodeContext): string {
    let copyCodeButtonClass = 'clipboard-btn';
    if (context.multiline) {
      copyCodeButtonClass += ' multiline';
    }
    return `<div class="code-wrapper noChars" id="codeWrapper${id}" onClick="markdownCopyCode(${id})">${content}` +
      `<span id="copyCodeId${id}" style="display: none;">${encodeURIComponent(context.code)}</span>` +
      `<button class="${copyCodeButtonClass}">\n` +
      `    <p>${this.translate.instant('markdown.copy-code')}</p>\n` +
      `    <div>\n` +
      `       <img src="/assets/copy-code-icon.svg" alt="${this.translate.instant('markdown.copy-code')}">\n` +
      `    </div>\n` +
      `</button>` +
      `</div>`;
  }

  private onSelectionChange() {
    const codeWrappers = $('.code-wrapper');
    codeWrappers.removeClass('noChars');
    const selectedChars = this.getSelectedText();
    if (!selectedChars) {
      codeWrappers.addClass('noChars');
    }
  }

  private getSelectedText(): string {
    let text;
    if (this.window.getSelection) {
      text = this.window.getSelection().toString();
    } else if (this.document.getSelection) {
      text = this.document.getSelection();
    } else if ((this.document as any).selection) {
      text = (this.document as any).selection.createRange().text;
    }
    return text;
  }

  private markdownCopyCode(id: number) {
    const copyWrapper = $('#codeWrapper' + id);
    if (copyWrapper.hasClass('noChars')) {
      const text = decodeURIComponent($('#copyCodeId' + id).text());
      if (this.clipboardService.copy(text)) {
        import('tooltipster').then(
          () => {
            if (!copyWrapper.hasClass('tooltipstered')) {
              copyWrapper.tooltipster(
                {
                  content: this.translate.instant('markdown.copied'),
//              theme: 'tooltipster-shadow',
                  delay: 0,
                  trigger: 'custom',
                  triggerClose: {
                    click: true,
                    tap: true,
                    scroll: true,
                    mouseleave: true
                  },
                  side: 'top',
                  distance: 12,
                  trackOrigin: true
                }
              );
            }
            const tooltip = copyWrapper.tooltipster('instance');
            tooltip.open();
          });
      }
    }
  }
}

interface CodeContext {
  copyCode: boolean;
  multiline: boolean;
  codeStyle?: string;
  code: string;
}

function processCode(code: string): CodeContext {
  const context: CodeContext = {
    copyCode: false,
    multiline: false,
    code
  };
  if (context.code.endsWith(copyCodeBlock)) {
    context.code = context.code.substring(0, context.code.length - copyCodeBlock.length);
    context.copyCode = true;
  }
  const codeStyleMatch = context.code.match(new RegExp(codeStyleRegex));
  if (codeStyleMatch) {
    context.codeStyle = codeStyleMatch[1];
    context.code = context.code.replace(new RegExp(codeStyleRegex), '');
  }
  const lineCount = context.code.trim().split('\n').length;
  context.multiline = lineCount > 1;
  return context;
}

function postProcessCodeContent(content: string, context: CodeContext): string {
  let replacement = '<pre ngNonBindable';
  if (!context.multiline) {
    replacement += ' class="no-line-numbers"';
  }
  if (context.codeStyle) {
    replacement += ` style="${context.codeStyle}"`;
  }
  replacement += '>';
  return content.replace('<pre>', replacement);
}
