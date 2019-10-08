package me.concision.warframe.decacher.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Decompression stream for a .cache file input stream
 *
 * @author Concision
 * @date 10/7/2019
 */
@RequiredArgsConstructor
public class PackageDecompressionInputStream extends InputStream {
    /**
     * Raw compressed input stream
     */
    @NonNull
    private final InputStream source;
    /**
     * Internal decompression buffer
     */
    private final byte[] buffer = new byte[Character.MAX_VALUE];

    /**
     * Decompressed value buffer
     */
    private ByteArrayInputStream bufferStream;

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
            int read = source.read(buffer, 0, blocklen);
            bufferStream = new ByteArrayInputStream(buffer, 0, read);
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
                    for (int i = codeWord; i >= 0; --i) {
                        buffer[decompPos] = (byte) source.read();
                        decompPos++;
                        compPos++;
                    }
                } else {
                    // encode dictionary
                    int copyLen = codeWord >>> 5; // high 3 bits are copy length
                    if (copyLen == 7) {
                        if (compPos >= blocklen) {
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
}