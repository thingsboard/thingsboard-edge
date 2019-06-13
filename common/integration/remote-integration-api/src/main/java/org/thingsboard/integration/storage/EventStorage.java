package org.thingsboard.integration.storage;

import com.google.protobuf.GeneratedMessageV3;

import java.util.List;
import java.util.function.Function;

public interface EventStorage<T extends GeneratedMessageV3> {

    void init(Function<byte[], T> parserFunction);

    void write(T event);

    List<T> readCurrentBatch();

    void discardCurrentBatch();

}
