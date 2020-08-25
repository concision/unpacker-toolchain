package me.concision.extractor.api;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.codec.binary.Hex;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Reads entries from a .toc file input stream
 *
 * @author Concision
 */
public class TocStreamReader {
    /**
     * Wrapped decompressed Packages.bin input stream
     */
    private final DataInputStream stream;
    /**
     * Directory hierarchy
     */
    private final List<String> hierarchy = new LinkedList<>(Collections.singletonList(""));
    /**
     * TOC file header
     */
    private byte[] header;
    /**
     * TOC file format version
     */
    private int version;

    /**
     * Constructs a TOC reader from an input stream
     *
     * @param stream decompressed Packages.bin input stream
     */
    @SneakyThrows
    public TocStreamReader(@NonNull InputStream stream) {
        this.stream = new DataInputStream(stream);
    }

    /**
     * Reads TOC version
     *
     * @return TOC version
     * @throws IOException if an underlying IO exception occurs
     */
    public int version() throws IOException {
        if (header == null) {
            this.readHeader();
        }
        return version;
    }

    // decoding

    /**
     * Read TOC header from stream
     *
     * @throws IOException if an underlying IO exception occurs
     */
    private void readHeader() throws IOException {
        // header magic value
        header = new byte[4];
        stream.readFully(header);
        // verify magic value
        if (!Arrays.equals(header, new byte[]{0x4E, (byte) 0xC6, 0x67, 0x18})) {
            throw new IllegalArgumentException("header magic value mismatch (expected: 0x4EC66718, received: " + Hex.encodeHexString(header) + ")");
        }

        // header format version
        version = Integer.reverseBytes(stream.readInt());
        // verify version support
        if (version != 0x14) {
            throw new IllegalArgumentException("version not supported by TOC reader: " + version);
        }
    }

    /**
     * Read the next package entry
     *
     * @return next {@link PackageEntry}, or null if no remaining packages
     * @throws IOException if an underlying IO exception occurs
     */
    public PackageEntry nextEntry() throws IOException {
        // ensure header is read
        if (header == null) {
            this.readHeader();
        }

        try {
            // attempt reading a file until success
            while (true) {
                // parse TOC entry
                long offset = Long.reverseBytes(stream.readLong());
                long timeStamp = Long.reverseBytes(stream.readLong());
                int compressedFileSize = Integer.reverseBytes(stream.readInt());
                int uncompressedFileSize = Integer.reverseBytes(stream.readInt());
                int buffer = Integer.reverseBytes(stream.readInt());
                int depthId = Integer.reverseBytes(stream.readInt());
                // filename
                byte[] filenameBuffer = new byte[64];
                stream.readFully(filenameBuffer);
                int nameEof = 0;
                while (nameEof < filenameBuffer.length && filenameBuffer[nameEof] != '\0') {
                    nameEof++;
                }
                String filename = new String(filenameBuffer, 0, nameEof);

                // skip deleted files (timestamp of 0 indicates deleted)
                if (timeStamp == 0) continue;

                // parent
                String parent = hierarchy.get(depthId);
                // name
                String absoluteFileName = parent + "/" + filename;

                // check if entry is a directory
                if (offset == -1) {
                    // directory
                    hierarchy.add(absoluteFileName);
                    continue;
                }

                // construct package entry
                return new PackageEntry(
                        absoluteFileName,
                        offset,
                        timeStamp,
                        compressedFileSize,
                        uncompressedFileSize
                );
            }
        } catch (EOFException ignored) {
            return null;
        }
    }

    /**
     * Iteratively search through TOC for a specific file
     *
     * @param absoluteFilename specific an absolute filename (e.g. /Lotus/Parent/Filename)
     * @return an {@link Optional <PackageEntry>} of the package
     * @throws IOException if an underlying IO exception occurs
     */
    public Optional<PackageEntry> findEntry(@NonNull String absoluteFilename) throws IOException {
        for (PackageEntry entry; (entry = this.nextEntry()) != null; ) {
            if (entry.filename.equals(absoluteFilename)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }


    /**
     * Package entry construct
     */
    @Value
    public static class PackageEntry {
        /**
         * Package filename
         */
        String filename;
        /**
         * Byte offset of file bytes inside of cache file
         */
        long offset;
        /**
         * Time created
         */
        long timestamp;
        /**
         * Compressed byte size
         */
        int compressedSize;
        /**
         * Uncompressed byte size
         */
        int uncompressedSize;
    }
}