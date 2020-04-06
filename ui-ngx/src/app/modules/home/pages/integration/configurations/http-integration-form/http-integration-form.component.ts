import { Component, OnInit, Input } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Integration, IntegrationType } from '@shared/models/integration.models';

@Component({
  selector: 'tb-http-integration-form',
  templateUrl: './http-integration-form.component.html',
  styleUrls: ['./http-integration-form.component.scss']
})
export class HttpIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;
  integrationTypes = IntegrationType;

  enableSecurity: boolean;

  constructor() { }

  ngOnInit(): void {
  }

}
