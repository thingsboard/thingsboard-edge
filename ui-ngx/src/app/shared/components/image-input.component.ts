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

import { AfterViewInit, Component, forwardRef, Input, Output, EventEmitter, OnDestroy, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, } from '@angular/forms';
import { Subscription } from 'rxjs';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { FlowDirective } from '@flowjs/ngx-flow';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-image-input',
  templateUrl: './image-input.component.html',
  styleUrls: ['./image-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ImageInputComponent),
      multi: true
    }
  ]
})
export class ImageInputComponent extends PageComponent implements AfterViewInit, OnDestroy, ControlValueAccessor {

  @Input()
  label: string;

  @Input()
  accept = 'image/*';

  @Input()
  noImageText = this.translate.instant('dashboard.no-image');

  @Input()
  inputId = this.utils.guid();

  @Input()
  dropLabel = this.translate.instant('dashboard.drop-image');

  @Input()
  maxImageSize = 0;

  @Input()
  allowedImageMimeTypes: string[];

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
    }
  }

  @Input()
  disabled: boolean;

  @Output()
  imageTypeChanged = new EventEmitter<string>();

  @Output()
  imageSizeOverflow = new EventEmitter();

  @Output()
  imageTypeError = new EventEmitter();

  @Output()
  imageCleared = new EventEmitter();

  @Input()
  showClearButton = true;

  @Input()
  showPreview = true;

  imageType: string;
  imageUrl: string;
  safeImageUrl: SafeUrl;

  @ViewChild('flow', {static: true})
  flow: FlowDirective;

  autoUploadSubscription: Subscription;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private utils: UtilsService,
              private sanitizer: DomSanitizer) {
    super(store);
  }

  ngAfterViewInit() {
    this.autoUploadSubscription = this.flow.events$.subscribe(event => {
      if (event.type === 'fileAdded') {
        const file = (event.event[0] as flowjs.FlowFile).file;
        const reader = new FileReader();
        reader.onload = (loadEvent) => {
          let allowedImage = true;
          let type;
          let dataUrl;
          if (typeof reader.result === 'string' && reader.result.startsWith('data:image/')) {
            dataUrl = reader.result;
            type = this.extractType(dataUrl);
            if (this.allowedImageMimeTypes && this.allowedImageMimeTypes.length) {
              if (!type || this.allowedImageMimeTypes.indexOf(type) === -1) {
                allowedImage = false;
              }
            }
          } else {
            allowedImage = false;
          }
          if (allowedImage) {
            this.imageType = type;
            this.imageUrl = dataUrl;
            this.safeImageUrl = this.sanitizer.bypassSecurityTrustUrl(dataUrl);
            this.updateModel();
          } else {
            this.imageTypeError.emit();
          }
        };
        if (this.maxImageSize > 0 && file.size > this.maxImageSize) {
          this.imageSizeOverflow.emit();
        } else {
          reader.readAsDataURL(file);
        }
      }
    });
  }

  private extractType(dataUrl: string): string {
    let type;
    if (dataUrl) {
      let res: string | string[] = dataUrl.split(';');
      if (res && res.length) {
        res = res[0];
        res = res.split(':');
        if (res && res.length > 1) {
          type = res[1];
        }
      }
    }
    return type;
  }

  ngOnDestroy() {
    this.autoUploadSubscription.unsubscribe();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: string): void {
    this.imageUrl = value;
    this.imageType = this.extractType(value);
    if (this.imageUrl) {
      this.safeImageUrl = this.sanitizer.bypassSecurityTrustUrl(this.imageUrl);
    } else {
      this.safeImageUrl = null;
    }
  }

  private updateModel() {
    this.propagateChange(this.imageUrl);
    this.imageTypeChanged.emit(this.imageType);
  }

  clearImage() {
    this.imageType = null;
    this.imageUrl = null;
    this.safeImageUrl = null;
    this.updateModel();
    this.imageCleared.emit();
  }
}
