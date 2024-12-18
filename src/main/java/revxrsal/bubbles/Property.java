package revxrsal.bubbles;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public final class Property {

    private final String name;
    private final Method method;
    private final Type type;
    private final Class<?> configType;

    public Property(String name, Method method, Class<?> configType) {
        this.name = name;
        this.method = method;
        this.configType = configType;
        this.type = method.getGenericReturnType();
    }
}
