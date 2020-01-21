package me.concision.unpacker.api;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;
import org.semver.Version;

/**
 * Reads raw binary Packages.bin file.
 *
 * @author Concision
*/
public class PackageParser {
    /**
     * Parses a raw full input stream of Packages.bin
     *
     * @param inputStream Packages.bin input stream
     * @return a deserialized list of all packages
     * @throws IOException if an underlying IO exception occurs
     */
    public static <R extends List<PackageRecord> & Queue<PackageRecord>> R parsePackages(@NonNull InputStream inputStream) throws IOException {
        List<PackageRecord> chunkList = new LinkedList<>();

        // wrap with data input stream
        try (DataInputStream stream = new DataInputStream(inputStream)) {
            // unknown header bytes
            stream.skipBytes(17);

            int patch = stream.readInt();
            int minor = stream.readInt();
            int major = stream.readInt();

            int unknownLength;
            if (0 <= new Version(major, minor, patch).compareTo(new Version(0, 1, 29))) {
                unknownLength = 1;
            } else {
                unknownLength = 4;
            }

            // read header structures
            int structureCount = Integer.reverseBytes(stream.readInt());
            for (int i = 0; i < structureCount; i++) {
                // string length
                int stringLength = Integer.reverseBytes(stream.readInt());

                // string terminated by NUL
                stream.skipBytes(stringLength);

                // unknown integer
                stream.skipBytes(unknownLength);
            }


            // total number of bytes for every null-terminated chunk; null bytes are included in this count
            int totalChunkSize = Integer.reverseBytes(stream.readInt());

            // chunk pool
            List<String> chunks = new ArrayList<>();
            // chunk string builder
            StringBuilder chunkBuilder = new StringBuilder();

            // read null-terminated strings
            for (int i = 0; i < totalChunkSize; i++ /* for null terminator */) {
                // read next null-terminated string
                for (byte in; (in = stream.readByte()) != 0x00 && i < totalChunkSize; i++ /* each byte read */) {
                    chunkBuilder.append((char) in);
                }
                // add string to pool
                chunks.add(chunkBuilder.toString());
                // reset string builder
                chunkBuilder.setLength(0);
            }

            // read expected chunk count
            int expectedChunks = Integer.reverseBytes(stream.readInt());
            // sanity check
            if (expectedChunks != chunks.size()) {
                throw new RuntimeException("chunk count mismatch (expected: " + expectedChunks + ", deserialized: " + chunks.size() + ")");
            }


            // buffer
            byte[] buffer = new byte[4096];

            // assign path + names to chunks
            for (int i = 0; i < expectedChunks; i++) {
                String chunk = chunks.get(i);

                // read directory path
                String path;
                {
                    int length = Integer.reverseBytes(stream.readInt());
                    if (buffer.length < length) buffer = new byte[buffer.length];
                    stream.readFully(buffer, 0, length);
                    path = new String(buffer, 0, length);
                }


                // read package name
                String name;
                {
                    int length = Integer.reverseBytes(stream.readInt());
                    if (buffer.length < length) buffer = new byte[buffer.length];
                    stream.readFully(buffer, 0, length);
                    name = new String(buffer, 0, length);
                }

                // unknown bytes
                stream.skipBytes(5);

                // base package; no idea what it is, not really useful (looks like @\n, @\b, A\n, B\n, etc).
                {
                    int length = Integer.reverseBytes(stream.readInt());
                    // string
                    stream.skipBytes(length);
                }

                // unknown bytes
                stream.skipBytes(4);

                // add to package chunk pool
                chunkList.add(new PackageRecord(
                        path,
                        name,
                        chunk
                ));
            }
        }

        return (R) chunkList;
    }

    /**
     * Holds a parsed package and it's raw text
     */
    @Value
    public static class PackageRecord {
        /**
         * Package parent directory path
         */
        private final String path;
        /**
         * Package name
         */
        private final String name;
        /**
         * Raw package contents
         */
        @Wither
        private final String contents;

        /**
         * Retrieves the package's fully qualified path
         *
         * @return fully qualified package path
         */
        public String fullPath() {
            return path + name;
        }
    }
}