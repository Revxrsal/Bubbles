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

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import revxrsal.bubbles.loader.Definer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static revxrsal.bubbles.blueprint.AsmConstants.*;

final class BlueprintGenerator {

    private final @NotNull BlueprintClass bp;
    private final @NotNull ClassWriter writer;
    private final @NotNull GeneratorAdapter constructor;

    public BlueprintGenerator(@NotNull BlueprintClass bp) {
        this.bp = bp;
        this.writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(
                Opcodes.V1_8,
                ACC_PUBLIC,
                bp.implType().getInternalName(),
                null,
                OBJECT_CLASS.getInternalName(),
                new String[]{bp.blueprintType().getInternalName()}
        );
        constructor = generateConstructor();

        for (BlueprintProperty property : bp.properties().values()) {
            generateProperty(property);
        }

        constructor.returnValue();
        constructor.endMethod();
        generateToString();

        writer.visitEnd();
    }

    private GeneratorAdapter generateConstructor() {
        GeneratorAdapter adapter = new GeneratorAdapter(
                ACC_PUBLIC, NO_ARG_CONSTRUCTOR, null, null, writer
        );
        adapter.loadThis();
        adapter.invokeConstructor(OBJECT_CLASS, NO_ARG_CONSTRUCTOR);
        return adapter;
    }

    public void generateProperty(@NotNull BlueprintProperty property) {
        // defines the field
        FieldVisitor fv = writer.visitField(
                ACC_PUBLIC,
                property.fieldName(),
                property.type().getDescriptor(),
                null,
                null
        );

        // defines @SerializedName if needed
        if (!property.fieldName().equals(property.key())) {
            AnnotationVisitor av = fv.visitAnnotation(SERIALIZED_NAME.getDescriptor(), true);
            av.visit("value", property.key());
            av.visitEnd();
        }

        // adds a constructor instruction if the field has a default value
        if (property.hasDefault()) {
            constructor.loadThis();
            constructor.loadThis();

            invokeInsn(constructor, Opcodes.INVOKESPECIAL, bp.blueprintType(), property.asmGetter(), true);
            constructor.putField(bp.implType(), property.fieldName(), property.type());
        } else if (Blueprints.isBlueprint(property.propClass())) {
            BlueprintClass bpc = Blueprints.from(property.propClass());
            initWithNoArg(bpc.implType(), property);
        } else if (property.propClass() == List.class
                || property.propClass() == Iterable.class
                || property.propClass() == Collection.class
        ) {
            initWithNoArg(ARRAY_LIST, property);
        } else if (property.propClass() == Set.class) {
            initWithNoArg(LINKED_HASH_SET, property);
        } else if (property.propClass() == Map.class) {
            initWithNoArg(LINKED_HASH_MAP, property);
        } else if (property.propClass().isArray()) {
            constructor.loadThis();
            constructor.push(0);
            constructor.newArray(Type.getType(property.propClass().getComponentType()));
            constructor.putField(bp.implType(), property.fieldName(), property.type());
        }
        generateGetter(property);
        if (property.setter() != null)
            generateSetter(property);
    }

    private static void invokeInsn(
            GeneratorAdapter adapter,
            final int opcode,
            final Type type,
            final Method method,
            final boolean isInterface
    ) {
        String owner = type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
        adapter.visitMethodInsn(opcode, owner, method.getName(), method.getDescriptor(), isInterface);
    }

    private void initWithNoArg(Type type, BlueprintProperty property) {
        constructor.loadThis();
        constructor.loadThis();
        constructor.newInstance(type);
        constructor.dup();
        constructor.invokeConstructor(type, NO_ARG_CONSTRUCTOR);
        constructor.putField(bp.implType(), property.fieldName(), property.type());
    }

    private void generateGetter(@NotNull BlueprintProperty property) {
        GeneratorAdapter adapter = new GeneratorAdapter(ACC_PUBLIC, property.asmGetter(), null, null, writer);

        adapter.loadThis();
        adapter.getField(bp.implType(), property.fieldName(), property.type());

        adapter.returnValue();
        adapter.endMethod();
    }

    private void generateSetter(@NotNull BlueprintProperty property) {
        Objects.requireNonNull(property.asmSetter());
        GeneratorAdapter adapter = new GeneratorAdapter(ACC_PUBLIC, property.asmSetter(), null, null, writer);
        adapter.visitParameter(property.fieldName(), 0);

        adapter.loadThis();
        adapter.loadArg(0);
        adapter.putField(bp.implType(), property.fieldName(), property.type());

        adapter.returnValue();
        adapter.endMethod();
    }

    private void generateToString() {
        GeneratorAdapter adapter = newMethodGenerator(writer, "toString", "()Ljava/lang/String;");
        adapter.newInstance(TO_STRING_BUILDER);
        adapter.dup();
        adapter.push(bp.simpleName());
        adapter.invokeConstructor(TO_STRING_BUILDER, TO_STRING_BUILDER_CONSTRUCTOR);
        int localIndex = adapter.newLocal(TO_STRING_BUILDER);
        adapter.storeLocal(localIndex);
        for (BlueprintProperty property : bp.properties().values()) {
            adapter.loadLocal(localIndex);
            adapter.push(property.key());
            adapter.loadThis();
            adapter.getField(bp.implType(), property.fieldName(), property.type());
            adapter.box(property.type());
            adapter.invokeVirtual(TO_STRING_BUILDER, TO_STRING_APPEND);
        }
        adapter.loadLocal(localIndex);
        adapter.invokeVirtual(TO_STRING_BUILDER, TO_STRING);
        adapter.returnValue();
        adapter.endMethod();
    }

    private static @NotNull GeneratorAdapter newMethodGenerator(ClassWriter writer, String name, String descriptor, String... throwables) {
        return new GeneratorAdapter(writer.visitMethod(ACC_PUBLIC, name, descriptor, null, throwables), ACC_PUBLIC, name, descriptor);
    }

    public @NotNull Class<?> define() {
        return Definer.defineClass(
                getClass().getClassLoader(),
                bp.implType().getClassName(),
                writer.toByteArray()
        );
    }

    @SneakyThrows
    public void output(@NotNull String file) {
        output(new File(file));
    }

    @SneakyThrows
    public void output(@NotNull File file) {
        if (!file.exists())
            file.createNewFile();
        Files.write(file.toPath(), writer.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING);
    }

}
