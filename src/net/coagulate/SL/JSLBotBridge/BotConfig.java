package net.coagulate.SL.JSLBotBridge;

import net.coagulate.JSLBot.Configuration;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Iain Price
 */
public class BotConfig extends Configuration {

    Map<String,String> config=new HashMap<>();
    @Nullable
    @Override
    public String get(String param) {
        return config.get(param);
    }

    @Override
    public void put(String key, String value) {
        config.put(key,value);
    }

    @Override
    public Set<String> get() {
        return config.keySet();
    }

    @Override
    public String dump() {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public boolean persistent() {
        return false;
    }
}
