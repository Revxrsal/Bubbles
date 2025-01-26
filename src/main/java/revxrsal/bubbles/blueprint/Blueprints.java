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

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import revxrsal.bubbles.annotation.Blueprint;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class Blueprints {

    private static final Map<Class<?>, BlueprintClass> IMPLEMENTATIONS = new HashMap<>();

    /**
     * Generates and loads (if necessary) the blueprint implementation of
     * the given blueprint interface
     *
     * @param interfaceType The blueprint type
     * @return The generated {@link BlueprintClass}
     */
    public static @NotNull BlueprintClass from(@NotNull Class<?> interfaceType) {
        if (!interfaceType.isInterface())
            throw new IllegalArgumentException("Class is not an interface.");
        if (!interfaceType.isAnnotationPresent(Blueprint.class))
            throw new IllegalArgumentException("Interface must have @Blueprint");
        BlueprintClass bp = IMPLEMENTATIONS.get(interfaceType);
        if (bp != null)
            return bp;
        bp = BlueprintClass.from(interfaceType);
        BlueprintGenerator generator = new BlueprintGenerator(bp);
        bp.setClass(generator.define());
        IMPLEMENTATIONS.put(interfaceType, bp);
        return bp;
    }

    /**
     * Tests whether the given class is a blueprint interface or not
     *
     * @param cl The class to check for
     * @return true if it's a blueprint
     */
    public static boolean isBlueprint(@NotNull Class<?> cl) {
        return cl.isInterface() && cl.isAnnotationPresent(Blueprint.class);
    }

    /**
     * Returns a {@link TypeAdapterFactory} specialized for parsing
     * blueprint interfaces. You must register this to be able
     * to use blueprints
     *
     * @return
     */
    public static @NotNull TypeAdapterFactory gsonFactory() {
        return ICreator.instance;
    }

    private static class ICreator implements TypeAdapterFactory {

        private static final ICreator instance = new ICreator();

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            Class<? super T> rawType = typeToken.getRawType();
            if (!isBlueprint(rawType)) {
                return null;
            }
            BlueprintClass impl = Blueprints.from(rawType);
            Type iType = typeToken.getType();
            TypeToken<?> implType;
            if (iType instanceof ParameterizedType) {
                implType = TypeToken.getParameterized(impl.implClass(), ((ParameterizedType) iType).getActualTypeArguments());
            } else {
                implType = TypeToken.get(impl.implClass());
            }
            return (TypeAdapter<T>) gson.getDelegateAdapter(this, implType);
        }
    }

}
