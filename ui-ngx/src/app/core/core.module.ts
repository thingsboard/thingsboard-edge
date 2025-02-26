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

import { NgModule } from '@angular/core';
import { CommonModule, IMAGE_CONFIG } from '@angular/common';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import { GlobalHttpInterceptor } from './interceptors/global-http-interceptor';
import { effects, metaReducers, reducers } from './core.state';
import { environment as env } from '@env/environment';

import {
  MissingTranslationHandler,
  TranslateCompiler,
  TranslateLoader,
  TranslateModule,
  TranslateParser
} from '@ngx-translate/core';
import { TbMissingTranslationHandler } from './translate/missing-translate-handler';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DEFAULT_OPTIONS, MatDialogConfig, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateDefaultCompiler } from '@core/translate/translate-default-compiler';
import { WINDOW_PROVIDERS } from '@core/services/window.service';
import { HotkeyModule } from 'angular2-hotkeys';
import { TranslateDefaultParser } from '@core/translate/translate-default-parser';
import { TranslateDefaultLoader } from '@core/translate/translate-default-loader';
import { EntityConflictInterceptor } from '@core/interceptors/entity-conflict.interceptor';

@NgModule({ exports: [], imports: [CommonModule,
        MatDialogModule,
        MatButtonModule,
        MatSnackBarModule,
        // ngx-translate
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useClass: TranslateDefaultLoader
            },
            missingTranslationHandler: {
                provide: MissingTranslationHandler,
                useClass: TbMissingTranslationHandler
            },
            compiler: {
                provide: TranslateCompiler,
                useClass: TranslateDefaultCompiler
            },
            parser: {
                provide: TranslateParser,
                useClass: TranslateDefaultParser
            }
        }),
        HotkeyModule.forRoot(),
        // ngrx
        StoreModule.forRoot(reducers, { metaReducers,
            runtimeChecks: {
                strictStateImmutability: true,
                strictActionImmutability: true,
                strictStateSerializability: true,
                strictActionSerializability: true
            } }),
        EffectsModule.forRoot(effects),
        env.production
            ? []
            : StoreDevtoolsModule.instrument({
                name: env.appTitle,
                connectInZone: true
            })], providers: [
        {
            provide: HTTP_INTERCEPTORS,
            useClass: GlobalHttpInterceptor,
            multi: true
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: EntityConflictInterceptor,
            multi: true
        },
        {
            provide: MAT_DIALOG_DEFAULT_OPTIONS,
            useValue: {
                ...new MatDialogConfig(),
                restoreFocus: false
            }
        },
        WINDOW_PROVIDERS,
        provideHttpClient(withInterceptorsFromDi()),
       {
            provide: IMAGE_CONFIG,
            useValue: {
              disableImageSizeWarning: true,
              disableImageLazyLoadWarning: true
            }
       }
    ] })
export class CoreModule {
}
