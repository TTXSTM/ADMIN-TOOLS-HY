package dev.lussuria.admintools.asset;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

public final class AssetPackInstaller {
    private static final String MOD_FOLDER_NAME = "dev.lussuria_AdminTools";
    private static final String LEGACY_MOD_ARCHIVE_NAME = "lussuria_admintools_asset_pack.zip";

    private static final ResourceEntry[] RESOURCE_ENTRIES = new ResourceEntry[] {
        new ResourceEntry("assetpack/manifest.json", "manifest.json"),
        new ResourceEntry("Server/Item/Items/AdminTools/AdminTools_Storm_Wand.json", "Server/Item/Items/AdminTools/AdminTools_Storm_Wand.json"),
        new ResourceEntry("Server/Languages/en-US/items.lang", "Server/Languages/en-US/items.lang"),
        new ResourceEntry("Common/Blocks/valentine_lantern_floor/valentine_lantern_floor.blockymodel", "Common/Blocks/valentine_lantern_floor/valentine_lantern_floor.blockymodel"),
        new ResourceEntry("Common/BlockTextures/valentine_lantern_floor.png", "Common/BlockTextures/valentine_lantern_floor.png"),
        new ResourceEntry("Common/Icons/ItemsGenerated/valentine_lantern_floor.png", "Common/Icons/ItemsGenerated/valentine_lantern_floor.png"),
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
        try {
            Path modsDirectory = resolveModsDirectory(dataDirectory);
            Files.createDirectories(modsDirectory);

            Path destinationRoot = modsDirectory.resolve(MOD_FOLDER_NAME);
            Files.createDirectories(destinationRoot);

            for (ResourceEntry resource : RESOURCE_ENTRIES) {
                copyResourceToFile(resource, destinationRoot);
            }

            // Remove old zip artifact from previous implementation.
            Files.deleteIfExists(modsDirectory.resolve(LEGACY_MOD_ARCHIVE_NAME));

            logger.at(Level.INFO).log("AdminTools assets synced to folder: %s", destinationRoot.toAbsolutePath());
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to sync AdminTools asset folder: %s", e.getMessage());
        }
    }

    private static void copyResourceToFile(ResourceEntry resource, Path destinationRoot) throws IOException {
        Path destination = destinationRoot.resolve(resource.relativePath);
        Files.createDirectories(destination.getParent());

        try (InputStream in = AssetPackInstaller.class.getClassLoader().getResourceAsStream(resource.sourcePath)) {
            if (in == null) {
                throw new IOException("Missing bundled asset resource: " + resource.sourcePath);
            }
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record ResourceEntry(String sourcePath, String relativePath) {
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
