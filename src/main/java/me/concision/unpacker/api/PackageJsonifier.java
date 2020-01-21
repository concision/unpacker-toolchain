package me.concision.unpacker.api;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.bson.Document;

/**
 * Parses raw packages to a JSON document structure
 *
 * @author Concision
*/
public class PackageJsonifier {
    // regexes for ease of writng parser
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^([^\\[.]+)((?:\\.[^\\[.]+)*)((?:\\[(?:\\w+)])*)=(.+)$");
    private static final Pattern SUBKEY_PATTERN = Pattern.compile("\\[(\\w+)]");
    private static final Pattern INLINE_ARRAY_PATTERN = Pattern.compile("^\\{(?:[^\"]+|\"(?:[^\"]+)?\")(?:,(?:[^\"]+|\"(?:[^\"]+)?\"))+}$");
    private static final Pattern INLINE_ARRAY_FINDER_PATTERN = Pattern.compile("(?:^\\{)?([^\",]+|\"(?:[^\"]+)?\")[,}]");

    /**
     * Parses a raw package chunk into a document structure
     *
     * @param packageText raw chunk contents
     * @return document structure
     */
    public static Document parse(@NonNull String packageText) {
        // version 14 parsing
        String[] split = packageText.split("[\\r\\n]+");
        Deque<String> lines = new LinkedList<>();
        for (String line : split) {
            String trim = line.trim();
            if (trim.startsWith(">>>>") || trim.startsWith("<<<<")) continue;
            lines.add(trim);
        }

        return parseMultilineMap(lines, true);
    }


    // map

    /**
     * Reads the equivalent of a {@link Document}
     *
     * @param lines remaining lines in chunk
     * @param root  whether this is the root document
     * @return a parsed {@link Document}
     */
    private static Document parseMultilineMap(Deque<String> lines, boolean root) {
        Document parent = new Document();
        // read
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

            // structure end
            if (line.startsWith("}")) {
                break;
            }

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
                set(parent, subKeys, realValue);
            }
        }
        return parent;
    }


    // list

    /**
     * Parses a multi-line array structure
     *
     * @param lines remaining lines in chunk
     * @return a parsed {@link List}
     */
    private static List parseMultilineArray(Deque<String> lines) {
        List list = new ArrayList();
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
     * Parses an inline array
     *
     * @param inlineArray raw inline array
     * @return parsed {@link List}
     */
    private static List parseInlineArray(String inlineArray) {
        List list = new ArrayList();

        // strip brackets
        for (Matcher subkeyMatcher = INLINE_ARRAY_FINDER_PATTERN.matcher(inlineArray); subkeyMatcher.find(); ) {
            list.add(parseInlinedValue(subkeyMatcher.group(1)));
        }

        return list;
    }

    /**
     * Parses a value in a map or list
     *
     * @param lines   remaining lines
     * @param value   raw string value
     * @param inArray whether the value is in an array or not
     * @return parsed object
     */
    private static Object parseValue(Deque<String> lines, String value, boolean inArray) {
        // check for map/array
        if (value.equals("{}")) {
            return new Document();
        } else if (value.equals("{")) {
            // do lookahead, check if map
            if (KEY_VALUE_PATTERN.matcher(lines.peekFirst()).matches()) {
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
    private static Object parseInlinedValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}
        return value;
    }


    // sets

    /**
     * Sets a deep key-value mapping in a map
     *
     * @param absoluteParent parent map
     * @param keys           recursive keys (e.g. map['x']['y'][0] = ... is ['x', 'y', '0']
     * @param value          parsed value to set
     */
    private static void set(Document absoluteParent, Deque<String> keys, Object value) {
        Object nextParent = absoluteParent;

        //
        while (1 < keys.size()) {
            String currentKey = keys.pollFirst();
            String nextKey = keys.peekFirst();

            // next
            boolean isNextNumber = false;
            try {
                Integer.parseInt(nextKey);
                isNextNumber = true;
            } catch (NumberFormatException ignored) {}

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
                        List replacedChild = (List) child;
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
                List parent = (List) nextParent;
                int currentIndex = Integer.parseInt(currentKey);
                Object child = currentIndex < parent.size() ? parent.get(currentIndex) : null;

                // create new child if necessary
                if (child == null) {
                    if (isNextNumber) {
                        child = new ArrayList<>();
                    } else {
                        child = new Document();
                    }

                    set(parent, currentIndex, child);
                } else {
                    // if next tree is an array, but our key isn't an index
                    if (child instanceof List && !isNextNumber) {
                        // upgrade to map
                        List replacedChild = (List) child;
                        Document replacement = new Document();

                        for (int i = 0; i < replacedChild.size(); i++) {
                            Object element = replacedChild.get(i);
                            if (element != null) {
                                replacement.put(String.valueOf(i), element);
                            }
                        }

                        set(parent, currentIndex, child);
                        child = replacement;
                    }
                }

                nextParent = child;
            }
        }

        // insert last
        String lastKey = keys.pollFirst();
        if (nextParent instanceof Document) {
            ((Document) nextParent).put(lastKey, value);
        } else if (nextParent instanceof List) {
            set((List) nextParent, Integer.parseInt(lastKey), value);
        }
    }

    /**
     * Sets a position to a value in a list
     *
     * @param list  list to set
     * @param index index to set, if it exceeeds list size, nulls are added
     * @param value element value
     */
    private static void set(List list, int index, Object value) {
        while (list.size() < index) list.add(null);
        if (list.size() == index) {
            list.add(value);
        } else {
            list.set(index, value);
        }
    }
}