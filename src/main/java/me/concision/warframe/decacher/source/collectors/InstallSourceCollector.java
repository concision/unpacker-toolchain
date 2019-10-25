package me.concision.warframe.decacher.source.collectors;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.log4j.Log4j2;
import me.concision.warframe.decacher.CommandArguments;
import me.concision.warframe.decacher.source.SourceCollector;
import me.concision.warframe.decacher.source.SourceType;

/**
 * See {@link SourceType#INSTALL}
 *
 * @author Concision
 * @date 10/21/2019
 */
@Log4j2
public class InstallSourceCollector implements SourceCollector {
    private static final String REGISTRY_PATH = "Software\\Digital Extremes\\Warframe\\Launcher";

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

        File warframeDirectory;
        if (downloadDir != null) {
            warframeDirectory = new File(downloadDir, "Public");
        } else {
            warframeDirectory = new File(launcherExe).getParentFile().getParentFile();
        }
        log.info("Warframe directory: " + warframeDirectory.getAbsolutePath());

        File cacheDirectory = new File(warframeDirectory, "Cache.Windows");
        log.info("Cache directory: " + cacheDirectory.getAbsolutePath());

        return new FolderSourceCollector().generate(cacheDirectory);
    }
}