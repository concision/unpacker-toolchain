package me.concision.unnamed.unpacker.cli.source.collectors;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.java.Log;
import me.concision.unnamed.unpacker.cli.CommandArguments;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.source.SourceCollector;
import me.concision.unnamed.unpacker.cli.source.SourceType;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import static com.sun.jna.platform.win32.User32.INSTANCE;
import static com.sun.jna.platform.win32.WinNT.PROCESS_QUERY_INFORMATION;
import static com.sun.jna.platform.win32.WinUser.SW_HIDE;
import static me.concision.unnamed.unpacker.cli.source.collectors.OriginSourceCollector.DEPOT_FILE_PATTERN;
import static me.concision.unnamed.unpacker.cli.source.collectors.OriginSourceCollector.INDEX_MANIFEST;
import static me.concision.unnamed.unpacker.cli.source.collectors.OriginSourceCollector.ORIGIN_URL;

/**
 * See {@link SourceType#UPDATER}
 *
 * @author Concision
 */
@Log
public class UpdaterSourceCollector implements SourceCollector {
    /**
     * Game client executable filename
     */
    private static final String CLIENT_EXECUTABLE_FILENAME = new String(Base64.getDecoder().decode("V2FyZnJhbWUueDY0LmV4ZQ=="), StandardCharsets.ISO_8859_1);

    @Override
    @SuppressWarnings("DuplicatedCode")
    public InputStream generate(@NonNull CommandArguments args) throws IOException {
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
                .addInterceptorLast(new OriginSourceCollector.HeaderFormatter())
                .build()) {
            // form index request
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

                    // parse depot
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

        // create temporary directory
        log.info("Creating temporary directory");
        File tempDirectory;
        try {
            tempDirectory = Files.createTempDirectory(Unpacker.class.getSimpleName()).toFile();
        } catch (IOException exception) {
            throw new RuntimeException("failed to create a temporary directory", exception);
        }
        // delete temporary directory on system exit
        tempDirectory.deleteOnExit();
        //noinspection ResultOfMethodCallIgnored
        Runtime.getRuntime().addShutdownHook(new Thread(() -> walk(tempDirectory, File::delete), "Unpacker::ShutdownHook"));
        log.info("Using temporary directory: " + tempDirectory.getAbsolutePath());

        // find game client
        log.info("Fetching game client");
        File executableFile = new File(tempDirectory, CLIENT_EXECUTABLE_FILENAME);
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .addInterceptorLast(new OriginSourceCollector.HeaderFormatter())
                .build()) {
            // build game client url
            String clientUrl = files.stream()
                    .filter(file -> file.name.equals(CLIENT_EXECUTABLE_FILENAME))
                    .findFirst()
                    .map(entry -> ORIGIN_URL + entry.path)
                    .orElseThrow(() -> new RuntimeException("failed to find game client in manifest"));
            log.info("Game client URL: " + clientUrl);

            // form request
            HttpGet request = new HttpGet(clientUrl);
            request.setProtocolVersion(HttpVersion.HTTP_1_1);

            // execute request
            CloseableHttpResponse response = httpClient.execute(request);

            // download game client and save it to temporary directory
            try (InputStream stream = new LZMACompressorInputStream(response.getEntity().getContent())) {
                try (OutputStream executableStream = new BufferedOutputStream(new FileOutputStream(executableFile))) {
                    IOUtils.copy(stream, executableStream);
                }
            }
            response.close();
        } catch (EOFException ignored) {
            throw new RuntimeException("reached EOF before finding game client");
        } catch (Throwable throwable) {
            throw new RuntimeException("failed to fetch game client", throwable);
        }

