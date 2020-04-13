import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-azure-event-hub-integration-form',
  templateUrl: './azure-event-hub-integration-form.component.html',
  styleUrls: ['./azure-event-hub-integration-form.component.scss']
})
export class AzureEventHubIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;


  constructor() { }

  ngOnInit(): void {
  }

}
