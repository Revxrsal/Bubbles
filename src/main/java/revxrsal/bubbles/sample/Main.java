package revxrsal.bubbles.sample;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Throwable {
        parse(HomeConfig.class);
    }

    public static @NotNull List<Method> parse(Class<?> c) {
        List<Method> methods = new ArrayList<>();
        if (!c.isInterface())
            throw new IllegalArgumentException("Class must be an interface!");
        for (Method method : c.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()))
                continue;
            System.out.println(method.getName());
            methods.add(method);
        }
        return methods;
    }
}
