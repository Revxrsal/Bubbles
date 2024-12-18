package revxrsal.bubbles;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import revxrsal.bubbles.annotation.Comment;
import revxrsal.bubbles.annotation.Key;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public final class Property {

    private final @NotNull String name;
    private final @NotNull List<String> path;
    private final @NotNull List<String> comments;
    private final @NotNull Method interfaceMethod;
    private final @NotNull org.objectweb.asm.commons.Method asmMethod, loaderMethod;
    private final @NotNull Type propertyType;

    public Property(@NotNull Method interfaceMethod) {
        this.name = interfaceMethod.getName();
        this.path = path(interfaceMethod);
        this.interfaceMethod = interfaceMethod;
        this.asmMethod = org.objectweb.asm.commons.Method.getMethod(interfaceMethod);
        this.propertyType = Type.getType(interfaceMethod.getReturnType());
        this.loaderMethod = new org.objectweb.asm.commons.Method(
                "property_" + interfaceMethod.getName(),
                Type.getMethodDescriptor(interfaceMethod)
        );
        this.comments = comment(interfaceMethod);
    }

    private static List<String> comment(Method interfaceMethod) {
        Comment comment = interfaceMethod.getAnnotation(Comment.class);
        if (comment != null) {
            return Arrays.asList(comment.value());
        }
        return Collections.emptyList();
    }

    private static @NotNull List<String> path(@NotNull Method method) {
        Key key = method.getAnnotation(Key.class);
        if (key != null) {
            return Arrays.asList(key.value());
        }
        return Collections.singletonList(method.getName());
    }

    public boolean hasDefaultValue() {
        return interfaceMethod.isDefault();
    }
}
