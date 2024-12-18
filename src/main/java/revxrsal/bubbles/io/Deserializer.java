package revxrsal.bubbles.io;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface Deserializer<T> {

    T serialize(@NotNull Type type);

}