        // build game client command to execute
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        log.info("Is Windows: " + isWindows);
        String clientPath = executableFile.getAbsolutePath();
        try {
            // build command
            String[] command;
            if (isWindows) {
                command = new String[]{clientPath, "-silent", "-cluster:public", "-applet:/EE/Types/Framework/ContentUpdate"};
            } else {
                command = new String[]{
                        "sh",
                        "-c",
                        args.wineCmd.replace("%UNPACKER_COMMAND%", (clientPath.contains("\"") ? "\"" + clientPath + "\"" : clientPath) + " -silent -cluster:public -applet:/EE/Types/Framework/ContentUpdate")
                };
            }
            log.info("Client command: " + String.join(" ", command));

            // execute game client
            Process updaterProcess = Runtime.getRuntime().exec(
                    command,
                    new String[]{
                            "WINEARCH=win64",
                            "WINEPREFIX=" + tempDirectory.getAbsolutePath() + "/.wine64",
                            // solves a "Error opening terminal: unknown" (see https://askubuntu.com/a/1156144)
                            "TERM=xterm-256color"
                    }
            );

            // process watching threads to initialize
            List<Thread> threads = new LinkedList<>();

            // if on Windows, start a thread to hide any popups once the process is available
            if (isWindows) {
                threads.add(new Thread(() -> awaitHideWindow(executableFile), "Unpacker::ClientUpdater::HideWindow"));
            }

            // logging pipe threads
            threads.add(new Thread(() -> log("STDOUT", updaterProcess.getInputStream()), "Unpacker::ClientUpdater::StandardInput"));
            threads.add(new Thread(() -> log("STDERR", updaterProcess.getErrorStream()), "Unpacker::ClientUpdater::StandardError"));

            // start all threads
            for (Thread thread : threads) {
                // do not allow this thread to hang the JVM on main thread exit
                thread.setDaemon(true);
                thread.start();
            }

            // wait for game client to finish
            int exitCode = updaterProcess.waitFor();
            log.info("Client exit code: " + exitCode);

            // cleanup threads
            log.info("Waiting for process management threads to finish");
            for (Thread thread : threads) {
                log.info("Awaiting " + thread.getName());
                thread.interrupt();
                thread.join();
            }
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException("failed to run updater command", exception);
        }

        // clean up temporary directory
        log.info("Cleaning up temporary directory");
        File cacheFolder = new File(tempDirectory, "Cache.Windows");
        Path cachePath = cacheFolder.toPath();
        // delete all unnecessary files
        if (!isWindows) {
            // native acceleration must be used for performance
            Process deleteProcess = Runtime.getRuntime().exec(new String[]{
                    "rm", "-rf", clientPath, new File(tempDirectory, ".wine64").getAbsolutePath()
            });
            try {
                deleteProcess.waitFor();
            } catch (InterruptedException ignored) {
            }
        }
        walk(tempDirectory, (File file) -> {
            // check if Packages.bin related file
            if (file.toPath().startsWith(cachePath)) {
                // delete when system exits
                file.deleteOnExit();
            } else {
                // delete now
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        });

        // delegate to folder source collector
        return new FolderSourceCollector().generate(cacheFolder);
    }

    // threads

    /**
     * Redirects process {@link InputStream} to logger
     *
     * @param prefix logging prefix
     * @param stream {@link InputStream} to read
     */
    private static void log(String prefix, InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            for (String line; (line = reader.readLine()) != null; ) {
                log.info("[" + prefix + "] " + line);
            }
        } catch (IOException exception) {
            log.severe("[" + prefix + "] Stream closed");
        }
    }

    /**
     * Waits for a process to be spawned and then hides all GUI windows using JNA
     *
     * @param executableFile Game client executable {@link File} to match
     */
    @SuppressWarnings("BusyWait")
    private static void awaitHideWindow(File executableFile) {
        // found client hwnd
        HWND[] clientHwnd = new HWND[1];

        // JNA buffer
        byte[] buffer = new byte[executableFile.getAbsolutePath().length() + 2];
        IntByReference pointer = new IntByReference();

        while (clientHwnd[0] == null) {
            INSTANCE.EnumWindows((hwnd, __) -> {
                // get process id
                INSTANCE.GetWindowThreadProcessId(hwnd, pointer);
                // read process information
                WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(PROCESS_QUERY_INFORMATION, false, pointer.getValue());
                // read absolute filename
                Psapi.INSTANCE.GetModuleFileNameExA(process, null, buffer, buffer.length - 1 /* null terminator */);

                // convert to File
                File processFile = new File(Native.toString(buffer));

                // check if process is our executable
                if (processFile.getName().equals(executableFile.getName()) || executableFile.equals(processFile)) {
                    // save file
                    clientHwnd[0] = hwnd;
                    // exit iteration early
                    return false;
                }

                // keep iterating
                return true;
            }, null);

            // try again in 1 microsecond
            if (clientHwnd[0] == null) {
                try {
                    Thread.sleep(0, (int) TimeUnit.MICROSECONDS.toNanos(1));
                } catch (InterruptedException ignored) {
                    return;
                }
            }
        }

        log.info("Detected running process; removing visibility");
        try {
            while (true) {
                INSTANCE.ShowWindow(clientHwnd[0], SW_HIDE);
                Thread.sleep(1);
            }
        } catch (InterruptedException ignored) {
        }
    }

    // utility

    /**
     * Traverse a directory recursively
     *
     * @param parent   root directory to traverse
     * @param consumer {@link Consumer} callback
     */
    private static void walk(File parent, Consumer<File> consumer) {
        File[] files = parent.listFiles();
        if (files != null) {
            for (File file : files) {
                walk(file, consumer);
            }
        }
        consumer.accept(parent);
    }
}
