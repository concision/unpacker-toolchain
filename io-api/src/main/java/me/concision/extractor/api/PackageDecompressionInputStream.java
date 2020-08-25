package me.concision.extractor.api;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.sun.istack.internal.NotNull;
import lombok.NonNull;

/**
 * Decompression stream for a .cache file input stream
 *
 * @author Concision
*/
public class PackageDecompressionInputStream extends InputStream {
    /**
     * Raw compressed input stream
     */
    private final DataInputStream source;
    /**
     * Internal decompression buffer
     */
    private byte[] buffer = new byte[Character.MAX_VALUE];

    /**
     * Decompressed value buffer
     */
    private ByteArrayInputStream bufferStream;

    /**
     * Wraps an input stream with a Packages.bin decompression input stream
     *
     * @param stream {@link InputStream} to wrap
     */
    public PackageDecompressionInputStream(@NonNull InputStream stream) {
        source = new DataInputStream(stream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        if (bufferStream == null || bufferStream.available() == 0) {
            this.decompress();
        }

        return bufferStream.read();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }

        int totalBytesRead = 0;
        for (int needed = len, offset = off; 0 < needed; ) {
            if (bufferStream == null || bufferStream.available() == 0) {
                this.decompress();
            }

            int bytesRead = bufferStream.read(buffer, offset, Math.min(needed, buffer.length - off + totalBytesRead));

            if (bytesRead == -1) {
                break;
            }
            totalBytesRead += bytesRead;

            needed -= bytesRead;
            offset += bytesRead;
        }
        if (totalBytesRead == 0) {
            return -1;
        }
        return totalBytesRead;
    }

    /**
     * Decompresses a block from the input stream
     *
     * @throws IOException if an underlying IO exception occurs
     */
    private void decompress() throws IOException {
        int blocklen = ((source.read() & 0xff) << 8) | (source.read() & 0xFF);
        int decomplen = ((source.read() & 0xFF) << 8) | (source.read() & 0xFF);
        if (blocklen == decomplen) {
            source.readFully(buffer, 0, blocklen);
            bufferStream = new ByteArrayInputStream(buffer, 0, blocklen);
        } else if (blocklen < decomplen) {
            int compPos = 0;
            int decompPos = 0;
            int dictDist;

            while (compPos < blocklen) {
                compPos++;
                byte dcodeWord = (byte) source.read();
                int codeWord = dcodeWord & 0xFF;
                if (codeWord <= 0x1f) {
                    // Encode literal
                    if (decomplen < decompPos + codeWord + 1) {
                        throw new IndexOutOfBoundsException();
                    }
                    if (blocklen < compPos + codeWord + 1) {
                        throw new IndexOutOfBoundsException();
                    }
                    for (int i = codeWord; 0 <= i; --i) {
                        buffer[decompPos] = (byte) source.read();
                        decompPos++;
                        compPos++;
                    }
                } else {
                    // encode dictionary
                    int copyLen = codeWord >>> 5; // high 3 bits are copy length
                    if (copyLen == 7) {
                        if (blocklen <= compPos) {
                            throw new IndexOutOfBoundsException();
                        }
                        copyLen += source.read(); // grab next byte and add 7 to it
                        compPos++;
                    }
                    if (compPos >= blocklen) {
                        throw new IndexOutOfBoundsException();
                    }

                    dictDist = ((codeWord & 0x1f) << 8) | (source.read() & 0xFF); // 13 bits code lookback offset

                    compPos++;
                    copyLen += 2; // Add 2 to copy length
                    if (decomplen < decompPos + copyLen) {
                        throw new IndexOutOfBoundsException();
                    }
                    int decompDistBeginPos = decompPos - 1 - dictDist;
                    if (decompDistBeginPos < 0) {
                        throw new IndexOutOfBoundsException();
                    }
                    for (int i = 0; i < copyLen; i++, decompPos++) {
                        buffer[decompPos] = buffer[decompDistBeginPos + i];
                    }
                }
            }

            if (decompPos != decomplen && blocklen != 0) {
                throw new IndexOutOfBoundsException();
            }

            bufferStream = new ByteArrayInputStream(buffer, 0, decomplen);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        buffer = null;
        bufferStream = null;
        source.close();
    }
}