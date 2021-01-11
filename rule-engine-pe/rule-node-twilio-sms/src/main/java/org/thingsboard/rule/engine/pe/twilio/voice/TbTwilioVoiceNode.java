/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.pe.twilio.voice;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Pause;
import com.twilio.twiml.voice.Say;
import com.twilio.twiml.voice.SsmlProsody;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "twilio voice",
        configClazz = TbTwilioVoiceNodeConfiguration.class,
        nodeDescription = "Sends voice message via Twilio.",
        nodeDetails = "Will send message payload as voice message via Twilio, using Twilio text to speech service.",
        uiResources = {"static/rulenode/twilio-sms-config.js"},
        configDirective = "tbActionNodeTwilioVoiceConfig",
        icon = "phone_in_talk",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/external-nodes/#twilio-voice-node"
)
public class TbTwilioVoiceNode implements TbNode {

    private TbTwilioVoiceNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbTwilioVoiceNodeConfiguration.class);
        Twilio.init(this.config.getAccountSid(), this.config.getAccountToken());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        withCallback(ctx.getExternalCallExecutor().executeAsync(() -> {
                    sendVoiceMessage(ctx, msg);
                    return null;
                }),
                ok -> ctx.tellNext(msg, SUCCESS),
                fail -> ctx.tellFailure(msg, fail));
    }

    private void sendVoiceMessage(TbContext ctx, TbMsg msg) throws Exception {
        String numberFrom = TbNodeUtils.processPattern(this.config.getNumberFrom(), msg.getMetaData());
        String numbersTo = TbNodeUtils.processPattern(this.config.getNumbersTo(), msg.getMetaData());

        String[] numbersToList = numbersTo.split(",");
        if (StringUtils.isBlank(numbersToList[0])) {
            throw new IllegalArgumentException("To numbers list is empty!");
        }

        String payload = msg.getData();
        payload = payload.substring(1, payload.length()-1);
        Say.Language language = Say.Language.EN_US;
        for (Say.Language lang : Say.Language.values()) {
            if (lang.toString().equals(config.getLanguage())){
                language = lang;
                break;
            }
        }

        Say.Voice voice = Say.Voice.MAN;
        for (Say.Voice voiceIter : Say.Voice.values()) {
            if (voiceIter.toString().equals(config.getVoice())){
                voice = voiceIter;
                break;
            }
        }

        SsmlProsody prosody = new SsmlProsody.Builder(payload)
                .pitch(config.getPitch().toString() + "%")
                .rate(config.getRate().toString() + "%")
                .volume(config.getVolume().toString() + "dB")
                .build();

        Pause startPause = new Pause.Builder().length(config.getStartPause()).build();
        Say say = new Say.Builder().language(language).voice(voice).prosody(prosody).build();
        VoiceResponse response = new VoiceResponse.Builder().pause(startPause).say(say).build();

        for (String numberTo : numbersToList) {
            Call.creator(
                    new PhoneNumber(numberTo.trim()),
                    new PhoneNumber(numberFrom.trim()),
                    new Twiml(response.toXml())
            ).create();
        }
    }

    @Override
    public void destroy() {
    }

}
