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
abstract class TbBaseIntervalState implements TbIntervalState {

    private boolean hasChanges = false;

    @Override
    public void update(double value) {
        if(doUpdate(value)){
            hasChanges = true;
        }
    }

    @Override
    public boolean hasChanges() {
        return hasChanges;
    }

    @Override
    public void clearChanges() {
        hasChanges = false;
    }

    protected abstract boolean doUpdate(double value);
}
