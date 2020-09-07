package me.concision.unnamed.unpacker.cli.source.collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Delegate;
import lombok.extern.java.Log;
import me.concision.unnamed.decacher.api.CacheDecompressionInputStream;
import me.concision.unnamed.decacher.api.TocStreamReader;
import me.concision.unnamed.decacher.api.TocStreamReader.CacheEntry;
import me.concision.unnamed.unpacker.cli.CommandArguments;
import me.concision.unnamed.unpacker.cli.source.SourceCollector;
import me.concision.unnamed.unpacker.cli.source.SourceType;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * See {@link SourceType#ORIGIN}.
 *
 * @author Concision
 */
@Log
public class OriginSourceCollector implements SourceCollector {
    /**
     * Origin server URL
     */
    @SuppressWarnings("SpellCheckingInspection")
    static final String ORIGIN_URL = new String(Base64.getDecoder().decode("aHR0cDovL29yaWdpbi53YXJmcmFtZS5jb20="), StandardCharsets.ISO_8859_1);
    /**
     * Origin index manifest file URI
     */
    static final String INDEX_MANIFEST = new String(Base64.getDecoder().decode("aW5kZXgudHh0Lmx6bWE="), StandardCharsets.ISO_8859_1);
    /**
     * Matches depot files format from index manifest
     */
    static final Pattern DEPOT_FILE_PATTERN = Pattern.compile("^(?<path>/(?:[^/]+/)*(?<filename>.+)\\.[0-9A-F]{32}\\.lzma),(?<filesize>\\d+)$");

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("DuplicatedCode")
    public InputStream acquire(@NonNull CommandArguments args) {
        // obtain listing of CDN depot files
        @RequiredArgsConstructor
        @ToString
        class DepotFile {
            final String path;
            final String name;
            final long size;
        }
        List<DepotFile> files = new LinkedList<>();
        log.info("Fetching " + INDEX_MANIFEST + " manifest");
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .addInterceptorLast(new HeaderFormatter())
                .build()) {
            // form index manifest request
            HttpGet request = new HttpGet(ORIGIN_URL + "/" + INDEX_MANIFEST);
            request.setProtocolVersion(HttpVersion.HTTP_1_1);

            // execute request
            CloseableHttpResponse response = httpClient.execute(request);

            // parse response
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new LZMACompressorInputStream(response.getEntity().getContent())))) {
                // parse entries in manifest
                for (String entry; (entry = reader.readLine()) != null; ) {
                    Matcher matcher = DEPOT_FILE_PATTERN.matcher(entry);

                    if (!matcher.matches()) {
                        log.warning("Unknown manifest format: " + entry);
                        continue;
                    }

                    // parse depot format
                    String path = matcher.group("path");
                    String filename = matcher.group("filename");
                    long filesize;
                    try {
                        filesize = Long.parseLong(matcher.group("filesize"));
                    } catch (NumberFormatException exception) {
                        log.warning("Invalid filesize: " + matcher.group("filesize"));
                        continue;
                    }

                    files.add(new DepotFile(path, filename, filesize));
                }
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("failed to fetch " + INDEX_MANIFEST + " manifest", throwable);
        }

        // find Packages.bin .toc entry
        log.info("Fetching " + TOC_NAME);
        CacheEntry packagesBinEntry;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .addInterceptorLast(new HeaderFormatter())
                .build()) {
            // build TOC url
            String tocUrl = files.stream()
                    .filter(file -> file.name.equals(TOC_NAME))
                    .findFirst()
                    .map(entry -> ORIGIN_URL + entry.path)
                    .orElseThrow(() -> new RuntimeException("failed to find " + TOC_NAME + " in manifest"));
            log.info("TOC URL: " + tocUrl);

            // build request
            HttpGet request = new HttpGet(tocUrl);
            request.setProtocolVersion(HttpVersion.HTTP_1_1);

            // execute request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                log.info(TOC_NAME + " response code: " + response.getStatusLine());
                // parse response in real-time
                try (InputStream stream = new LZMACompressorInputStream(response.getEntity().getContent())) {
                    // search for Packages.bin
                    Optional<CacheEntry> cacheEntry = new TocStreamReader(new BufferedInputStream(stream)).findEntry("/Packages.bin");

                    if (!cacheEntry.isPresent()) {
                        throw new RuntimeException("failed to find Packages.bin");
                    }

                    packagesBinEntry = cacheEntry.get();
                }
            }
        } catch (EOFException ignored) {
            throw new RuntimeException("reached EOF before finding Packages.bin");
        } catch (Throwable throwable) {
            throw new RuntimeException("failed to parse " + TOC_NAME + " or find Packages.bin entry", throwable);
        }

        // fetch .cache file
        log.info("Fetching " + CACHE_NAME + "...");
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .addInterceptorLast(new HeaderFormatter())
                .build();
        try {
            // build cache url
            String cacheUrl = files.stream()
                    .filter(file -> file.name.equals(CACHE_NAME))
                    .findFirst()
                    .map(entry -> ORIGIN_URL + entry.path)
                    .orElseThrow(() -> new RuntimeException("failed to find " + CACHE_NAME + " in manifest"));
            log.info("Cache URL: " + cacheUrl);

            // download cache file
            HttpGet request = new HttpGet(cacheUrl);
            request.setProtocolVersion(HttpVersion.HTTP_1_1);

            // execute response
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            log.info(CACHE_NAME + " response code: " + response.getStatusLine());

            InputStream input = new BufferedInputStream(new LZMACompressorInputStream(new BufferedInputStream(entity.getContent())));
            // skip bytes
            log.info("Skipping until Packages.bin");
            IOUtils.skip(input, packagesBinEntry.offset());
            log.info("Reached Packages.bin; streaming to next format writer");

            // Packages.bin decompressor
            return new DependentInputStream(new CacheDecompressionInputStream(new BoundedInputStream(
                    input,
                    packagesBinEntry.compressedSize()
            )), httpClient);
        } catch (Throwable throwable) {
            IOUtils.closeQuietly(httpClient);
            throw new RuntimeException("failed to fetch " + CACHE_NAME, throwable);
        }
    }

    /**
     * Formats header to not provide much client information
     */
    static class HeaderFormatter implements HttpRequestInterceptor {
        @Override
        public void process(HttpRequest request, HttpContext httpContext) {
            // preserve specific host and header
            Header hostHeader = request.getFirstHeader("Host");

            // strip all headers
            for (HeaderIterator iterator = request.headerIterator(); iterator.hasNext(); ) {
                iterator.nextHeader();
                iterator.remove();
            }

            // rebuild headers
            if (hostHeader != null) {
                request.setHeader("Host", hostHeader.getValue());
            }
            request.setHeader("Connection", "Keep-Alive");
            request.setHeader("Pragma", "no-cache");
        }
    }

    /**
     * Closes a {@link Closeable instance} upon stream {@link InputStream#close()} invocation.
     */
    @RequiredArgsConstructor
    private static class DependentInputStream extends InputStream implements Closeable {
        /**
         * {@link InputStream} to hook {@link InputStream#close()} call on.
         */
        @Delegate(excludes = Closeable.class)
        private final InputStream stream;

        /**
         * {@link Closeable} to be closed.
         */
        private final Closeable closeable;

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            IOUtils.closeQuietly(closeable);
            stream.close();
        }
    }
}
