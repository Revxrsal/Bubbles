package revxrsal.bubbles.asm;

import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import revxrsal.Container;
import revxrsal.bubbles.Config;
import revxrsal.bubbles.Property;
import revxrsal.bubbles.io.Emitter;
import revxrsal.bubbles.loader.Definer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.objectweb.asm.Opcodes.*;

@Getter
public final class ClassGenerator {

    private static final Type CONFIG_TYPE = Type.getType(Config.class);
    private static final Type EMITTER_TYPE = Type.getType(Emitter.class);
    private static final Type GENERATED_CONFIG_TYPE = Type.getType(GeneratedConfig.class);
    private static final Method CLASS_CONSTRUCTOR = new Method("<init>", Type.VOID_TYPE, new Type[]{CONFIG_TYPE});
    private static final Method OBJECT_CONSTRUCTOR = Method.getMethod("void <init>()");
    private static final Method RELOAD_METHOD = Method.getMethod("void reload()");
    private static final Method EMIT_METHOD = Method.getMethod("void emit(" + EMITTER_TYPE.getClassName() + ")");
    private static final Method PARSE, EXISTS;
    private final ClassWriter writer;
    private final Container container;
    private final Type implType;

    public ClassGenerator(Container container) {
        this.writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        this.container = container;
        this.implType = Type.getType("L" + (container.getConfigInterface().getName() + "Impl").replace('.', '/') + ";");
        writer.visit(Opcodes.V1_8,
                ACC_PUBLIC,
                implType.getInternalName(),
                null,
                "java/lang/Object",
                new String[]{container.getConfigType().getInternalName(), GENERATED_CONFIG_TYPE.getInternalName()}
        );
    }

    private void defineConfigField() {
        writer.visitField(
                ACC_PRIVATE | ACC_FINAL,
                "config",
                CONFIG_TYPE.getDescriptor(),
                null,
                null
        );
    }

    private void definePropertyField(Property property) {
        writer.visitField(
                ACC_PRIVATE,
                property.getName(),
                property.getPropertyType().getDescriptor(),
                null,
                null
        );
    }

    private void generateConstructor() {
        GeneratorAdapter adapter = new GeneratorAdapter(
                ACC_PUBLIC, CLASS_CONSTRUCTOR, null, null, writer
        );
        adapter.visitParameter("config", 0);
        adapter.loadThis();
        adapter.invokeConstructor(Type.getType(Object.class), OBJECT_CONSTRUCTOR);

        adapter.loadThis();
        adapter.loadArg(0);
        adapter.putField(implType, "config", CONFIG_TYPE);

        adapter.loadThis();
        adapter.invokeVirtual(implType, RELOAD_METHOD);

        adapter.returnValue();
        adapter.endMethod();
    }

    private void createReloadMethod() {
        GeneratorAdapter adapter = new GeneratorAdapter(
                ACC_PUBLIC, RELOAD_METHOD, null, null, writer
        );

        for (Property property : container.getProperties()) {
            adapter.loadThis();
            adapter.loadThis();
            adapter.invokeVirtual(implType, property.getLoaderMethod());
            adapter.putField(implType, property.getName(), property.getPropertyType());
        }

        adapter.returnValue();
        adapter.endMethod();
    }

    private void createEmitMethod() {
        GeneratorAdapter adapter = new GeneratorAdapter(
                ACC_PUBLIC, EMIT_METHOD, null, null, writer
        );
        adapter.visitParameter("emitter", 0);
        adapter.returnValue();
        adapter.endMethod();
    }

    private void createLoaderMethod(@NotNull Property property) {
        Method method = property.getLoaderMethod();
        GeneratorAdapter adapter = new GeneratorAdapter(
                ACC_PUBLIC, method, null, null, writer
        );
        adapter.loadThis();
        adapter.getField(implType, "config", CONFIG_TYPE);
        adapter.push(property.getName()); // the property name
        adapter.invokeInterface(CONFIG_TYPE, EXISTS);
        Label afterThrow = new Label();
        adapter.visitJumpInsn(Opcodes.IFNE, afterThrow);

        if (!property.hasDefaultValue()) {
            adapter.newInstance(Type.getType(IllegalArgumentException.class));
            adapter.dup(); // Duplicate the reference for the constructor call
            adapter.push("Missing property: '" + property.getName() + "'"); // Push the exception message
            adapter.invokeConstructor(Type.getType(IllegalArgumentException.class),
                    new Method("<init>", Type.VOID_TYPE, new Type[]{Type.getType(String.class)}));
            adapter.throwException(); // Throw the exception
        } else {
            adapter.loadThis();
            // for some reason INVOKEINTERFACE is invokeConstructor... :shrug:
            adapter.invokeConstructor(container.getConfigType(), property.getAsmMethod());
            adapter.returnValue();
        }
        adapter.mark(afterThrow);

        adapter.loadThis();
        adapter.getField(implType, "config", CONFIG_TYPE);
        adapter.push(property.getName());
        adapter.push(method.getReturnType());
        adapter.invokeInterface(CONFIG_TYPE, PARSE);
        adapter.unbox(method.getReturnType());
        adapter.returnValue();

        adapter.endMethod();
    }

    public void generate() {
        defineConfigField();
        generateConstructor();
        for (Property property : container.getProperties()) {
            definePropertyField(property);
            createPropertyGetter(property);
            createLoaderMethod(property);
        }
        createEmitMethod();
        createReloadMethod();
    }

    private void createPropertyGetter(Property property) {
        Method method = property.getAsmMethod();
        GeneratorAdapter adapter = new GeneratorAdapter(
                ACC_PUBLIC, method, null, null, writer
        );
        adapter.loadThis();
        adapter.getField(implType, property.getName(), property.getPropertyType());
//        adapter.checkCast(property.getPropertyType());
        adapter.returnValue();
        adapter.endMethod();
    }

    public byte[] output() {
        return writer.toByteArray();
    }

    @SneakyThrows
    public void output(@NotNull File file) {
        if (!file.exists())
            file.createNewFile();
        Files.write(file.toPath(), output(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    @SneakyThrows
    public void output(@NotNull Path file) {
        if (!Files.exists(file))
            Files.createFile(file);
        Files.write(file, output(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    @SneakyThrows
    public void output(@NotNull String name) {
        output(new File(name));
    }

    public <T> Class<? extends T> load() {
        //noinspection unchecked
        return (Class<? extends T>) Definer.defineClass(
                getClass().getClassLoader(),
                implType.getClassName(),
                output()
        );
    }

    static {
        try {
            PARSE = Method.getMethod(Config.class.getMethod("parse", String.class, java.lang.reflect.Type.class));
            EXISTS = Method.getMethod(Config.class.getMethod("exists", String.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
