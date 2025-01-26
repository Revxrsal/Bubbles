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
package revxrsal.bubbles.yml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.*;
import revxrsal.bubbles.blueprint.BlueprintClass;
import revxrsal.bubbles.blueprint.Blueprints;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A configuration that supports comments. Set comments with
 * {@link #setComments(Map)}
 */
public final class CommentedConfiguration {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(Blueprints.gsonFactory())
            .registerTypeAdapterFactory(EnumTypeAdapterFactory.get())
            .disableHtmlEscaping()
            .create();

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    private static final Pattern NEW_LINE = Pattern.compile("\n");

    private final Yaml yaml;
    private final Map<String, String> configComments = new HashMap<>();
    private final Gson gson;
    private final Path file;
    private JsonObject data;

    CommentedConfiguration(Path file, Gson gson) {
        this.gson = gson;
        this.file = file;
        DumperOptions options = new DumperOptions();
        setProcessComments(options, false);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
    }

    /**
     * Loads the content of this configuration
     */
    @SneakyThrows
    public void load() {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            Map<String, Object> map = yaml.load(reader);
            JsonElement element = gson.toJsonTree(map, MAP_TYPE);
            if (element.isJsonNull())
                data = new JsonObject();
            else
                data = element.getAsJsonObject();
        }
    }

    /**
     * Sets the comment of the given path.
     *
     * @param path    The comment path. Subkeys are delimited by '.', and array entries have 0
     *                as their parent.
     * @param comment The comment
     */
    public void setComment(@NotNull String path, @NotNull String comment) {
        this.configComments.put(path, comment);
    }

    /**
     * Sets the comments of this configuration file.
     *
     * @param comments The comments to set. Supports multiple lines (use \n as a spacer).
     */
    public void setComments(@NotNull Map<String, String> comments) {
        this.configComments.clear();
        this.configComments.putAll(comments);
    }

    /**
     * Saves this configuration file with comments set with {@link #setComments(Map)}.
     */
    @SneakyThrows
    public void save() {
        Map<String, Object> dataToMap = gson.fromJson(data, MAP_TYPE);
        String simpleDump = yaml.dump(dataToMap);
        String[] aLines = NEW_LINE.split(simpleDump);
        List<String> lines = new ArrayList<>();
        Collections.addAll(lines, aLines);
        StringReader reader = new StringReader(simpleDump);
        Iterable<Event> events = yaml.parse(reader);
        handleEvents(events.iterator(), lines); // terribly inefficient way but I can't care less lol
        if (!lines.isEmpty()) {
            String first = lines.get(0);
            if (Character.isWhitespace(first.charAt(0))) {
                lines.set(0, first.substring(1));
            }
        }
        Files.write(file, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * Load a config from a file
     *
     * @param file The file to load the config from.
     * @param gson The GSON instance to deserialize with
     * @return A new instance of CommentedConfiguration
     */
    public static CommentedConfiguration from(Path file, Gson gson) {
        //Creating a blank instance of the config.
        return new CommentedConfiguration(file, gson);
    }

    /**
     * Load a config from a file
     *
     * @param file The file to load the config from.
     * @return A new instance of CommentedConfiguration
     */
    public static CommentedConfiguration from(Path file) {
        //Creating a blank instance of the config.
        return from(file, GSON);
    }

    private static @Nullable Method SET_PROCESS_COMMENTS;

    static {
        try {
            SET_PROCESS_COMMENTS = DumperOptions.class.getDeclaredMethod("setProcessComments", boolean.class);
            SET_PROCESS_COMMENTS.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
        }
    }

    @SneakyThrows
    private static void setProcessComments(@NotNull DumperOptions options, boolean process) {
        if (SET_PROCESS_COMMENTS != null)
            // Invoke the method with the desired parameter
            SET_PROCESS_COMMENTS.invoke(options, process);
    }

    public <T> T get(String key, Type type) {
        return gson.fromJson(data.get(key), type);
    }

    public <T> T getAs(Type type) {
        return gson.fromJson(data, type);
    }

    public <T> T get(String key, Class<T> type) {
        return get(key, (Type) type);
    }

    public void set(String key, Object v) {
        data.add(key, gson.toJsonTree(v));
    }

    public void set(String key, Object v, Type type) {
        data.add(key, gson.toJsonTree(v, type));
    }

    public boolean contains(String path) {
        return data.has(path);
    }

    public void setData(JsonObject jsonObject) {
        this.data = jsonObject;
    }

    public JsonObject getData() {
        return data;
    }

    private void handleEvents(Iterator<Event> eventsI, List<String> lines) {
        PeekingIterator<Event> events = PeekingIterator.from(eventsI);
        LinkedList<String> path = new LinkedList<>();
        Set<String> commentsAdded = new HashSet<>();
        boolean expectKey = true;
        boolean lastWasScalar = false;
        int offset = 0;
        while (events.hasNext()) {
            Event event = events.next();
            if (event instanceof DocumentStartEvent) {
                expectKey = true;
            }
            if (event instanceof MappingStartEvent) {
                expectKey = true;
            } else if (event instanceof MappingEndEvent) {
                expectKey = false;
                path.pollLast();
            } else if (event instanceof ScalarEvent) {
                if (expectKey) {
                    expectKey = false;
                    if (lastWasScalar)
                        path.removeLast();
                    path.add(((ScalarEvent) event).getValue());
                } else {
                    expectKey = true;
                }
            }
            if (event instanceof SequenceStartEvent) {
                path.add(BlueprintClass.ARRAY_INDEX);
            } else if (event instanceof SequenceEndEvent) {
                path.pollLast();
                expectKey = true;
                if (events.hasNext()) {
                    Event next = events.peek();
                    if (next instanceof ScalarEvent) {
                        path.pollLast();
                    }
                }
            }

            lastWasScalar = event instanceof ScalarEvent;
            String commentPath = String.join(".", path);
            String comment = configComments.get(commentPath);
            if (comment != null && commentsAdded.add(commentPath)) {
                lines.add(event.getStartMark().getLine() + (offset++), comment);
            }
        }
    }

    /**
     * Legally stolen and re-adapted from Guava's PeekingImpl class
     * <p>
     * A {@link Iterator} wrapper that allows peeking at the next element
     * without advancing the iterator.
     *
     * @param <E> The element type
     */
    private static final class PeekingIterator<E> implements Iterator<E> {

        private final @NotNull Iterator<? extends E> iterator;
        private @Nullable E peekedElement;
        private boolean hasPeeked;

        PeekingIterator(@NotNull Iterator<? extends E> iterator) {
            this.iterator = iterator;
        }

        /**
         * Returns {@code true} if there are more elements in the iteration.
         *
         * @return {@code true} if the iteration has more elements.
         */
        public boolean hasNext() {
            return this.hasPeeked || this.iterator.hasNext();
        }

        /**
         * Returns the next element in the iteration. If peeked, returns the peeked element.
         *
         * @return The next element.
         * @throws java.util.NoSuchElementException If no more elements.
         */
        public E next() {
            if (!this.hasPeeked) {
                return this.iterator.next();
            } else {
                E result = this.peekedElement;
                this.hasPeeked = false;
                this.peekedElement = null;
                return result;
            }
        }

        /**
         * Removes the last element returned by {@code next()}.
         *
         * @throws IllegalStateException If {@code peek()} was called after the last {@code next()}.
         */
        public void remove() {
            if (hasPeeked)
                throw new IllegalStateException("Can't remove after you've peeked at next");
            this.iterator.remove();
        }

        /**
         * Peeks at the next element without advancing the iterator.
         *
         * @return The next element.
         * @throws java.util.NoSuchElementException If no more elements.
         */
        public E peek() {
            if (!this.hasPeeked) {
                this.peekedElement = this.iterator.next();
                this.hasPeeked = true;
            }

            return this.peekedElement;
        }

        /**
         * Creates a new {@code PeekingIterator} from the given iterator.
         *
         * @param <E>      The type of elements.
         * @param iterator The iterator to wrap.
         * @return A new {@code PeekingIterator}.
         */
        public static <E> @NotNull PeekingIterator<E> from(@NotNull Iterator<E> iterator) {
            return new PeekingIterator<>(iterator);
        }
    }
}