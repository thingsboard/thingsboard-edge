package org.thingsboard.server.service.edge.stats;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CloudStatsCounterService {

    private final MsgCounters uplinkCounters = new MsgCounters();

    public void incrementUplinkMsgsAdded() {
        uplinkCounters.getMsgsAdded().incrementAndGet();
    }

    public void incrementUplinkMsgsPushed() {
        uplinkCounters.getMsgsPushed().incrementAndGet();
    }

    public void addUplinkMsgsPermanentlyFailed(long value) {
        uplinkCounters.getMsgsPermanentlyFailed().addAndGet(value);
    }

    public void incrementUplinkMsgsTmpFailed() {
        uplinkCounters.getMsgsTmpFailed().incrementAndGet();
    }

    public void setUplinkMsgsLag(long value) {
        uplinkCounters.getMsgsLag().set(value);
    }

    public MsgCounters getUplinkCounters() {
        return uplinkCounters;
    }

    public void clear() {
        uplinkCounters.clear();
    }

}
