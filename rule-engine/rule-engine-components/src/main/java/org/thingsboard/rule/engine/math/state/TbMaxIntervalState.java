package org.thingsboard.rule.engine.math.state;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
public class TbMaxIntervalState extends TbBaseIntervalState {

    private double max = -Double.MAX_VALUE;

    public TbMaxIntervalState(double max) {
        this.max = max;
    }

    @Override
    protected boolean doUpdate(double value) {
        if (value > max) {
            max = value;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public double getValue() {
        return max;
    }

    @Override
    public String toJson(Gson gson) {
        JsonObject object = new JsonObject();
        object.addProperty("max", max);
        return gson.toJson(object);
    }
}
