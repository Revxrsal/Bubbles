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

import com.google.gson.*;
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
import java.io.BufferedWriter;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.*;

/**
 * A configuration that supports comments. Set comments with
 * {@link #setComments(Map)}
 */
public final class CommentedConfiguration {

    /**
     * Shared Gson instance with custom type adapters
     */
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(Blueprints.gsonFactory())
            .registerTypeAdapterFactory(EnumTypeAdapterFactory.get())
            .disableHtmlEscaping()
            .create();

    /**
     * Type reference for deserializing maps with string keys and object values.
     */
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    /**
     * Pattern for matching newline characters.
     */
    private static final Pattern NEW_LINE = Pattern.compile("\n");

    /**
     * YAML processor instance for reading and writing YAML data.
     */
    private final Yaml yaml;

    /**
     * A map storing comments associated with specific configuration keys.
     */
    private final Map<String, String> configComments = new HashMap<>();

    /**
     * Gson instance for serializing and deserializing JSON data.
     */
    private final Gson gson;

    /**
     * Path to the configuration file.
     */
    private final Path file;

    /**
     * The JSON representation of the configuration data.
     */
    private JsonElement data = JsonNull.INSTANCE;

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
            data = gson.toJsonTree(map, MAP_TYPE);
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
        if (configComments.isEmpty()) {
            try (BufferedWriter writer = Files.newBufferedWriter(file, CREATE, TRUNCATE_EXISTING, WRITE)) {
                yaml.dump(dataToMap, writer);
            }
            return;
        }
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
        Files.write(file, lines, CREATE, TRUNCATE_EXISTING, WRITE);
    }

    /**
     * Load a config from a file
     *
     * @param file The file to load the config from.
     * @param gson The GSON instance to deserialize with
     * @return A new instance of CommentedConfiguration
     */
    public static @NotNull CommentedConfiguration from(@NotNull Path file, @NotNull Gson gson) {
        //Creating a blank instance of the config.
        return new CommentedConfiguration(file, gson);
    }

    /**
     * Load a config from a file
     *
     * @param file The file to load the config from.
     * @return A new instance of CommentedConfiguration
     */
    public static @NotNull CommentedConfiguration from(@NotNull Path file) {
        //Creating a blank instance of the config.
        return from(file, GSON);
    }

    /**
     * Retrieves the value for a key and deserializes it to the specified type.
     *
     * @param key  The key to retrieve the value for.
     * @param type The type to deserialize the value into.
     * @param <T>  The type of the returned value.
     * @return The deserialized value.
     */
    public <T> T get(@NotNull String key, @NotNull Type type) {
        return gson.fromJson(data.getAsJsonObject().get(key), type);
    }

    /**
     * Deserializes the entire configuration data to the specified type.
     *
     * @param type The type to deserialize the data into.
     * @param <T>  The type of the returned value.
     * @return The deserialized data.
     */
    public <T> T getAs(@NotNull Type type) {
        return gson.fromJson(data, type);
    }

    /**
     * Retrieves the value for a key and deserializes it to the specified class.
     *
     * @param key  The key to retrieve the value for.
     * @param type The class to deserialize the value into.
     * @param <T>  The type of the returned value.
     * @return The deserialized value.
     */
    public <T> T get(@NotNull String key, @NotNull Class<T> type) {
        return get(key, (Type) type);
    }

    /**
     * Sets a value for a key using JSON serialization.
     *
     * @param key The key to set the value for.
     * @param v   The value to set.
     */
    public void set(@NotNull String key, @NotNull Object v) {
        data.getAsJsonObject().add(key, gson.toJsonTree(v));
    }

    /**
     * Sets a value for a key using JSON serialization with a specific type.
     *
     * @param key  The key to set the value for.
     * @param v    The value to set.
     * @param type The type used for serialization.
     */
    public void set(@NotNull String key, @NotNull Object v, @NotNull Type type) {
        data.getAsJsonObject().add(key, gson.toJsonTree(v, type));
    }

    /**
     * Checks if the configuration contains a value for the given path.
     *
     * @param path The path to check.
     * @return {@code true} if the path exists, {@code false} otherwise.
     */
    public boolean contains(@NotNull String path) {
        return data.getAsJsonObject().has(path);
    }

    /**
     * Replaces the configuration data with the given JSON object.
     *
     * @param data The new JSON object to set.
     */
    public void setData(@NotNull JsonObject data) {
        this.data = data;
    }

    /**
     * Replaces the configuration data with the given JSON object.
     *
     * @param data The new JSON object to set.
     */
    public void setData(@NotNull Object data, Type type) {
        this.data = gson.toJsonTree(data, type);
    }

    /**
     * Replaces the configuration data with the given JSON object.
     *
     * @param data The new JSON object to set.
     */
    public void setData(@NotNull Object data) {
        this.data = gson.toJsonTree(data);
    }

    /**
     * Retrieves the entire configuration data as a JSON object.
     *
     * @return The configuration data.
     */
    public JsonElement getData() {
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
     * Reflective access to the `setProcessComments` method in {@link DumperOptions}.
     */
    private static @Nullable Method SET_PROCESS_COMMENTS;

    static {
        try {
            // Attempt to retrieve the private `setProcessComments` method.
            SET_PROCESS_COMMENTS = DumperOptions.class.getDeclaredMethod("setProcessComments", boolean.class);
            SET_PROCESS_COMMENTS.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
            // Ignored as the method may not exist in older versions.
        }
    }

    /**
     * Sets the `processComments` flag on the given {@link DumperOptions} instance.
     *
     * @param options The {@link DumperOptions} instance.
     * @param process The value to set for `processComments`.
     */
    @SneakyThrows
    private static void setProcessComments(@NotNull DumperOptions options, boolean process) {
        if (SET_PROCESS_COMMENTS != null)
            SET_PROCESS_COMMENTS.invoke(options, process);
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