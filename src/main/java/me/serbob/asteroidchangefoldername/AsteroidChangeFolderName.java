package me.serbob.asteroidchangefoldername;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.zip.*;
import java.nio.file.*;

public final class AsteroidChangeFolderName extends JavaPlugin {

    @Override
    public void onLoad() {
        saveDefaultConfig();

        String currentFileName = getConfig().getString("current_file_name");
        String newFileName = getConfig().getString("new_file_name");

        try {
            File pluginFolder = getDataFolder().getParentFile();

            File targetJar = null;
            for (File file : pluginFolder.listFiles()) {
                if (file.getName().startsWith(currentFileName)
                        && file.getName().endsWith(".jar")) {
                    targetJar = file;
                    break;
                }
            }

            if (targetJar == null) {
                getLogger().severe("No Asteroid plugin jar found!");
                return;
            }

            File tempJar = new File(pluginFolder, "Asteroid_temp.jar");

            try (ZipInputStream zin = new ZipInputStream(new FileInputStream(targetJar));
                 ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(tempJar))) {

                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    zout.putNextEntry(new ZipEntry(entry.getName()));

                    if (entry.getName().equals("plugin.yml")) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zin.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }

                        String originalYml = baos.toString();
                        String modifiedYml = originalYml.replaceFirst("name:.*",
                                "name: "+newFileName);

                        byte[] modifiedBytes = modifiedYml.getBytes();
                        zout.write(modifiedBytes, 0, modifiedBytes.length);
                    } else {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zin.read(buffer)) > 0) {
                            zout.write(buffer, 0, len);
                        }
                    }
                    zout.closeEntry();
                }
            }

            String version = targetJar.getName().substring(currentFileName.length());
            if (!version.startsWith("-") && !version.isEmpty()) {
                version = "-" + version;
            }

            File newJar = new File(pluginFolder, newFileName + version);

            Files.delete(targetJar.toPath());
            Files.move(tempJar.toPath(), newJar.toPath());

            String currentFolderName = currentFileName.replace("-", "");

            File oldDataFolder = new File(pluginFolder, currentFolderName);
            if (oldDataFolder.exists()) {
                File newDataFolder = new File(pluginFolder, newFileName);
                try {
                    getLogger().info("Attempting to rename folder from: " + oldDataFolder.getPath());
                    getLogger().info("To: " + newDataFolder.getPath());

                    Files.move(oldDataFolder.toPath(), newDataFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    getLogger().info("Successfully renamed the data folder!");
                } catch (IOException e) {
                    getLogger().severe("Failed to rename folder: " + e.getMessage());
                }
            } else {
                getLogger().warning("Old data folder not found at: " + oldDataFolder.getPath());
            }

            getLogger().info("Successfully modified Asteroid plugin name and folders! New jar: " + newJar.getName());

            try {
                getLogger().info("Waiting for changes to be processed...");
                Thread.sleep(2000); // 2 second delay
            } catch (InterruptedException e) {
                getLogger().warning("Sleep interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }

        } catch (IOException e) {
            e.printStackTrace();
            getLogger().severe("Failed to modify Asteroid plugin: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {}
}
