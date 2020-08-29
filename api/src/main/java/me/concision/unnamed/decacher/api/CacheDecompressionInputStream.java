package me.concision.unnamed.decacher.api;

import lombok.NonNull;
import me.concision.unnamed.decacher.api.TocStreamReader.CacheEntry;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Decompresses an cache entry's byte range from a {@link InputStream}.
 * <p>
 * Note: Oodle decompression is currently not supported.
 *
 * @author Concision
 */
public class CacheDecompressionInputStream extends InputStream {
    /**
     * A wrapped {@link DataInputStream} of the passed {@link InputStream} specified in the constructor
     * {@link #CacheDecompressionInputStream}; wrapping is useful for the functionality that {@link DataInputStream}
     * supports.
     */
    private final DataInputStream stream;

    // internal buffer

    /**
     * An internal decompression buffer. Upon {@link CacheDecompressionInputStream#close()}, the field is set to
     * {@code null}.
     */
    private byte[] buffer = new byte[0xFFFF];
    /**
     * A wrapped {@link ByteArrayInputStream} of {@link #buffer} with a specified byte length. Upon
     * {@link CacheDecompressionInputStream#close()}, the field is set to {@code null}.
     */
    private ByteArrayInputStream bufferStream;


    /**
     * Initializes a decompression stream from a compliant underlying {@link InputStream}.
     * <p>
     * Note: An unsupported or invalid cache entry might cause an over-consumption of {@link InputStream} and read into
     * subsequent bytes. An {@link InputStream} should be limited to a certain number of bytes if necessary, before
     * invoking this constructor.
     *
     * @param stream a {@link InputStream} containing bytes in a {@link CacheEntry}'s range
     */
    public CacheDecompressionInputStream(@NonNull InputStream stream) {
        this.stream = new DataInputStream(stream);
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
    public int read(@SuppressWarnings("NullableProblems") @NonNull byte[] buffer, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }

        int bytesRead = 0;
        for (int needed = len, offset = off; 0 < needed; ) {
            // decompress more bytes
            if (bufferStream == null || bufferStream.available() == 0) {
                this.decompress();
            }

            // read bytes from a decompressed block
            int read = bufferStream.read(buffer, offset, Math.min(needed, buffer.length - off + bytesRead));

            // break if the stream is fully consumed
            if (read == -1) {
                break;
            }

            bytesRead += read;
            needed -= read;
            offset += read;
        }
        if (bytesRead == 0) {
            return -1;
        }
        return bytesRead;
    }

    /**
     * Decompresses a block of up to 0xFFFF bytes from the underlying {@link InputStream}.
     *
     * @throws IOException if an underlying I/O exception occurs
     */
    private void decompress() throws IOException {
        int blockSize = ((stream.read() & 0xff) << 8) | (stream.read() & 0xFF);
        int decompressedSize = ((stream.read() & 0xFF) << 8) | (stream.read() & 0xFF);

        // decompress block
        if (blockSize == decompressedSize) { // no compression
            stream.readFully(buffer, 0, blockSize);
            bufferStream = new ByteArrayInputStream(buffer, 0, blockSize);
        } else if (decompressedSize <= blockSize) { // oodle decompress
            // TODO: implement oodle decompression
            // int decompressedLength = OodleLZ_Decompress(new byte[blockSize], blockSize, new byte[decompressedLength], decompressedLength);
            // assert decompressedLength == decompressedSize;
            throw new IllegalStateException("OodleLZ_Decompress decompression is unsupported");
        } else {
            int compressionIndex = 0;
            int decompressionIndex = 0;

            while (compressionIndex < blockSize) {
                compressionIndex++;

                int codeWord = stream.read() & 0xFF;
                if (codeWord <= 0x1f) {
                    // Encode literal
                    if (decompressedSize < decompressionIndex + codeWord + 1) {
                        throw new IndexOutOfBoundsException();
                    }
                    if (blockSize < compressionIndex + codeWord + 1) {
                        throw new IndexOutOfBoundsException();
                    }
                    for (int i = codeWord; 0 <= i; i--) {
                        buffer[decompressionIndex] = (byte) stream.read();
                        decompressionIndex++;
                        compressionIndex++;
                    }
                } else {
                    // encode dictionary
                    int copyLength = codeWord >>> 5; // high 3 bits are copy length
                    if (copyLength == 7) {
                        if (blockSize <= compressionIndex) {
                            throw new IndexOutOfBoundsException();
                        }
                        copyLength += stream.read(); // read next byte and add 7 to it
                        compressionIndex++;
                    }

                    if (blockSize <= compressionIndex) {
                        throw new IndexOutOfBoundsException();
                    }

                    // 13 bits code look-back offset
                    int dictDist = ((codeWord & 0x1f) << 8) | (stream.read() & 0xFF);

                    compressionIndex++;
                    copyLength += 2; // add 2 to copy length
                    if (decompressedSize < decompressionIndex + copyLength) {
                        throw new IndexOutOfBoundsException();
                    }
                    int decompressionStartPosition = decompressionIndex - 1 - dictDist;
                    if (decompressionStartPosition < 0) {
                        throw new IndexOutOfBoundsException();
                    }
                    for (int i = 0; i < copyLength; i++, decompressionIndex++) {
                        buffer[decompressionIndex] = buffer[decompressionStartPosition + i];
                    }
                }
            }

            if (decompressionIndex != decompressedSize && blockSize != 0) {
                throw new IndexOutOfBoundsException();
            }

            bufferStream = new ByteArrayInputStream(buffer, 0, decompressedSize);
        }
    }

    /**
     * Disposes of internal buffer references on stream close.
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // release buffers
        buffer = null;
        bufferStream = null;
        stream.close();
    }
}
