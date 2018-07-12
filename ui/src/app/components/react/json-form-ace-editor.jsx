/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
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
import './json-form-ace-editor.scss';

import React from 'react';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import reactCSS from 'reactcss';
import AceEditor from 'react-ace';
import FlatButton from 'material-ui/FlatButton';
import 'brace/ext/language_tools';
import 'brace/theme/github';

import fixAceEditor from './../ace-editor-fix';

class ThingsboardAceEditor extends React.Component {

    constructor(props) {
        super(props);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        this.onTidy = this.onTidy.bind(this);
        this.onLoad = this.onLoad.bind(this);
        var value = props.value ? props.value + '' : '';
        this.state = {
            value: value,
            focused: false
        };
    }

    onValueChanged(value) {
        this.setState({
            value: value
        });
        this.props.onChangeValidate({
            target: {
                value: value
            }
        });
    }

    onBlur() {
        this.setState({ focused: false })
    }

    onFocus() {
        this.setState({ focused: true })
    }

    onTidy() {
        if (!this.props.form.readonly) {
            var value = this.state.value;
            value = this.props.onTidy(value);
            this.setState({
                value: value
            })
            this.props.onChangeValidate({
                target: {
                    value: value
                }
            });
        }
    }

    onLoad(editor) {
        fixAceEditor(editor);
    }

    render() {

        const styles = reactCSS({
            'default': {
                tidyButtonStyle: {
                    color: '#7B7B7B',
                    minWidth: '32px',
                    minHeight: '15px',
                    lineHeight: '15px',
                    fontSize: '0.800rem',
                    margin: '0',
                    padding: '4px',
                    height: '23px',
                    borderRadius: '5px',
                    marginLeft: '5px'
                }
            }
        });

        var labelClass = "tb-label";
        if (this.props.form.required) {
            labelClass += " tb-required";
        }
        if (this.props.form.readonly) {
            labelClass += " tb-readonly";
        }
        if (this.state.focused) {
            labelClass += " tb-focused";
        }

        return (
            <div className="tb-container">
                <label className={labelClass}>{this.props.form.title}</label>
                <div className="json-form-ace-editor">
                    <div className="title-panel">
                        <label>{this.props.mode}</label>
                        <FlatButton style={ styles.tidyButtonStyle } className="tidy-button" label={'Tidy'} onTouchTap={this.onTidy}/>
                    </div>
                    <AceEditor mode={this.props.mode}
                               height="150px"
                               width="300px"
                               theme="github"
                               onChange={this.onValueChanged}
                               onFocus={this.onFocus}
                               onBlur={this.onBlur}
                               onLoad={this.onLoad}
                               name={this.props.form.title}
                               value={this.state.value}
                               readOnly={this.props.form.readonly}
                               editorProps={{$blockScrolling: Infinity}}
                               enableBasicAutocompletion={true}
                               enableSnippets={true}
                               enableLiveAutocompletion={true}
                               style={this.props.form.style || {width: '100%'}}/>
                </div>
                <div className="json-form-error"
                    style={{opacity: this.props.valid ? '0' : '1'}}>{this.props.error}</div>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardAceEditor);
