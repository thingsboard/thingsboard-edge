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

import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpParams, HttpRequest, HttpStatusCode } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import {
  EntityConflictDialogComponent
} from '@shared/components/dialog/entity-conflict-dialog/entity-conflict-dialog.component';
import { EntityInfoData, VersionedEntity } from '@shared/models/entity.models';
import { getInterceptorConfig } from './interceptor.util';
import { isDefined } from '@core/utils';
import { InterceptorConfig } from '@core/interceptors/interceptor-config';
import { RuleChainMetaData } from '@shared/models/rule-chain.models';

@Injectable()
export class EntityConflictInterceptor implements HttpInterceptor {

  constructor(
    private dialog: MatDialog,
  ) {}

  intercept(request: HttpRequest<VersionedEntity>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!request.url.startsWith('/api/')) {
      return next.handle(request);
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status !== HttpStatusCode.Conflict) {
          return throwError(() => error);
        }

        return this.handleConflictError(request, next, error);
      })
    );
  }

  private handleConflictError(
    request: HttpRequest<VersionedEntity>,
    next: HttpHandler,
    error: HttpErrorResponse
  ): Observable<HttpEvent<unknown>> {
    if (getInterceptorConfig(request).ignoreVersionConflict || !this.isVersionedEntity(request.body)) {
      return throwError(() => error);
    }

    return this.openConflictDialog(request.body, error.error.message).pipe(
      switchMap(result => {
        if (isDefined(result)) {
          if (result) {
            return next.handle(this.updateRequestVersion(request));
          }
          (request.params as HttpParams & { interceptorConfig: InterceptorConfig }).interceptorConfig.ignoreErrors = true;
          return throwError(() => error);
        }
        return of(null);
      })
    );
  }

  private updateRequestVersion(request: HttpRequest<VersionedEntity>): HttpRequest<VersionedEntity> {
    const body = { ...request.body, version: null };
    return request.clone({ body });
  }

  private isVersionedEntity(entity: VersionedEntity): boolean {
    return !!((entity as EntityInfoData)?.id ?? (entity as RuleChainMetaData)?.ruleChainId)
  }

  private openConflictDialog(entity: VersionedEntity, message: string): Observable<boolean> {
    const dialogRef = this.dialog.open(EntityConflictDialogComponent, {
      disableClose: true,
      data: { message, entity },
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
    });

    return dialogRef.afterClosed();
  }
}
