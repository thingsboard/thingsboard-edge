import { Component, OnInit, Input, AfterViewInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { Integration, IntegrationType } from '@shared/models/integration.models';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-http-integration-form',
  templateUrl: './http-integration-form.component.html',
  styleUrls: ['./http-integration-form.component.scss']
})
export class HttpIntegrationFormComponent implements AfterViewInit {

  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;
  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;
  @Input() routingKey;

  integrationTypes = IntegrationType;

  constructor(protected store: Store<AppState>, private translate: TranslateService) { }

  ngAfterViewInit(): void {
    this.integrationBaseUrlChanged();
    this.form.get('httpEndpoint').disable();
  }

  httpEnableSecurityChanged = () => {
    if (this.form.get('enableSecurity').value &&
      !this.form.get('headersFilter').value) {
      this.form.get('headersFilter').patchValue({});
    } else if (!this.form.get('enableSecurity').value) {
      this.form.get('headersFilter').patchValue(null)
    }
  };

  thingparkEnableSecurityChanged = () => {
    if (this.form.get('enableSecurity').value &&
      !this.form.get('maxTimeDiffInSeconds').value) {
      this.form.get('maxTimeDiffInSeconds').patchValue(60);
    }
    else {
      this.form.get('enableSecurityNew').patchValue(false);
      this.form.get('clientIdNew').patchValue(null);
      this.form.get('clientSecret').patchValue(null);
      this.form.get('asIdNew').patchValue(null);
      this.form.get('asKey').patchValue(null);
    }
  };

  onHttpEndpointCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('integration.http-endpoint-url-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

  integrationBaseUrlChanged() {
    let url = this.form.get('baseUrl').value;
    const type = this.integrationType ? this.integrationType.toLowerCase() : '';
    const key = this.routingKey || '';
    url += `/api/v1/integrations/${type}/${key}`;
    this.form.get('httpEndpoint').patchValue(url);
  };
}
