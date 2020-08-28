package me.concision.unnamed.unpacker.api;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts LUA text tables from {@link PackageEntry#contents()} to BSON/JSON documents. The LUA text table is not very
 * developer-friendly for building applications, as it does not have a plethora of parsing libraries (unlike JSON).
 *
 * @author Concision
 */
@SuppressWarnings("DuplicatedCode")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Lua2JsonConverter {
    // helpful regular expressions for simplifying parsing
    /**
     * Matches assignment with sub-keys (e.g. "value[0][1].x=...").
     */
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^([^\\[.]+)((?:\\.[^\\[.]+)*)((?:\\[(?:\\w+)])*)=(.+)$");
    /**
     * Matches each occurrence of a valid sub-key from the sub-key capture group in {@link #KEY_VALUE_PATTERN}.
     */
    private static final Pattern SUBKEY_PATTERN = Pattern.compile("\\[(\\w+)]");
    /**
     * Matches if a parsed value during assignment is a single-line ("inline") array (e.g. "{1, \"2\", 3}").
     */
    private static final Pattern INLINE_ARRAY_PATTERN = Pattern.compile("^\\{(?:[^\"]+|\"(?:[^\"]+)?\")(?:,(?:[^\"]+|\"(?:[^\"]+)?\"))+}$");
    /**
     * Matches individual elements in an single-line ("inline") array value. Applied when a value matches
     * {@link #INLINE_ARRAY_PATTERN}.
     */
    private static final Pattern INLINE_ARRAY_FINDER_PATTERN = Pattern.compile("(?:^\\{)?([^\",]+|\"(?:[^\"]+)?\")[,}]");

    // cached JSONifier structures to reduce memory footprint; if more flags are added, these will be removed
    private static final Lua2JsonConverter JSONIFIER_PARSE_STRINGS_TRUE = new Lua2JsonConverter(true);
    private static final Lua2JsonConverter JSONIFIER_PARSE_STRINGS_FALSE = new Lua2JsonConverter(false);

    /**
     * Indicates to parser that LUA table string literals should be treated the same as ENUMs or package paths
     * (e.g. if set to {@code true}, a LUA literal string of "\"content\"" will be converted to "content").
     */
    private final boolean convertStringLiterals;

    /**
     * Parses a raw package chunk into a document structure
     *
     * @param packageText  raw chunk contents
     * @param parseStrings sets {@link #convertStringLiterals} flag
     * @return document structure
     */
    public static Document parse(@NonNull String packageText, boolean parseStrings) {
        return (parseStrings ? JSONIFIER_PARSE_STRINGS_TRUE : JSONIFIER_PARSE_STRINGS_FALSE).parse(packageText);
    }

    /**
     * Converts raw LUA text tables to an equivalent BSON/JSON document structure.
     *
     * @param packageText a raw {@link PackageEntry#contents()} value
     * @return a 1-to-1 mapping of {@param packageText} to a BSON/JSON document structure
     */
    public Document parse(@NonNull String packageText) {
        // version 14 parsing
        Deque<String> lines = Arrays.stream(packageText.split("[\\r\\n]+"))
                .map(String::trim)
                .filter(line -> !line.startsWith(">>>>") && !line.startsWith("<<<<"))
                .collect(Collectors.toCollection(LinkedList::new));

        return parseMultilineMap(lines, true);
    }


    // map

    /**
     * Parses a (possibly multiline) LUA table structure as the equivalent of a {@link Document}.
     *
     * @param lines remaining lines in chunk
     * @param root  indicates this is the root LUA table, invoked from {@link #parse(String)}
     * @return an equivalent {@link Document}
     */
    private Document parseMultilineMap(Deque<String> lines, boolean root) {
        Document parent = new Document();

        // read until out of lines
        while (true) {
            String line = lines.pollFirst();

            // EOF check
            if (line == null) {
                if (!root) {
                    throw new IllegalStateException("EOF while parsing object");
                }
                break;
            }
            // skip empty lines
            if (line.isEmpty()) {
                continue;
            }

            // detect structure end
            if (line.startsWith("}")) {
                break;
            }

            // extract an assignment operation
            Matcher matcher = KEY_VALUE_PATTERN.matcher(line);
            // exception if not key => value
            if (!matcher.find()) {
                continue;
            }

            // key
            String key = matcher.group(1);
            Deque<String> subKeys = new LinkedList<>();
            subKeys.add(key);
            String value = matcher.group(4);

            // build subkey list
            for (String subKey : matcher.group(2).split("\\.")) {
                if (!subKey.isEmpty()) {
                    subKeys.addLast(subKey);
                }
            }
            for (Matcher subkeyMatcher = SUBKEY_PATTERN.matcher(matcher.group(3)); subkeyMatcher.find(); ) {
                subKeys.add(subkeyMatcher.group(1));
            }

            // parse value
            Object realValue = parseValue(lines, value, false);

            // set value
            if (!key.startsWith("$")) {
                assign(parent, subKeys, realValue);
            }
        }

        return parent;
    }


    // list

    /**
     * Parses a multi-line LUA table array structure as a {@link List}.
     *
     * @param lines remaining lines in chunk
     * @return an equivalent {@link List}
     */
    private List<Object> parseMultilineArray(Deque<String> lines) {
        List<Object> list = new ArrayList<>();
        while (true) {
            String line = lines.pollFirst();
            if (line == null) {
                throw new IllegalStateException("EOF while parsing object");
            }
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("}")) {
                break;
            }

            if (line.contains("=")) {
                Matcher matcher = KEY_VALUE_PATTERN.matcher(line);
                // exception if not key => value
                if (matcher.find()) {
                    // skip line
                    lines.addFirst(line);
                    break;
                }
            }

            list.add(parseValue(lines, line, true));
        }
        return list;
    }

    /**
     * Parses a single-line ("inline") LUA table array structure as a {@link List}.
     *
     * @param inlineArray raw inline array
     * @return an equivalent {@link List}
     */
    private List<Object> parseInlineArray(String inlineArray) {
        List<Object> list = new ArrayList<>();

        // strip brackets
        for (Matcher subkeyMatcher = INLINE_ARRAY_FINDER_PATTERN.matcher(inlineArray); subkeyMatcher.find(); ) {
            list.add(parseInlinedValue(subkeyMatcher.group(1)));
        }

        return list;
    }

    /**
     * Context-aware parse of a LUA value that may be several lines into its BSON/JSON equivalent or string literal.
     *
     * @param lines   remaining lines in chunk
     * @param value   raw string value
     * @param inArray indicates the value being parsed in an array
     * @return an equivalent BSON/JSON structure or literal value
     */
    private Object parseValue(Deque<String> lines, String value, boolean inArray) {
        // check for map/array
        if (value.equals("{}")) {
            return new Document();
        } else if (value.equals("{")) {
            // skip blank lines to ensure accuracy of lookahead parse
            String line;
            while ((line = lines.peekFirst()) != null && line.trim().isEmpty()) {
                lines.pollFirst();
            }
            if (line == null) {
                throw new IllegalStateException("EOF while parsing object");
            }

            // lookahead check if next entry is map (check if there is an assignment)
            if (KEY_VALUE_PATTERN.matcher(line).matches()) {
                return parseMultilineMap(lines, false);
            } else {
                return parseMultilineArray(lines);
            }
        } else {
            // strip comma
            if (inArray) {
                if (value.endsWith(",")) {
                    value = value.substring(0, value.length() - 1);
                }
            }

            // check if value is an inline array
            Matcher arrayMatcher = INLINE_ARRAY_PATTERN.matcher(value);
            if (arrayMatcher.find()) { // if array
                return parseInlineArray(value);
            } else {
                return parseInlinedValue(value);
            }
        }
    }

    /**
     * Attempts to parse an inline value as proper data type
     *
     * @param value raw value
     * @return parsed value
     */
    private Object parseInlinedValue(String value) {
        // try parsing as an integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }
        // try parsing as a double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
        }

        // convert string literals, if enabled
        if (convertStringLiterals) {
            if (value.startsWith("\"") && value.endsWith("\"") && 2 <= value.length()) {
                value = value.substring(1, value.length() - 1);
            }
        }

        // assume string
        return value;
    }


    // assignment

    /**
     * Deep assigns a key-value mapping in to a map.
     *
     * @param absoluteParent parent map
     * @param keys           recursive keys (e.g. map['x']['y'][0] = ... is ['map', 'x', 'y', '0']
     * @param value          a value to assign
     */
    private void assign(Document absoluteParent, Deque<String> keys, Object value) {
        Object nextParent = absoluteParent;

        while (2 <= keys.size()) {
            String currentKey = keys.pollFirst();
            String nextKey = keys.peekFirst();

            // next
            boolean isNextNumber = false;
            try {
                //noinspection ConstantConditions
                Integer.parseInt(nextKey);
                isNextNumber = true;
            } catch (NumberFormatException | NullPointerException ignored) {
            }

            if (nextParent instanceof Document) {
                Document parent = (Document) nextParent;
                Object child = parent.get(currentKey);

                // create new child if necessary
                if (child == null) {
                    if (isNextNumber) {
                        child = new ArrayList<>();
                    } else {
                        child = new Document();
                    }
                    parent.put(currentKey, child);
                } else {
                    // if next tree is an array, but our key isn't an index
                    if (child instanceof List && !isNextNumber) {
                        // upgrade to map
                        //noinspection unchecked
                        List<Object> replacedChild = (List<Object>) child;
                        Document replacement = new Document();

                        for (int i = 0; i < replacedChild.size(); i++) {
                            Object element = replacedChild.get(i);
                            if (element != null) {
                                replacement.put(String.valueOf(i), element);
                            }
                        }

                        parent.put(currentKey, replacement);
                        child = replacement;
                    }
                }

                nextParent = child;
            } else if (nextParent instanceof List) {
                //noinspection unchecked
                List<Object> parent = (List<Object>) nextParent;
                int currentIndex = Integer.parseInt(currentKey);
                Object child = currentIndex < parent.size() ? parent.get(currentIndex) : null;

                // create new child if necessary
                if (child == null) {
                    if (isNextNumber) {
                        child = new ArrayList<>();
                    } else {
                        child = new Document();
                    }

                    assign(parent, currentIndex, child);
                } else {
                    // if next tree is an array, but our key isn't an index
                    if (child instanceof List && !isNextNumber) {
                        // upgrade to map
                        //noinspection unchecked
                        List<Object> replacedChild = (List<Object>) child;
                        Document replacement = new Document();

                        for (int i = 0; i < replacedChild.size(); i++) {
                            Object element = replacedChild.get(i);
                            if (element != null) {
                                replacement.put(String.valueOf(i), element);
                            }
                        }

                        assign(parent, currentIndex, child);
                        child = replacement;
                    }
                }

                nextParent = child;
            }
        }

        // insert last
        String lastKey = keys.pollFirst();
        if (lastKey != null) {
            if (nextParent instanceof Document) {
                ((Document) nextParent).put(lastKey, value);
            } else if (nextParent instanceof List) {
                //noinspection unchecked
                assign((List<Object>) nextParent, Integer.parseInt(lastKey), value);
            }
        }
    }

    /**
     * Assigns a position to a value in a {@link List}. If {@code list.size() <= index}, nulls are inserted to pad the
     * list.
     *
     * @param list  list to set
     * @param index index to set; if it exceeds list size, nulls are added
     * @param value element value
     */
    private void assign(List<Object> list, int index, Object value) {
        while (list.size() < index) {
            list.add(null);
        }
        if (list.size() == index) {
            list.add(value);
        } else {
            list.set(index, value);
        }
    }
}
