package me.concision.unnamed.decacher.api;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Enables reading .cache file manifest entries as {@link CacheEntry}s from a corresponding .toc (table-of-contents)
 * file, from any compliant underlying {@link InputStream}. Only file entries (and not partial directory paths) are
 * available for reading with {@link #nextEntry()}.
 * Note that when opening cache entries from the corresponding .cache file, data must be passed through a
 * {@link CacheDecompressionInputStream} first.
 *
 * @author Concision
 */
public class TocStreamReader {
    /**
     * Currently expected 4-byte file header to recognize .toc files.
     */
    private static final byte[] TOC_HEADER = new byte[]{0x4E, (byte) 0xC6, 0x67, 0x18};


    /**
     * A wrapped {@link DataInputStream} of the passed {@link InputStream} specified in the constructor
     * {@link #TocStreamReader}; wrapping is useful for the functionality that {@link DataInputStream} supports.
     */
    private final DataInputStream stream;

    // header

    /**
     * The read 4-byte .toc file header; {@code null} indicates that the file header has not been read from the
     * underlying {@link InputStream} yet. The only expected (and supported) header is {@link #TOC_HEADER}.
     */
    private byte[] header;
    /**
     * The .toc file format version; if {@code this.header == null}, this valid has not been read from the underlying
     * {@link InputStream} yet. The only expected (and supported) version is 14 (0x0000000E).
     */
    private int version;

    // contents

    /**
     * Directories are declared sequentially in the .toc file, and are referenced by subsequent file/directory entries
     * by the order declared. Declared directories are inserted into this {@link List<String>} as their absolute paths.
     * <p>
     * Note: the root directory is never explicitly declared in the .toc file, but is referenced at index 0. Is it
     * inserted below in the following initializer.
     */
    private final List<String> directoryHierarchy = new ArrayList<>(Collections.singletonList(""));


    /**
     * Initializes a .toc entry reader from a compliant underlying {@link InputStream}.
     * <p>
     * Note: The underlying {@link InputStream} should be internally buffered for performance reasons (e.g. single byte
     * {@link InputStream#read()}s); this can be done by layering the stream through a {@link BufferedInputStream}
     * prior.
     * <p>
     * Note: An invalid format could cause an expected over-consumption of the underlying {@link InputStream}; the
     * {@param inputStream} should be limited to a certain number of bytes if necessary, before invoking this method.
     *
     * @param stream a {@link InputStream} containing a raw .toc file
     */
    public TocStreamReader(@NonNull InputStream stream) {
        // wrap stream, for ease of reading data
        this.stream = new DataInputStream(stream);
    }

    // header

    /**
     * Returns the .toc file header read from the file header (i.e. first 4-bytes).
     *
     * @return 4-byte .toc header
     * @throws IOException if an underlying I/O exception occurs
     */
    public byte[] header() throws IOException {
        if (header == null) {
            this.readHeader();
        }
        return header;
    }

    /**
     * Returns the .toc file version read from the file header (i.e. first 4-bytes as an integer).
     *
     * @return .toc version format
     * @throws IOException if an underlying I/O exception occurs
     */
    public int version() throws IOException {
        if (header == null) {
            this.readHeader();
        }
        return version;
    }

    // decoding

    /**
     * Reads a .toc file header from {@link #stream}. No header validations are performed until {@link #nextEntry()} is
     * executed, enabling safe reading of the values returned from {@link #header()} and {@link #version()}.
     *
     * @throws IOException if an underlying I/O exception occurs
     */
    private void readHeader() throws IOException {
        // read header magic value
        header = new byte[4];
        stream.readFully(header);

        // read header format version (little endian)
        version = Integer.reverseBytes(stream.readInt());
    }

    /**
     * Attempts to read the next available cache file entry, if any exists.
     *
     * @return the next {@link CacheEntry}, or {@code null} if no remaining entries are available from {@link #stream}
     * @throws IOException if an underlying I/O exception occurs
     */
    public CacheEntry nextEntry() throws IOException {
        // ensure header is read
        if (header == null) {
            this.readHeader();
        }
        // validate headers before processing content
        // verify magic value
        if (!Arrays.equals(header, TOC_HEADER)) {
            throw new IllegalArgumentException("header magic value mismatch " +
                    "(expected: 0x" + bytesToHex(TOC_HEADER) +
                    ", received: 0x" + bytesToHex(header) + ")"
            );
        }
        // verify version support
        if (version != 0x14) {
            throw new IllegalArgumentException("version not supported by TOC reader: " + version);
        }


        // attempt to read a cache entry until a file is read, or end of stream
        try {
            while (true) {
                // parse toc cache entry
                long offset = Long.reverseBytes(stream.readLong());
                long timeStamp = Long.reverseBytes(stream.readLong());
                int compressedFileSize = Integer.reverseBytes(stream.readInt());
                int uncompressedFileSize = Integer.reverseBytes(stream.readInt());
                // unknown 4 bytes; likely an integer
                stream.readInt();
                int depthId = Integer.reverseBytes(stream.readInt());
                String filename;
                {
                    byte[] filenameBuffer = new byte[64];
                    stream.readFully(filenameBuffer);
                    int nameEof = 0;
                    while (nameEof < filenameBuffer.length && filenameBuffer[nameEof] != '\0') {
                        nameEof++;
                    }
                    filename = new String(filenameBuffer, 0, nameEof);
                }

                // skip deleted files (timestamp of 0 indicates deleted)
                if (timeStamp == 0) continue;

                // retrieve parent directory by index
                String parent = directoryHierarchy.get(depthId);
                // construct the absolute path of the cache entry
                String absoluteFileName = parent + "/" + filename;

                // check if entry is a directory (offset is non-positive)
                if (offset == -1) {
                    // declare new directory in cache file
                    directoryHierarchy.add(absoluteFileName);
                    continue;
                }

                // construct cache entry
                return new CacheEntry(
                        absoluteFileName,
                        offset,
                        timeStamp,
                        compressedFileSize,
                        uncompressedFileSize
                );
            }
        } catch (EOFException ignored) {
            // end of stream, no cache entry available
            return null;
        }
    }

    /**
     * Skip through {@link CacheEntry}s to search for a specific file entry.
     * <p>
     * Note that this should NOT be more than once, as entries are skipped and the order of cache entries is not
     * necessarily guaranteed.
     *
     * @param absoluteFilename an absolute path filename (e.g. {@code "/Path/To/Filename.ext"})
     * @return an {@link Optional<CacheEntry>} of the cache entry
     * @throws IOException if an underlying I/O exception occurs
     */
    public Optional<CacheEntry> findEntry(@NonNull String absoluteFilename) throws IOException {
        for (CacheEntry entry; (entry = this.nextEntry()) != null; ) {
            if (entry.filename.equals(absoluteFilename)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }


    /**
     * Immutable cache entry structure
     */
    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CacheEntry {
        /**
         * Absolute path/filename
         */
        String filename;
        /**
         * Byte offset of the location of the file inside of the corresponding .cache file
         */
        long offset;
        /**
         * UNIX timestamp of creation date
         */
        long timestamp;
        /**
         * Compressed entry file size inside the corresponding .cache file
         */
        int compressedSize;
        /**
         * Uncompressed byte size after passing through {@link CacheDecompressionInputStream}
         */
        int uncompressedSize;
    }

    // utility
    private static String bytesToHex(byte[] array) {
        return IntStream
                .range(0, array.length)
                .map(i -> array[i])
                .mapToObj(b -> String.format("%02X", b))
                .collect(Collectors.joining());
    }
}
