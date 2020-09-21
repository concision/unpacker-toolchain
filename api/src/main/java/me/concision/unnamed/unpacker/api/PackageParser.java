package me.concision.unnamed.unpacker.api;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;
import me.concision.unnamed.decacher.api.CacheDecompressionInputStream;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Parses package structures as {@link PackageEntry}s from a raw binary "Packages.bin" file from any compliant
 * underlying {@link InputStream}.
 *
 * @author Concision
 */
@UtilityClass
public class PackageParser {
    /**
     * Parses package entries as {@link PackageEntry}s from a decompressed {@link InputStream} containing
     * "Packages.bin".
     * <p>
     * Note: {@link InputStream} should be decompressed prior; this can be done by layering the stream through a
     * {@link CacheDecompressionInputStream} prior.
     * <p>
     * Note: The underlying {@link InputStream} should be internally buffered for performance reasons (e.g. single byte
     * {@link InputStream#read()}s); this can be done by layering the stream through a {@link BufferedInputStream}
     * prior.
     * <p>
     * Note: {@link InputStream} will not necessarily be fully consumed.
     * <p>
     * Note: An invalid format could cause an expected over-consumption of the underlying {@link InputStream}; the
     * {@param inputStream} should be limited to a certain number of bytes if necessary, before invoking this method.
     *
     * @param inputStream a decompressed Packages.bin {@link InputStream}
     * @return a {@link Deque<PackageEntry>} of all package entries
     * @throws IOException if an underlying I/O exception occurs
     */
    @SuppressWarnings("DuplicatedCode")
    public Deque<PackageEntry> parseStream(@NonNull InputStream inputStream) throws IOException {
        // deserialized package entries
        Deque<PackageEntry> entries = new LinkedList<>();

        // wrap with a DataInputStream for ease of reading
        try (DataInputStream stream = new DataInputStream(inputStream)) {
            // unknown classified header bytes
            skipNBytes(stream, 17);

            // read file format version
            int patch = stream.readInt();
            int minor = stream.readInt();
            int major = stream.readInt();

            // determine length of an unknown integer (file format changes)
            int unknownLength;
            // equivalent to org.semver.Version: 0 <= new Version(major, minor, patch).compareTo(new Version(0, 1, 29)
            if (0 <= major && (major != 0 || minor != 1 || 29 <= patch)) {
                unknownLength = 1;
            } else {
                unknownLength = 4;
            }

            // read various header structures
            int structureCount = Integer.reverseBytes(stream.readInt());
            for (int i = 0; i < structureCount; i++) {
                // string length
                int stringLength = Integer.reverseBytes(stream.readInt());
                // string is also terminated by NUL
                skipNBytes(stream, stringLength);

                // unknown integer
                skipNBytes(stream, unknownLength);
            }


            // total length of all chunks, including the NUL-terminating bytes for strings
            int totalChunkSize = Integer.reverseBytes(stream.readInt());

            // entry contents chunk pool; this is the raw LUA table contents of packages, without an assigned path
            List<String> chunks = new ArrayList<>();

            // chunk string builder
            StringBuilder chunkBuilder = new StringBuilder();
            // read all null-terminated string chunks
            for (int i = 0; i < totalChunkSize; i++ /* for null terminator */) {
                // read next null-terminated string
                for (byte in; (in = stream.readByte()) != 0x00 && i < totalChunkSize; i++ /* for each char read */) {
                    chunkBuilder.append((char) in);
                }
                // add string contents to chunk pool
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


            // path string building buffer; resized on demand, if necessary
            byte[] buffer = new byte[512];

            // assign path/names to chunks
            for (int i = 0; i < expectedChunks; i++) {
                String chunk = chunks.get(i);

                // read absolute package directory path
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
                skipNBytes(stream, 5);

                // "base package"; no idea what it is, not really useful (e.g. @\n, @\b, A\n, B\n, etc).
                {
                    int length = Integer.reverseBytes(stream.readInt());
                    // string
                    skipNBytes(stream, length);
                }

                // unknown bytes
                skipNBytes(stream, 4);

                // add to package chunks
                entries.add(new PackageEntry(
                        path,
                        name,
                        chunk
                ));
            }
        }

        return entries;
    }

    /**
     * Implementation of {@link InputStream#skipNBytes(long)} for JREs before Java 12.
     * <p>
     * Skips over and discards exactly {@code n} bytes of data from this input
     * stream.  If {@code n} is zero, then no bytes are skipped.
     * If {@code n} is negative, then no bytes are skipped.
     * Subclasses may handle the negative value differently.
     *
     * <p> This method blocks until the requested number of bytes have been
     * skipped, end of file is reached, or an exception is thrown.
     *
     * <p> If end of stream is reached before the stream is at the desired
     * position, then an {@code EOFException} is thrown.
     *
     * <p> If an I/O error occurs, then the input stream may be
     * in an inconsistent state. It is strongly recommended that the
     * stream be promptly closed if an I/O error occurs.
     *
     * @param n the number of bytes to be skipped.
     * @throws EOFException if end of stream is encountered before the
     *                      stream can be positioned {@code n} bytes beyond its position
     *                      when this method was invoked.
     * @throws IOException  if the stream cannot be positioned properly or
     *                      if an I/O error occurs.
     * @implNote Subclasses are encouraged to provide a more efficient implementation
     * of this method.
     * @implSpec If {@code n} is zero or negative, then no bytes are skipped.
     * If {@code n} is positive, the default implementation of this method
     * invokes {@link InputStream#skip(long) skip()} with parameter {@code n}.  If the
     * return value of {@code skip(n)} is non-negative and less than {@code n},
     * then {@link InputStream#read()} is invoked repeatedly until the stream is {@code n}
     * bytes beyond its position when this method was invoked or end of stream
     * is reached.  If the return value of {@code skip(n)} is negative or
     * greater than {@code n}, then an {@code IOException} is thrown.  Any
     * exception thrown by {@code skip()} or {@code read()} will be propagated.
     * @see java.io.InputStream#skip(long)
     */
    private static void skipNBytes(InputStream inputStream, long n) throws IOException {
        if (0 < n) { // skipped too few bytes
            // read until requested number skipped or EOS reached
            while (0 < n && inputStream.read() != -1) {
                n--;
            }
            // if not enough skipped, then EOFE
            if (n != 0) {
                throw new EOFException();
            }
        } else if (0 != n) { // skipped negative or too many bytes
            throw new IOException("Unable to skip exactly");
        }
    }

    /**
     * Immutable package entry structure
     */
    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @SuppressWarnings("RedundantModifiersUtilityClassLombok") // compilation issue without static keyword
    public static class PackageEntry {
        /**
         * Package parent directory absolute path
         */
        String path;
        /**
         * Package entry name
         */
        String name;
        /**
         * Package entry LUA table
         */
        String contents;

        /**
         * Retrieves the package entry's full absolute path
         *
         * @return absolute package path
         */
        public String absolutePath() {
            return path + name;
        }
    }
}
