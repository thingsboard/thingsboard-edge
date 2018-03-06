/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
import './json-form.scss';

import React from 'react';
import getMuiTheme from 'material-ui/styles/getMuiTheme';
import createThingsboardTheme from './styles/thingsboardTheme';
import ThingsboardSchemaForm from './json-form-schema-form.jsx';

class ReactSchemaForm extends React.Component {

    getChildContext() {
        return {
            muiTheme: this.state.muiTheme
        };
    }

    constructor(props) {
        super(props);
        this.state = {
            muiTheme: getMuiTheme(createThingsboardTheme(props.primaryPalette, props.accentPalette))
        };
    }

    render () {
        if (this.props.form.length > 0) {
            return <ThingsboardSchemaForm {...this.props} />;
        } else {
            return <div></div>;
        }
    }
}

ReactSchemaForm.propTypes = {
        primaryPalette: React.PropTypes.object,
        accentPalette: React.PropTypes.object,
        schema: React.PropTypes.object,
        form: React.PropTypes.array,
        model: React.PropTypes.object,
        option: React.PropTypes.object,
        onModelChange: React.PropTypes.func,
        onColorClick: React.PropTypes.func
}

ReactSchemaForm.defaultProps = {
    schema: {},
    form: [ "*" ]
}

ReactSchemaForm.childContextTypes = {
        muiTheme: React.PropTypes.object
}

export default ReactSchemaForm;
