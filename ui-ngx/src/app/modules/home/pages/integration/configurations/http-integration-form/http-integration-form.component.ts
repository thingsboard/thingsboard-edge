import { Component, Input, AfterViewInit, ChangeDetectionStrategy } from '@angular/core';
import { FormGroup, Validators, AbstractControl } from '@angular/forms';
import { IntegrationType } from '@shared/models/integration.models';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-http-integration-form',
  templateUrl: './http-integration-form.component.html',
  styleUrls: ['./http-integration-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HttpIntegrationFormComponent implements AfterViewInit {

  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;
  @Input() routingKey;

  integrationTypes = IntegrationType;

  constructor(protected store: Store<AppState>, private translate: TranslateService) { }

  ngAfterViewInit(): void {
    this.integrationBaseUrlChanged();
  }

  httpEnableSecurityChanged = () => {
    const headersFilter = this.form.get('headersFilter');
    if (this.form.get('enableSecurity').value &&
      !headersFilter.value) {
      headersFilter.patchValue({});
      headersFilter.setValidators(Validators.required);
      headersFilter.updateValueAndValidity();
    } else if (!this.form.get('enableSecurity').value) {
      headersFilter.patchValue(null);
      headersFilter.setValidators([]);
    }
  };

  thingparkEnableSecurityChanged = () => {
    if (!this.form.get('enableSecurity').value) {
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
    setTimeout(() => {
      this.form.get('httpEndpoint').patchValue(url);
    }, 0);
  };
}
