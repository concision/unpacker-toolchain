package me.concision.unnamed.unpacker.cli.paths;

import lombok.NonNull;

import java.util.regex.Pattern;

/**
 * A glob to {@link Pattern} compiler.
 * Source: https://stackoverflow.com/a/17369948
 */
class GlobCompiler {
    /**
     * Converts a standard POSIX Shell globbing pattern into a regular expression
     * pattern. The result can be used with the standard {@link java.util.regex} API to
     * recognize strings which match the glob pattern.
     * <p/>
     * See also, the POSIX Shell language:
     * http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
     *
     * @param pattern A glob pattern.
     * @return A regex pattern to recognize the given glob pattern.
     */
    public static String convertGlobToRegex(@NonNull String pattern) {
        StringBuilder builder = new StringBuilder(pattern.length());

        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] characters = pattern.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            char ch = characters[i];
            switch (ch) {
                case '\\':
                    if (characters.length <= ++i) {
                        builder.append('\\');
                    } else {
                        char next = characters[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                builder.append('\\');
                            default:
                                builder.append('\\');
                        }
                        builder.append(next);
                    }
                    break;
                case '*':
                    if (inClass == 0)
                        builder.append(".*");
                    else
                        builder.append('*');
                    break;
                case '?':
                    if (inClass == 0)
                        builder.append('.');
                    else
                        builder.append('?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i + 1;
                    builder.append('[');
                    break;
                case ']':
                    inClass--;
                    builder.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                        builder.append('\\');
                    builder.append(ch);
                    break;
                case '!':
                    if (firstIndexInClass == i)
                        builder.append('^');
                    else
                        builder.append('!');
                    break;
                case '{':
                    inGroup++;
                    builder.append('(');
                    break;
                case '}':
                    inGroup--;
                    builder.append(')');
                    break;
                case ',':
                    if (inGroup > 0)
                        builder.append('|');
                    else
                        builder.append(',');
                    break;
                default:
                    builder.append(ch);
            }
        }

        return builder.toString();
    }
}
