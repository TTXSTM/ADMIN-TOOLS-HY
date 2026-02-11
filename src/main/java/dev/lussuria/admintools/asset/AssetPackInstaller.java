package dev.lussuria.admintools.asset;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class AssetPackInstaller {
    private static final String MOD_ARCHIVE_NAME = "lussuria_admintools_asset_pack.zip";

    private static final ResourceEntry[] RESOURCE_ENTRIES = new ResourceEntry[] {
        new ResourceEntry("assetpack/manifest.json", "manifest.json"),
        new ResourceEntry("Server/Item/Items/AdminTools/AdminTools_Storm_Wand.json", "Server/Item/Items/AdminTools/AdminTools_Storm_Wand.json"),
        new ResourceEntry("Server/Languages/en-US/items.lang", "Server/Languages/en-US/items.lang"),
        new ResourceEntry("Lightning/Lightning.particlesystem", "Lightning/Lightning.particlesystem"),
        new ResourceEntry("Lightning/Spawners/Lightning.particlespawner", "Lightning/Spawners/Lightning.particlespawner"),
        new ResourceEntry("Lightning/Spawners/Lightning_Poof.particlespawner", "Lightning/Spawners/Lightning_Poof.particlespawner"),
        new ResourceEntry("Lightning/Spawners/Lightning_Smoke.particlespawner", "Lightning/Spawners/Lightning_Smoke.particlespawner"),
        new ResourceEntry("Lightning/Spawners/Lightning_Sparks.particlespawner", "Lightning/Spawners/Lightning_Sparks.particlespawner"),
        new ResourceEntry("Lightning/Spawners/Lightning_Start.particlespawner", "Lightning/Spawners/Lightning_Start.particlespawner"),
        new ResourceEntry("Lightning/Spawners/Lightning_Trail.particlespawner", "Lightning/Spawners/Lightning_Trail.particlespawner")
    };

    private AssetPackInstaller() {
    }

    public static void installToMods(HytaleLogger logger, Path dataDirectory) {
        Path tempZip = null;
        try {
            Path modsDirectory = resolveModsDirectory(dataDirectory);
            Files.createDirectories(modsDirectory);

            tempZip = Files.createTempFile(dataDirectory, "admintools-assets-", ".zip");
            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(tempZip)))) {
                for (ResourceEntry resource : RESOURCE_ENTRIES) {
                    addResource(zip, resource);
                }
            }

            Path destination = modsDirectory.resolve(MOD_ARCHIVE_NAME);
            Files.move(tempZip, destination, StandardCopyOption.REPLACE_EXISTING);
            logger.at(Level.INFO).log("AdminTools asset pack generated at: %s", destination.toAbsolutePath());
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to generate AdminTools asset pack: %s", e.getMessage());
        } finally {
            if (tempZip != null) {
                try {
                    Files.deleteIfExists(tempZip);
                } catch (IOException ignored) {
                    // Best-effort cleanup.
                }
            }
        }
    }

    private static void addResource(ZipOutputStream zip, ResourceEntry resource) throws IOException {
        try (InputStream in = AssetPackInstaller.class.getClassLoader().getResourceAsStream(resource.sourcePath)) {
            if (in == null) {
                throw new IOException("Missing bundled asset resource: " + resource.sourcePath);
            }
            zip.putNextEntry(new ZipEntry(resource.zipEntryPath));
            in.transferTo(zip);
            zip.closeEntry();
        }
    }

    private record ResourceEntry(String sourcePath, String zipEntryPath) {
    }

    private static Path resolveModsDirectory(Path dataDirectory) {
        Path absolute = dataDirectory.toAbsolutePath();
        for (Path current = absolute; current != null; current = current.getParent()) {
            Path lowerMods = current.resolve("mods");
            if (Files.isDirectory(lowerMods)) {
                return lowerMods;
            }
            Path upperMods = current.resolve("Mods");
            if (Files.isDirectory(upperMods)) {
                return upperMods;
            }
            if (current.getFileName() != null && current.getFileName().toString().equalsIgnoreCase("plugins")) {
                return current.resolveSibling("mods");
            }
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath();
        return workingDirectory.resolve("mods");
    }
}
