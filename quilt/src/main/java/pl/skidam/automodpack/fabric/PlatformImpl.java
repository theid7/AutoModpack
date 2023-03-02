package pl.skidam.automodpack.fabric; // really don't know why it must be there but architectury says that so

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.minecraft.MinecraftQuiltLoader;
import pl.skidam.automodpack.Download;
import pl.skidam.automodpack.Platform;
import pl.skidam.automodpack.ReLauncher;
import pl.skidam.automodpack.client.ui.AutoModpackToast;
import pl.skidam.automodpack.utils.CustomFileUtils;
import pl.skidam.automodpack.utils.ModrinthAPI;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static pl.skidam.automodpack.Platform.ModPlatform.QUILT;
import static pl.skidam.automodpack.StaticVariables.*;

public class PlatformImpl {

    public static Platform.ModPlatform getPlatformType() {
        return QUILT;
}

    public static boolean isModLoaded(String modid) {
        return QuiltLoader.isModLoaded(modid);
    }

    public static Collection getModList() {
        Collection<ModContainer> modsList = QuiltLoader.getAllMods();

        return modsList.stream().map(mod -> mod.metadata().id() + " " + mod.metadata().version()).collect(Collectors.toList());
    }

    public static void downloadDependencies() {
        if (!Platform.isModLoaded("quilted_fabric_api") && !Platform.isModLoaded("fabric")) { // QFAPI

            LOGGER.warn("Dependency (QFAPI) was not found");

            if (Platform.getEnvironmentType().equals("SERVER")) {
                if (!serverConfig.downloadDependency) {
                    LOGGER.error("AutoModpack update check is disabled, you need to manually install quilted fabric api!");
                    return;
                }
            }

            if (Platform.getEnvironmentType().equals("CLIENT")) {
                if (!clientConfig.downloadDependency) {
                    LOGGER.error("AutoModpack update check is disabled, you need to manually install quilted fabric api!");
                    return;
                }
            }

            ModrinthAPI qfapi = new ModrinthAPI("qvIfYCYJ");

            if (qfapi == null) return;

            LOGGER.info("Installing latest Quilted Fabric API (QFAPI)! " + qfapi.modrinthAPIversion);
            LOGGER.info("Download URL: " + qfapi.modrinthAPIdownloadUrl);
            try {
                Download downloadInstance = new Download();

                File file = new File(modsPath.toFile() + File.separator + qfapi.modrinthAPIfileName);

                downloadInstance.download(qfapi.modrinthAPIdownloadUrl, file); // Download it

                String localChecksum = CustomFileUtils.getHash(file, "SHA-512");

                if (!localChecksum.equals(qfapi.modrinthAPISHA512Hash)) {
                    LOGGER.error("Checksums are not the same! Downloaded file is corrupted!");
                    AutoModpackToast.add(5);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("Failed to download FAPI!");
                return;
            }
            LOGGER.info("Successfully installed latest version of Quilted Fabric API (QFAPI)!");

            new ReLauncher.Restart(null, "Successfully installed latest (QFAPI)!");
        }
    }

    public static File getModPath(String modid) {
        ModContainer container = QuiltLoader.getModContainer(modid).isPresent() ? QuiltLoader.getModContainer(modid).get() : null;

        if (container != null) {
            for (Path path : Objects.requireNonNull(container.getSourcePaths().stream().findFirst().isPresent() ? container.getSourcePaths().stream().findFirst().get() : null)) {
                if (path == null) {
                    LOGGER.error("Could not find jar file for " + modid);
                    return null;
                }

                return path.toFile();
            }
        }
        return null;
    }

    public static String getEnvironmentType() {
        // get environment type on quilt loader
        if (MinecraftQuiltLoader.getEnvironmentType() == EnvType.CLIENT) {
            return "CLIENT";
        } else if (MinecraftQuiltLoader.getEnvironmentType() == EnvType.SERVER) {
            return "SERVER";
        } else {
            return "UNKNOWN";
        }
    }

    public static String getModVersion(String modid) {
        return QuiltLoader.getModContainer(modid).isPresent() ? QuiltLoader.getModContainer(modid).get().metadata().version().toString() : null;
    }

    public static String getModVersion(File file) {
        try {
            ZipFile zipFile = new ZipFile(file);
            ZipEntry entry = null;
            if (zipFile.getEntry("quilt.mod.json") != null) {
                entry = zipFile.getEntry("quilt.mod.json");
            } else if (zipFile.getEntry("fabric.mod.json") != null) {
                entry = zipFile.getEntry("fabric.mod.json");
            }

            if (entry != null) {
                Gson gson = new Gson();
                InputStream stream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                JsonObject json = gson.fromJson(reader, JsonObject.class);

                // close everything
                reader.close();
                stream.close();
                zipFile.close();

                if (entry.getName().equals("fabric.mod.json")) {
                    if (json.has("version")) {
                        return json.get("version").getAsString();
                    }
                } else if (json.has("quilt_loader")) {
                    JsonObject quiltLoader = json.get("quilt_loader").getAsJsonObject();
                    if (quiltLoader.has("version")) {
                        return quiltLoader.get("version").getAsString();
                    }
                }
            }
        } catch (ZipException ignored) {
            return null;
        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }


    public static String getConfigDir() {
        return QuiltLoader.getConfigDir().toString();
    }

    public static boolean isDevelopmentEnvironment() {
        return QuiltLoader.isDevelopmentEnvironment();
    }

    public static String getModEnvironment(String modid) {
        if (QuiltLoader.getModContainer(modid).isPresent()) {
            String env = QuiltLoader.getModContainer(modid).get().metadata().value("environment").toString();
            if (env == null) {
                env = FabricLoader.getInstance().getModContainer(modid).isPresent() ?  FabricLoader.getInstance().getModContainer(modid).get().getMetadata().getEnvironment().toString().toUpperCase() : "*";
            }

            return env.toUpperCase();
        }
        return "UNKNOWN";
    }

    public static String getModEnvironmentFromNotLoadedJar(File file) {
        if (!file.isFile()) return null;
        if (!file.getName().endsWith(".jar")) return null;

        try {
            ZipFile zipFile = new ZipFile(file);
            ZipEntry entry = null;
            if (zipFile.getEntry("quilt.mod.json") != null) {
                entry = zipFile.getEntry("quilt.mod.json");
            } else if (zipFile.getEntry("fabric.mod.json") != null) {
                entry = zipFile.getEntry("fabric.mod.json");
            }

            if (entry != null) {
                Gson gson = new Gson();
                InputStream stream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                JsonObject json = gson.fromJson(reader, JsonObject.class);

                // close everything
                reader.close();
                stream.close();
                zipFile.close();

                if (entry.getName().equals("fabric.mod.json")) {
                    if (json.has("environment")) {
                        return json.get("environment").getAsString().toUpperCase();
                    }
                } else if (json.has("quilt_loader")) {
                    JsonObject quiltLoader = json.get("quilt_loader").getAsJsonObject();
                    if (quiltLoader.has("environment")) {
                        return quiltLoader.get("environment").getAsString().toUpperCase();
                    }

                    // if not, we also can check in different place
                    if (json.has("minecraft")) {
                        JsonObject minecraft = json.get("minecraft").getAsJsonObject();
                        if (minecraft.has("environment")) {
                            return minecraft.get("environment").getAsString().toUpperCase();
                        }
                    }
                }
            }
        } catch (ZipException ignored) {
            return "UNKNOWN";
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "UNKNOWN";
    }

    public static String getModIdFromLoadedJar(File file, boolean checkAlsoOutOfContainer) {
        if (!file.isFile()) return null;
        if (!file.getName().endsWith(".jar")) return null;
        if (getModEnvironmentFromNotLoadedJar(file).equals("UNKNOWN")) return null;

        for (ModContainer modContainer : QuiltLoader.getAllMods()) {
            FileSystem fileSys = modContainer.rootPath().getFileSystem();
            File modFile = new File(fileSys.toString());
            if (modFile.getName().equals(file.getName())) {
                return modContainer.metadata().id();
            }
        }

        if (checkAlsoOutOfContainer) return getModIdFromNotLoadedJar(file);

        return null;
    }

    public static String getModIdFromNotLoadedJar(File file) {
        try {
            ZipFile zipFile = new ZipFile(file);
            ZipEntry entry = null;
            if (zipFile.getEntry("quilt.mod.json") != null) {
                entry = zipFile.getEntry("quilt.mod.json");
            } else if (zipFile.getEntry("fabric.mod.json") != null) {
                entry = zipFile.getEntry("fabric.mod.json");
            }

            if (entry != null) {
                Gson gson = new Gson();
                InputStream stream = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                JsonObject json = gson.fromJson(reader, JsonObject.class);

                // close everything
                reader.close();
                stream.close();
                zipFile.close();

                if (entry.getName().equals("fabric.mod.json")) {
                    if (json.has("id")) {
                        return json.get("id").getAsString();
                    }
                } else if (json.has("quilt_loader")) {
                    JsonObject quiltLoader = json.get("quilt_loader").getAsJsonObject();
                    if (quiltLoader.has("id")) {
                        return quiltLoader.get("id").getAsString();
                    }
                }
            }
        } catch (ZipException ignored) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
