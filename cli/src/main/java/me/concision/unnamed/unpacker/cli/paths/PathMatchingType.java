package me.concision.unnamed.unpacker.cli.paths;

import lombok.NonNull;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Generates {@link Predicate<String>}s for path patterns
 *
 * @author Concision
 */
public enum PathMatchingType {
    /**
     * Case-insensitively matches a path.
     */
    LITERAL {
        @Override
        public Predicate<String> asPredicate(@NonNull String pattern) {
            return pattern::equalsIgnoreCase;
        }
    },
    /**
     * Matches a path with glob wildcards using a pre-compiled regex pattern (e.g. "/**\/xx", "**\/xyz").
     */
    GLOB {
        @Override
        public Predicate<String> asPredicate(@NonNull String pattern) {
            Pattern compiledPattern = Pattern.compile(GlobCompiler.convertGlobToRegex(pattern));
            return (path) -> compiledPattern.matcher(path).matches();
        }
    },
    /**
     * Matches a path with a regex {@link Pattern}.
     */
    REGEX {
        @Override
        public Predicate<String> asPredicate(@NonNull String pattern) {
            Pattern compiledPattern;
            try {
                compiledPattern = Pattern.compile(pattern);
            } catch (PatternSyntaxException exception) {
                throw new IllegalArgumentException("invalid regex pattern: " + pattern);
            }
            return (path) -> compiledPattern.matcher(path).matches();
        }
    },
    /**
     * Matches a path using any type of {@link PathMatchingType} by specifying a pattern in the format of "type:pattern"
     * (e.g. "literal:/xyz/...").
     */
    MIXED {
        @Override
        public Predicate<String> asPredicate(@NonNull String pattern) {
            int separatorPosition = pattern.indexOf(":");
            if (separatorPosition < 0)
                throw new IllegalArgumentException("invalid pattern; must be in the format MATCHING_TYPE:pattern (e.g. 'literal:/xyz/'): " + pattern);

            String type = pattern.substring(0, separatorPosition);
            String delegatedPattern = pattern.substring(separatorPosition + 1);

            PathMatchingType pathMatchingType = MATCHING_TYPES.get(type);
            if (pathMatchingType == null) throw new IllegalArgumentException("unknown pattern matching type: " + type);

            return pathMatchingType.asPredicate(delegatedPattern);
        }
    };

    /**
     * Case-insensitive {@link PathMatchingType} lookup table
     */
    private static final Map<String, PathMatchingType> MATCHING_TYPES = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        for (PathMatchingType type : values()) {
            MATCHING_TYPES.put(type.name(), type);
        }
    }

    /**
     * Generates a predicate from a string pattern for the current {@link PathMatchingType}
     *
     * @param pattern a matching type pattern
     * @return a {@link Predicate} that matches package paths
     */
    public abstract Predicate<String> asPredicate(@NonNull String pattern);
}
