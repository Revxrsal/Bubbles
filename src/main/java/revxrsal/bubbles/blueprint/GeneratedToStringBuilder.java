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

import java.lang.reflect.Array;
import java.util.StringJoiner;

public final class GeneratedToStringBuilder {

    private final StringJoiner joiner;

    public GeneratedToStringBuilder(String name) {
        joiner = new StringJoiner(", ", name + "(", ")");
    }

    public void append(String name, Object value) {
        if (value == null) {
            joiner.add(name + "=null");
        } else {
            if (value.getClass().isArray()) {
                joiner.add(name + "=" + toString(value));
            } else {
                joiner.add(name + "=" + value);
            }
        }
    }

    @Override public String toString() {
        return joiner.toString();
    }

    private static String toString(Object a) {
        if (a == null)
            return "null";

        int iMax = Array.getLength(a) - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(Array.get(a, i));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

}
