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

import com.google.gson.annotations.SerializedName;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

final class AsmConstants {

    public static final Type OBJECT_CLASS = Type.getType(Object.class);
    public static final Method NO_ARG_CONSTRUCTOR = Method.getMethod("void <init>()");

    public static final Type TO_STRING_BUILDER = Type.getType(GeneratedToStringBuilder.class);
    public static final Method TO_STRING_BUILDER_CONSTRUCTOR = Method.getMethod("void <init>(java.lang.String)");
    public static final Method TO_STRING_APPEND = Method.getMethod("void append(java.lang.String, java.lang.Object)");
    public static final Method TO_STRING = Method.getMethod("java.lang.String toString()");

    public static final Type SERIALIZED_NAME = Type.getType(SerializedName.class);

    private AsmConstants() {
    }

}
