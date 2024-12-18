package revxrsal.bubbles;

import java.lang.reflect.Type;

public interface Config {

    <T> T parse(String key, Type type);

    boolean exists(String key);

}
