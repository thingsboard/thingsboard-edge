import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { IntegrationType } from '@app/shared/models/integration.models';

@Component({
  selector: 'tb-kafka-integration-form',
  templateUrl: './kafka-integration-form.component.html',
  styleUrls: ['./kafka-integration-form.component.scss']
})
export class KafkaIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;

  constructor() { }

  ngOnInit(): void {
  }

}
