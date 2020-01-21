package me.concision.unpacker.source.collectors;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import lombok.extern.log4j.Log4j2;
import me.concision.unpacker.CommandArguments;
import me.concision.unpacker.source.SourceCollector;
import me.concision.unpacker.source.SourceType;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.core.config.plugins.convert.Base64Converter;

/**
 * See {@link SourceType#INSTALL}
 *
 * @author Concision
*/
@Log4j2
public class InstallSourceCollector implements SourceCollector {
    /**
     * Windows registry location; slightly obfuscated to prevent search indexing
     */
    private static final String REGISTRY_PATH = new String(Base64Converter.parseBase64Binary("U29mdHdhcmVcRGlnaXRhbCBFeHRyZW1lc1xXYXJmcmFtZVxMYXVuY2hlcg=="), StandardCharsets.ISO_8859_1);

    @Override
    public InputStream generate(CommandArguments args) throws IOException {
        log.info("Searching for install");
        String launcherExe = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, "LauncherExe");
        log.info("Launcher: " + launcherExe);
        String downloadDir = null;
        try {
            downloadDir = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, "DownloadDir");
        } catch (Throwable ignored) {}
        log.info("Download directory: " + downloadDir);

        File gameDirectory;
        if (downloadDir != null) {
            gameDirectory = new File(downloadDir, "Public");
        } else {
            gameDirectory = new File(launcherExe).getParentFile().getParentFile();
        }
        log.info("Game directory: " + gameDirectory.getAbsolutePath());

        File cacheDirectory = new File(gameDirectory, "Cache.Windows");
        log.info("Cache directory: " + cacheDirectory.getAbsolutePath());

        return new FolderSourceCollector().generate(cacheDirectory);
    }
}