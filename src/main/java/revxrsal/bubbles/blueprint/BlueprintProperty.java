/*
 * This file is part of Bubbles, licensed under the MIT License.
 *
 *  Copyright (c) Revxrsal <reflxction.github@gmail.com>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package revxrsal.bubbles.blueprint;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.Type;
import revxrsal.bubbles.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Represents a property in a {@link Blueprint}
 */
@RequiredArgsConstructor
public final class BlueprintProperty {

    private final @NotNull String key;
    private Type type;
    private Class<?> propClass;
    private Method getter;
    private @Nullable Method setter;
    private @Unmodifiable List<String> comments = Collections.emptyList();

    private org.objectweb.asm.commons.Method asmGetter;
    private @Nullable org.objectweb.asm.commons.Method asmSetter;

    public @NotNull String key() {
        return key;
    }

    public @NotNull Type type() {
        return type;
    }

    public @NotNull Method getter() {
        return getter;
    }

    @SneakyThrows
    public Object get(Object v) {
        return getter.invoke(v);
    }

    public boolean hasDefault() {
        return getter.isDefault();
    }

    public @Nullable Method setter() {
        return setter;
    }

    public @NotNull @Unmodifiable List<String> comments() {
        return comments;
    }

    public boolean hasComments() {
        return !comments.isEmpty();
    }

    public @Nullable org.objectweb.asm.commons.Method asmSetter() {
        return asmSetter;
    }

    public @NotNull org.objectweb.asm.commons.Method asmGetter() {
        return asmGetter;
    }

    public @NotNull String fieldName() {
        return getter.getName();
    }

    public static @NotNull @Unmodifiable Map<String, BlueprintProperty> propertiesOf(@NotNull Class<?> interfaceType) {
        Objects.requireNonNull(interfaceType, "interface cannot be null!");
        if (!interfaceType.isInterface())
            throw new IllegalArgumentException("Class is not an interface: " + interfaceType.getName());
        if (!interfaceType.isAnnotationPresent(Blueprint.class))
            throw new IllegalArgumentException("Interface does not have @Blueprint on it!");
        Map<String, BlueprintProperty> properties = new LinkedHashMap<>();
        Method[] methods = interfaceType.getMethods();
        sortByAnnotation(methods);
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers()))
                continue;
            if (method.isAnnotationPresent(IgnoreMethod.class)) {
                if (method.isDefault())
                    continue;
                else
                    throw new IllegalArgumentException("Cannot ignore a non-default method! Ignored methods must be default");
            }
            parse(method, properties);
        }
        for (BlueprintProperty value : properties.values()) {
            if (value.type == null)
                throw new IllegalArgumentException("Failed to infer the type of property '" + value.key + "'!");
            if (value.getter == null)
                throw new IllegalArgumentException("No getter exists for property '" + value.key + "'!");
        }
        return Collections.unmodifiableMap(properties);
    }

    private static void sortByAnnotation(Method[] methods) {
        Arrays.sort(methods, (o1, o2) -> {
            Pos pos1 = o1.getAnnotation(Pos.class);
            Pos pos2 = o2.getAnnotation(Pos.class);
            if (pos1 == null && pos2 == null)
                return 0; // Both methods are unannotated
            if (pos1 == null)
                return -1; // o1 is unannotated, so it comes first
            if (pos2 == null)
                return 1;  // o2 is unannotated, so it comes first

            // Both methods have the annotation, compare their values
            return Integer.compare(pos1.value(), pos2.value());
        });
    }

    private void setType(@Nullable Type type) {
        if (this.type == null)
            this.type = type;
        else if (!this.type.equals(type))
            throw new IllegalArgumentException("Inconsistent types for property " + key + ". Received " + this.type + " and " + type + ".");
    }

    private static void parse(
            @NotNull Method method,
            @NotNull Map<String, BlueprintProperty> properties
    ) {
        String key = keyOf(method);
        BlueprintProperty existing = properties.computeIfAbsent(key, BlueprintProperty::new);
        @Nullable List<String> comments = commentsOf(method);
        if (comments != null) {
            if (existing.comments.isEmpty())
                existing.comments = comments;
            else
                throw new IllegalArgumentException("Inconsistent comments for property '" + key + "'");
        }
        if (method.getReturnType() == Void.TYPE || impliesSetter(method)) {
            if (existing.setter != null)
                throw new IllegalArgumentException("Found 2 setters for property '" + key + "'!");
            if (method.getReturnType() != Void.TYPE)
                throw new IllegalArgumentException("Setter for property '" + key + "' must return void!");
            if (method.getParameterCount() == 0)
                throw new IllegalArgumentException("Setter for property '" + key + "' has no parameters!");
            if (method.getParameterCount() > 1)
                throw new IllegalArgumentException("Setter for property '" + key + "' has more than 1 parameter!");
            existing.propClass = method.getParameterTypes()[0];
            Type type = Type.getType(existing.propClass);
            existing.setType(type);
            existing.setter = method;
            existing.asmSetter = org.objectweb.asm.commons.Method.getMethod(method);
        } else {
            if (existing.getter != null)
                throw new IllegalArgumentException("Found 2 getters for property '" + key + "'!");
            if (method.getParameterCount() != 0)
                throw new IllegalArgumentException("Getter for property '" + key + "' cannot take parameters!");
            existing.propClass = method.getReturnType();
            Type type = Type.getType(existing.propClass);
            existing.setType(type);
            existing.getter = method;
            existing.asmGetter = org.objectweb.asm.commons.Method.getMethod(method);
        }
    }

    private static boolean impliesSetter(@NotNull Method method) {
        return method.getName().startsWith("set");
    }

    private static String keyOf(@NotNull Method method) {
        Key key = method.getAnnotation(Key.class);
        if (key == null)
            return lowerFirst(fromName(method.getName()));
        return key.value();
    }

    private static String fromName(String name) {
        if (name.startsWith("get") || name.startsWith("set"))
            return name.substring(3);
        else if (name.startsWith("is"))
            return name.substring(2);
        return name;
    }

    private static String lowerFirst(@NotNull String name) {
        if (name.isEmpty())
            return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static @Nullable List<String> commentsOf(@NotNull Method method) {
        Comment comment = method.getAnnotation(Comment.class);
        if (comment != null)
            return Arrays.asList(comment.value());
        return null;
    }

    @Override
    public String toString() {
        return "BlueprintProperty(key='" + key + "')";
    }

    public Class<?> propClass() {
        return propClass;
    }

//    @Override
//    public String toString() {
//        return "BlueprintProperty(" +
//                "key='" + key + '\'' +
//                ", type=" + type.getClassName() +
//                ", getter=" + getter +
//                ", setter=" + setter +
//                ", comments=" + comments +
//                ')';
//    }
}
