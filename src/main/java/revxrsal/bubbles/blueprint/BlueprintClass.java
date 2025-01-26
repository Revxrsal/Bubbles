package revxrsal.bubbles.blueprint;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.Type;
import revxrsal.bubbles.annotation.Blueprint;

import java.lang.reflect.Constructor;
import java.util.*;

import static revxrsal.bubbles.blueprint.BlueprintProperty.propertiesOf;

public final class BlueprintClass {

    public static final String ARRAY_INDEX = "<arr>";

    private final @NotNull String simpleName;
    private final @NotNull Type blueprintType;
    private final @NotNull Type implType;
    private final @Unmodifiable Map<String, BlueprintProperty> properties;
    private @Nullable Map<String, String> comments;
    private Class<?> cl;

    BlueprintClass(
            @NotNull String simpleName,
            @NotNull Type blueprintType,
            @NotNull Type implType,
            @NotNull Map<String, BlueprintProperty> properties
    ) {
        this.simpleName = simpleName;
        this.blueprintType = blueprintType;
        this.implType = implType;
        this.properties = properties;
    }

    private @NotNull Map<String, String> computeComments() {
        Map<String, String> comments = new HashMap<>();
        computeCommentsRecursively(comments, properties.values(), "", 0);
        return comments;
    }

    private static @NotNull Map<String, String> computeComments(Collection<BlueprintProperty> values) {
        Map<String, String> comments = new HashMap<>();
        computeCommentsRecursively(comments, values, "", 0);
        return comments;
    }

    private static void computeCommentsRecursively(
            @NotNull Map<String, String> comments,
            @NotNull Collection<BlueprintProperty> properties,
            @NotNull String parentPath,
            int indent
    ) {
        for (BlueprintProperty property : properties) {
            if (!property.hasComments())
                continue;
            String indentStr = repeat(' ', indent);
            String commentPath = parentPath.isEmpty() ? property.key() : parentPath + '.' + property.key();
            StringJoiner commentsString = new StringJoiner(System.lineSeparator(), "\n", "");
            for (String comment : property.comments()) {
                commentsString.add(indentStr + "# " + comment);
            }
            comments.put(commentPath, commentsString.toString());
            if (Blueprints.isBlueprint(property.propClass())) {
                BlueprintClass bpc = Blueprints.from(property.propClass());
                computeCommentsRecursively(
                        comments,
                        bpc.properties().values(),
                        commentPath,
                        indent + 2
                );
            } else if (isCollection(property.propClass())) {
                Class<?> type = getCollectionType(property.getter().getGenericReturnType());
                if (Blueprints.isBlueprint(type)) {
                    BlueprintClass bpc = Blueprints.from(type);
                    computeCommentsRecursively(
                            comments,
                            bpc.properties().values(),
                            commentPath + "." + ARRAY_INDEX,
                            indent + 2
                    );
                }
            }
        }
    }

    private static Class<?> getCollectionType(java.lang.reflect.Type returnType) {
        Class<?> rawType = Classes.getRawType(returnType);
        if (Collection.class.isAssignableFrom(rawType)) {
            return Classes.getRawType(Classes.getFirstGeneric(returnType, Object.class));
        } else {
            return rawType.getComponentType();
        }
    }

    private static boolean isCollection(Class<?> aClass) {
        return Collection.class.isAssignableFrom(aClass) || aClass.isArray();
    }

    private static String repeat(char v, int times) {
        char[] c = new char[times];
        Arrays.fill(c, v);
        return new String(c);
    }

    static @NotNull BlueprintClass from(@NotNull Class<?> type) {
        Objects.requireNonNull(type, "interface cannot be null!");
        if (!type.isInterface())
            throw new IllegalArgumentException("Class is not an interface: " + type.getName());
        if (!type.isAnnotationPresent(Blueprint.class))
            throw new IllegalArgumentException("Interface does not have @Blueprint on it!");
        Map<String, BlueprintProperty> properties = propertiesOf(type);
        Type blueprintClass = Type.getType(type);
        Type implType = Type.getType("L" + (type.getName() + "Impl").replace('.', '/') + ";");
        return new BlueprintClass(type.getSimpleName(), blueprintClass, implType, properties);
    }

    public @NotNull Type blueprintType() {
        return blueprintType;
    }

    public @NotNull String simpleName() {
        return simpleName;
    }

    public @NotNull Type implType() {
        return implType;
    }

    void setClass(Class<?> cl) {
        this.cl = cl;
    }

    public @NotNull Class<?> implClass() {
        return cl;
    }

    public @NotNull Map<String, String> comments() {
        if (comments == null)
            comments = computeComments();
        return comments;
    }

    public @NotNull @Unmodifiable Map<String, BlueprintProperty> properties() {
        return properties;
    }

    /**
     * Constructs the default instance of this class
     *
     * @param <T> The type
     * @return The newly created instance
     */
    @SneakyThrows
    public @NotNull <T> T createDefault() {
        //noinspection unchecked
        Constructor<T> constructor = (Constructor<T>) implClass().getDeclaredConstructor();
        return constructor.newInstance();
    }
}
