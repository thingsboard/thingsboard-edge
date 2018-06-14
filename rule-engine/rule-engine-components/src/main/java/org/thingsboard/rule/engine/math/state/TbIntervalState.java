package org.thingsboard.rule.engine.math.state;

import com.google.gson.Gson;

/**
 * Created by ashvayka on 07.06.18.
 */

public interface TbIntervalState {

    void update(double value);

    boolean hasChanges();

    void clearChanges();

    double getValue();

    String toJson(Gson gson);

}
