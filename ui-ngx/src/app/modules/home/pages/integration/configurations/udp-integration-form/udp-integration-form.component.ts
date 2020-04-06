import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { IntegrationType } from '@app/shared/models/integration.models';

@Component({
  selector: 'tb-udp-integration-form',
  templateUrl: './udp-integration-form.component.html',
  styleUrls: ['./udp-integration-form.component.scss']
})
export class UdpIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;

  constructor() { }

  ngOnInit(): void {
  }

}
