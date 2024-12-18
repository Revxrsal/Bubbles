package revxrsal.bubbles.asm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.TraceClassVisitor;
import revxrsal.bubbles.Config;
import revxrsal.bubbles.sample.HomeConfig;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.ClassWriter.*;
import static org.objectweb.asm.Opcodes.*;

public class Generatos {

    private static final Type CONFIG_TYPE = Type.getType(Config.class);
    private static final Method PARSE_METHOD, EXISTS_METHOD;

    static {
        try {
            PARSE_METHOD = Method.getMethod(Config.class.getMethod("parse", String.class, java.lang.reflect.Type.class));
            EXISTS_METHOD = Method.getMethod(Config.class.getMethod("exists", String.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final ClassWriter writer;
    private final Type type, interfaceType;

    public Generatos(ClassWriter writer, Type interfaceType) {
        this.writer = writer;
        this.type = Type.getType("L" + (HomeConfig.class.getName() + "Impl").replace('.', '/') + ";");
        this.interfaceType = interfaceType;
        writer.visit(Opcodes.V1_8,
                ACC_PUBLIC,
                type.getInternalName(),
                null,
                "java/lang/Object",
                new String[]{interfaceType.getInternalName()}
        );
    }

    public static void main(String[] args) throws Throwable {
        File file = new File("output.class");
        System.out.println(file.getAbsoluteFile());
        file.createNewFile();
        ClassWriter writer = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
        Generatos generatos = new Generatos(writer, Type.getType(HomeConfig.class));
        generatos.defineConfigField();
        generatos.generateConstructor();
        for (java.lang.reflect.Method method : HomeConfig.class.getMethods()) {
            generatos.cloneMethod(method);
        }
        writer.visitEnd();
        Files.write(file.toPath(), writer.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING);

        TraceClassVisitor trace = new TraceClassVisitor(writer, new PrintWriter(System.out));
        ClassReader reader = new ClassReader(writer.toByteArray());
        reader.accept(trace, ClassReader.EXPAND_FRAMES);

    }

    public void defineConfigField() {
        writer.visitField(ACC_PRIVATE | ACC_FINAL, "config", CONFIG_TYPE.getDescriptor(), null, null);
    }

    public void generateConstructor() {
        Method constructor = new Method("<init>", Type.VOID_TYPE, new Type[]{CONFIG_TYPE});
        GeneratorAdapter adapter = new GeneratorAdapter(
                ACC_PUBLIC, constructor, null, null, writer
        );
        adapter.visitParameter("config", 0);
        adapter.loadThis();
        adapter.invokeConstructor(Type.getType(Object.class), Method.getMethod("void <init>()"));

        adapter.loadThis();
        adapter.loadArg(0);
        adapter.putField(type, "config", CONFIG_TYPE);

        adapter.returnValue();
        adapter.endMethod();
    }

    public void cloneMethod(@NotNull java.lang.reflect.Method refMethod) {
        Method method = Method.getMethod(refMethod);
        GeneratorAdapter adapter = new GeneratorAdapter(
                ACC_PUBLIC, method, null, null, writer
        );


        /* if (config.exists("property")) { */
        adapter.loadThis();
        adapter.getField(type, "config", CONFIG_TYPE);
        adapter.push(method.getName()); // the property name
        adapter.invokeInterface(CONFIG_TYPE, EXISTS_METHOD);
        Label afterThrow = new Label();
        adapter.visitJumpInsn(Opcodes.IFNE, afterThrow); // Jump to 'afterThrow' if exists() == true
        // Step 5: Throw new IllegalArgumentException("Missing property: 'home'")

        if (!refMethod.isDefault()) {
            adapter.newInstance(Type.getType(IllegalArgumentException.class));
            adapter.dup(); // Duplicate the reference for the constructor call
            adapter.push("Missing property: '" + method.getName() + "'"); // Push the exception message
            adapter.invokeConstructor(Type.getType(IllegalArgumentException.class),
                    new Method("<init>", Type.VOID_TYPE, new Type[]{Type.getType(String.class)}));
            adapter.throwException(); // Throw the exception
        } else {
            adapter.loadThis();
            adapter.invokeInterface(interfaceType, method);
            adapter.returnValue();
        }
        adapter.mark(afterThrow);

        /*      return config.parse(property name, property type) */
        adapter.loadThis();
        adapter.getField(type, "config", CONFIG_TYPE);
        adapter.push(refMethod.getName()); // technically the property name
        adapter.push(method.getReturnType());
        adapter.invokeInterface(CONFIG_TYPE, PARSE_METHOD);
        adapter.checkCast(method.getReturnType());
        adapter.returnValue();
        /* } */


        adapter.endMethod();
    }

}
