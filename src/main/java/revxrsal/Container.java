package revxrsal;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import revxrsal.bubbles.Property;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@Getter
public final class Container {

    private final Class<?> configInterface;
    private final Type configType;
    private final List<Property> properties;

    public Container(Class<?> configInterface) {
        this.configInterface = configInterface;
        this.configType = Type.getType(configInterface);
        this.properties = getProperties(configInterface);
    }

    private static List<Property> getProperties(Class<?> configInterface) {
        if (!configInterface.isInterface()) {
            throw new IllegalArgumentException("Class must be an interface!");
        }
        List<Property> properties = new ArrayList<>();
        for (Method method : configInterface.getMethods()) {
            Property property = createProperty(method);
            if (property == null)
                continue;
            properties.add(property);
        }
        return properties;
    }

    private static @Nullable Property createProperty(@NotNull Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return null;
        }
        return new Property(method);
    }

}
