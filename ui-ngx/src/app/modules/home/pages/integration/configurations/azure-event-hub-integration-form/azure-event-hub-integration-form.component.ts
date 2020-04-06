import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { IntegrationType } from '@app/shared/models/integration.models';

@Component({
  selector: 'tb-azure-event-hub-integration-form',
  templateUrl: './azure-event-hub-integration-form.component.html',
  styleUrls: ['./azure-event-hub-integration-form.component.scss']
})
export class AzureEventHubIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;

  constructor() { }

  ngOnInit(): void {
  }

}
