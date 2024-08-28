///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, UntypedFormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { getAce, Range, TbHighlightRule } from '@shared/models/ace/ace.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import { deepClone, guid, isUndefined } from '@app/core/utils';
import { TranslateService } from '@ngx-translate/core';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { ResizeObserver } from '@juggle/resize-observer';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { beautifyJs } from '@shared/models/beautify.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-js-func',
  templateUrl: './js-func.component.html',
  styleUrls: ['./js-func.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => JsFuncComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => JsFuncComponent),
      multi: true,
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class JsFuncComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  @ViewChild('javascriptEditor', {static: true})
  javascriptEditorElmRef: ElementRef;

  private jsEditor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;
  private ignoreChange = false;

  toastTargetId = `jsFuncEditor-${guid()}`;

  @Input() functionTitle: string;

  @Input() functionName: string;

  @Input() functionArgs: Array<string>;

  @Input() validationArgs: Array<any>;

  @Input() resultType: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() minHeight = '200px';

  @Input() editorCompleter: TbEditorCompleter;

  @Input() propertyHighlightRules: TbHighlightRule[];

  @Input() objectHighlightRules: TbHighlightRule[];

  @Input() globalVariables: Array<string>;

  @Input() disableUndefinedCheck = false;

  @Input() helpId: string;

  @Input() scriptLanguage: ScriptLanguage = ScriptLanguage.JS;

  @Input()
  @coerceBoolean()
  hideBrackets = false;

  private noValidateValue: boolean;
  get noValidate(): boolean {
    return this.noValidateValue;
  }
  @Input()
  set noValidate(value: boolean) {
    this.noValidateValue = coerceBooleanProperty(value);
  }

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  functionLabel: string;

  fullscreen = false;

  modelValue: string;

  functionValid = true;

  validationError: string;

  errorShowed = false;

  errorMarkers: number[] = [];
  errorAnnotationId = -1;

  private functionArgsString = '';

  private propagateChange = null;
  public hasErrors = false;

  constructor(public elementRef: ElementRef,
              private utils: UtilsService,
              private translate: TranslateService,
              protected store: Store<AppState>,
              private raf: RafService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    if (this.functionTitle) {
      this.hideBrackets = true;
    }
    if (!this.resultType || this.resultType.length === 0) {
      this.resultType = 'nocheck';
    }
    if (this.functionArgs) {
      this.functionArgs.forEach((functionArg) => {
        if (this.functionArgsString.length > 0) {
          this.functionArgsString += ', ';
        }
        this.functionArgsString += functionArg;
      });
    }
    if (this.functionTitle) {
      this.functionLabel = `${this.functionTitle}: f(${this.functionArgsString})`;
    } else {
      this.functionLabel =
        `function ${this.functionName ? this.functionName : ''}(${this.functionArgsString})${this.hideBrackets ? '' : ' {'}`;
    }
    const editorElement = this.javascriptEditorElmRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
        mode: 'ace/mode/javascript',
        showGutter: true,
        showPrintMargin: true,
        readOnly: this.disabled
    };
    if (ScriptLanguage.TBEL === this.scriptLanguage) {
      editorOptions.mode = 'ace/mode/tbel';
    }

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    getAce().subscribe(
      (ace) => {
        this.jsEditor = ace.edit(editorElement, editorOptions);
        this.jsEditor.session.setUseWrapMode(true);
        this.jsEditor.setValue(this.modelValue ? this.modelValue : '', -1);
        this.jsEditor.setReadOnly(this.disabled);
        this.jsEditor.on('change', () => {
          if (!this.ignoreChange) {
            this.cleanupJsErrors();
            this.updateView();
          }
        });
        if (!this.disableUndefinedCheck) {
          // @ts-ignore
          this.jsEditor.session.on('changeAnnotation', () => {
            const annotations = this.jsEditor.session.getAnnotations();
            annotations.filter(annotation => annotation.text.includes('is not defined')).forEach(annotation => {
              annotation.type = 'error';
            });
            this.jsEditor.renderer.setAnnotations(annotations);
            const hasErrors = annotations.filter(annotation => annotation.type === 'error').length > 0;
            if (this.hasErrors !== hasErrors) {
              this.hasErrors = hasErrors;
              this.propagateChange(this.modelValue);
              this.cd.markForCheck();
            }
          });
        }
        // @ts-ignore
        if ((this.propertyHighlightRules?.length || this.objectHighlightRules?.length) && !!this.jsEditor.session.$mode) {
          // @ts-ignore
          const newMode = new this.jsEditor.session.$mode.constructor();
          newMode.$highlightRules = new newMode.HighlightRules();
          if (this.propertyHighlightRules?.length) {
            const propertiesRules: { token: string; regex: RegExp }[] = newMode.$highlightRules.$rules.property;
            const index = propertiesRules.findIndex(p => p.token === 'support.constant');
            const additionalPropertyRules: { token: string; regex: RegExp }[] = this.propertyHighlightRules.map(r => ({
              token: `tb.${r.class}`,
              regex: r.regex
            }));
            propertiesRules.splice(index, 0, ...additionalPropertyRules);
          }
          if (this.objectHighlightRules?.length) {
            const noRegexRules: { token: string; regex: RegExp }[] = newMode.$highlightRules.$rules.no_regex;
            const index = noRegexRules.findIndex(p => Array.isArray(p.token) && p.token[0] === 'support.constant');
            const additionalNoRegexRules: { token: string; regex: RegExp }[] = this.objectHighlightRules.map(r => ({
              token: `tb.${r.class}`,
              regex: r.regex
            }));
            noRegexRules.splice(index, 0, ...additionalNoRegexRules);
          }
          // @ts-ignore
          this.jsEditor.session.$onChangeMode(newMode);
        }
        // @ts-ignore
        if (!!this.jsEditor.session.$worker) {
          const jsWorkerOptions = {
            undef: !this.disableUndefinedCheck,
            unused: true,
            globals: {}
          };
          if (!this.disableUndefinedCheck && this.functionArgs) {
            this.functionArgs.forEach(arg => {
              jsWorkerOptions.globals[arg] = false;
            });
          }
          if (!this.disableUndefinedCheck && this.globalVariables) {
            this.globalVariables.forEach(arg => {
              jsWorkerOptions.globals[arg] = false;
            });
          }
          // @ts-ignore
          this.jsEditor.session.$worker.send('changeOptions', [jsWorkerOptions]);
        }
        if (this.editorCompleter) {
          this.jsEditor.completers = [this.editorCompleter, ...(this.jsEditor.completers || [])];
        }
        this.editorResize$ = new ResizeObserver(() => {
          this.onAceEditorResize();
        });
        this.editorResize$.observe(editorElement);
      }
    );
  }

  ngOnDestroy(): void {
    if (this.editorResize$) {
      this.editorResize$.disconnect();
    }
    if (this.jsEditor) {
      this.jsEditor.destroy();
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.jsEditor.resize();
      this.jsEditor.renderer.updateFull();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.jsEditor) {
      this.jsEditor.setReadOnly(this.disabled);
    }
  }

  public validate(c: UntypedFormControl) {
    return (this.functionValid && !this.hasErrors) ? null : {
      jsFunc: {
        valid: false,
      },
    };
  }

  beautifyJs() {
    beautifyJs(this.modelValue, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        this.jsEditor.setValue(res ? res : '', -1);
        this.updateView();
      }
    );
  }

  validateOnSubmit(): void {
    if (!this.disabled) {
      this.cleanupJsErrors();
      this.functionValid = this.validateJsFunc();
      if (!this.functionValid) {
        this.propagateChange(this.modelValue);
        this.cd.markForCheck();
        this.store.dispatch(new ActionNotificationShow(
          {
            message: this.validationError,
            type: 'error',
            target: this.toastTargetId,
            verticalPosition: 'bottom',
            horizontalPosition: 'left'
          }));
        this.errorShowed = true;
      }
    }
  }

  public focus() {
    this.javascriptEditorElmRef.nativeElement.scrollIntoView();
    this.jsEditor?.focus();
  }

  private validateJsFunc(): boolean {
    try {
      const toValidate = new Function(this.functionArgsString, this.modelValue);
      if (this.noValidate) {
        return true;
      }
      if (this.validationArgs) {
        let res: any;
        let validationError: any;
        for (const validationArg of this.validationArgs) {
          try {
            res = toValidate.apply(this, validationArg);
            validationError = null;
            break;
          } catch (e) {
            validationError = e;
          }
        }
        if (validationError) {
          throw validationError;
        }
        if (this.resultType !== 'nocheck') {
          if (this.resultType === 'any') {
            if (isUndefined(res)) {
              this.validationError = this.translate.instant('js-func.no-return-error');
              return false;
            }
          } else {
            const resType = typeof res;
            if (resType !== this.resultType) {
              this.validationError = this.translate.instant('js-func.return-type-mismatch', {type: this.resultType});
              return false;
            }
          }
        }
        return true;
      } else {
        return true;
      }
    } catch (e) {
      const details = this.utils.parseException(e);
      let errorInfo = 'Error:';
      if (details.name) {
        errorInfo += ' ' + details.name + ':';
      }
      if (details.message) {
        errorInfo += ' ' + details.message;
      }
      if (details.lineNumber) {
        errorInfo += '<br>Line ' + details.lineNumber;
        if (details.columnNumber) {
          errorInfo += ' column ' + details.columnNumber;
        }
        errorInfo += ' of script.';
      }
      this.validationError = errorInfo;
      if (details.lineNumber) {
        const line = details.lineNumber - 1;
        let column = 0;
        if (details.columnNumber) {
          column = details.columnNumber;
        }
        const errorMarkerId = this.jsEditor.session.addMarker(new Range(line, 0, line, Infinity),
          'ace_active-line', 'screenLine');
        this.errorMarkers.push(errorMarkerId);
        const annotations = this.jsEditor.session.getAnnotations();
        const errorAnnotation: Ace.Annotation = {
          row: line,
          column,
          text: details.message,
          type: 'error'
        };
        this.errorAnnotationId = annotations.push(errorAnnotation) - 1;
        this.jsEditor.session.setAnnotations(annotations);
      }
      return false;
    }
  }

  private cleanupJsErrors(): void {
    if (this.errorShowed) {
      this.store.dispatch(new ActionNotificationHide(
        {
          target: this.toastTargetId
        }));
      this.errorShowed = false;
    }
    this.errorMarkers.forEach((errorMarker) => {
      this.jsEditor.session.removeMarker(errorMarker);
    });
    this.errorMarkers.length = 0;
    if (this.errorAnnotationId > -1) {
      const annotations = this.jsEditor.session.getAnnotations();
      annotations.splice(this.errorAnnotationId, 1);
      this.jsEditor.session.setAnnotations(annotations);
      this.errorAnnotationId = -1;
    }
  }

  writeValue(value: string): void {
    this.modelValue = value;
    if (this.jsEditor) {
      this.ignoreChange = true;
      this.jsEditor.setValue(this.modelValue ? this.modelValue : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.jsEditor.getValue();
    if (this.modelValue !== editorValue) {
      this.modelValue = editorValue;
      this.functionValid = true;
      this.propagateChange(this.modelValue);
      this.cd.markForCheck();
    }
  }
}
