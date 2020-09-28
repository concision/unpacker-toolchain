package me.concision.unnamed.unpacker.cli.source.collectors;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import lombok.NonNull;
import lombok.extern.java.Log;
import me.concision.unnamed.unpacker.cli.CommandArguments;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.source.SourceCollector;
import me.concision.unnamed.unpacker.cli.source.SourceType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * See {@link SourceType#INSTALL}.
 *
 * @author Concision
 */
@Log
public class InstallSourceCollector implements SourceCollector {
    /**
     * Installation path in Windows registry
     */
    @SuppressWarnings("SpellCheckingInspection")
    private static final String REGISTRY_PATH = new String(Base64.getDecoder().decode("U29mdHdhcmVcRGlnaXRhbCBFeHRyZW1lc1xXYXJmcmFtZVxMYXVuY2hlcg=="), StandardCharsets.ISO_8859_1);

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream acquire(@NonNull Unpacker unpacker) throws IOException {
        CommandArguments args = unpacker.args();

        // auto-discover installation directory
        log.info("Discovering game installation directory");
        String launcherExe = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, "LauncherExe");
        log.info("Launcher: " + launcherExe);
        String downloadDir = null;
        try {
            downloadDir = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, "DownloadDir");
        } catch (Throwable ignored) {
        }

        // find game directory
        log.info("Download directory: " + downloadDir);
        File gameDirectory;
        if (downloadDir != null) {
            gameDirectory = new File(downloadDir, "Public");
        } else {
            gameDirectory = new File(launcherExe).getParentFile().getParentFile();
        }
        log.info("Game directory: " + gameDirectory.getAbsolutePath());

        // derive cache directory
        File cacheDirectory = new File(gameDirectory, "Cache.Windows");
        log.info("Cache directory: " + cacheDirectory.getAbsolutePath());

        // delegate to directory source collector
        return new DirectorySourceCollector().generate(cacheDirectory);
    }
}
