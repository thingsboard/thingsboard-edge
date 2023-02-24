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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';
import {
  NotificationDeliveryMethodTranslateMap,
  NotificationTemplate,
  NotificationType
} from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { isEqual } from '@core/utils';
import {
  TemplateNotificationDialogComponent,
  TemplateNotificationDialogData
} from '@home/pages/notification-center/template-table/template-notification-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatButton } from '@angular/material/button';
import { coerceBoolean } from '@shared/decorators/coerce-boolean';

@Component({
  selector: 'tb-template-autocomplete',
  templateUrl: './template-autocomplete.component.html',
  styleUrls: ['./template-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TemplateAutocompleteComponent),
    multi: true
  }]
})
export class TemplateAutocompleteComponent implements ControlValueAccessor, OnInit {

  notificationDeliveryMethodTranslateMap = NotificationDeliveryMethodTranslateMap;
  selectTemplateFormGroup: FormGroup;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  @coerceBoolean()
  allowCreate: boolean = false;


  @Input()
  disabled: boolean;

  private notificationTypeValue: NotificationType;
  get notificationTypes(): NotificationType {
    return this.notificationTypeValue;
  }
  @Input()
  set notificationTypes(type) {
    if (type !== this.notificationTypeValue) {
      this.notificationTypeValue = type;
      this.reset();
    }
  }

  @ViewChild('templateInput', {static: true}) templateInput: ElementRef;

  filteredTemplate: Observable<Array<NotificationTemplate>>;

  searchText = '';

  private modelValue: EntityId | null;
  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private notificationService: NotificationService,
              private fb: FormBuilder,
              private dialog: MatDialog) {
    this.selectTemplateFormGroup = this.fb.group({
      templateName: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    if (this.required) {
      this.selectTemplateFormGroup.get('templateName').addValidators(Validators.required);
      this.selectTemplateFormGroup.get('templateName').updateValueAndValidity({emitEvent: false});
    }
    this.filteredTemplate = this.selectTemplateFormGroup.get('templateName').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchTemplate(name)),
        share()
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectTemplateFormGroup.disable({emitEvent: false});
    } else {
      this.selectTemplateFormGroup.enable({emitEvent: false});
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  writeValue(value: EntityId | null): void {
    this.searchText = '';
    if (value != null) {
      this.notificationService.getNotificationTemplateById(value.id, {ignoreLoading: true, ignoreErrors: true}).subscribe(
        (entity) => {
          this.modelValue = entity.id;
          this.selectTemplateFormGroup.get('templateName').patchValue(entity, {emitEvent: false});
        },
        () => {
          this.modelValue = null;
          this.selectTemplateFormGroup.get('templateName').patchValue('', {emitEvent: false});
          if (value !== null) {
            this.propagateChange(this.modelValue);
          }
        }
      );
    } else {
      this.modelValue = null;
      this.selectTemplateFormGroup.get('templateName').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectTemplateFormGroup.get('templateName').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  displayTemplateFn(template?: NotificationTemplate): string | undefined {
    return template ? template.name : undefined;
  }

  clear() {
    this.selectTemplateFormGroup.get('templateName').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.templateInput.nativeElement.blur();
      this.templateInput.nativeElement.focus();
    }, 0);
  }

  createTemplate($event: Event, button: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    button._elementRef.nativeElement.blur();
    this.dialog.open<TemplateNotificationDialogComponent, TemplateNotificationDialogData,
      NotificationTemplate>(TemplateNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        predefinedType: this.notificationTypes
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.selectTemplateFormGroup.get('templateName').patchValue(res);
        }
      });
  }

  private updateView(value: EntityId | null) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  private fetchTemplate(searchText?: string): Observable<Array<NotificationTemplate>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.notificationService.getNotificationTemplates(pageLink, this.notificationTypes, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<NotificationTemplate>())),
      map(pageData => {
        return pageData.data;
      })
    );
  }

  private reset() {
    this.selectTemplateFormGroup.get('templateName').patchValue('', {emitEvent: false});
    this.updateView(null);
    this.dirty = true;
  }
}
