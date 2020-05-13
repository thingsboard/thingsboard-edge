import { FormGroup } from '@angular/forms';

const basic = ['username', 'password'];
const pem = ['caCertFileName', 'caCert', 'certFileName', 'cert', 'privateKeyFileName', 'privateKey', 'privateKeyPassword'];


export function changeRequirement(form: FormGroup, credentialType: 'anonymous' | 'basic' | 'cert.PEM') {
    let disabled = [];
    let enabled = [];
    switch (credentialType) {
        case 'anonymous':
            disabled = [...basic, ...pem];
            break;
        case 'basic':
            disabled = pem;
            enabled = basic;
            break;
        case 'cert.PEM':
            disabled = basic;
            enabled = pem;
            break;
    }

    disableFields(form, disabled);
    enableFields(form, enabled);
}

export function disableFields(form: FormGroup, fields: string[]) {
    fields.forEach(key => {
        if (form.get(key)) {
            form.get(key).setValue(null);
            form.get(key).disable();
        }
    });
}

export function enableFields(form: FormGroup, fields: string[]) {
    fields.forEach(key => {
        if (form.get(key))
            form.get(key).enable();
    });
}