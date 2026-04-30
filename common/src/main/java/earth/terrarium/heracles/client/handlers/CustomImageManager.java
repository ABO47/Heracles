package earth.terrarium.heracles.client.handlers;

import earth.terrarium.heracles.Heracles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.platform.NativeImage;
import org.joml.Vector2i;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.awt.GraphicsEnvironment;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CustomImageManager {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("png", "jpg", "jpeg");
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static Path picturesDir;
    private static boolean loggedHeadlessWarning = false;

    private CustomImageManager() {}

    public static void init(Path gameDir) {
        Path root = gameDir.resolve("config").resolve(Heracles.MOD_ID).resolve("assets").resolve("pictures");
        picturesDir = root;
        try {
            Files.createDirectories(root);
        } catch (Exception e) {
            Heracles.LOGGER.error("Failed to create custom pictures directory {}", root, e);
        }
    }

    public static Optional<String> pickAndImportImage() {
        if (GraphicsEnvironment.isHeadless()) {
            if (!loggedHeadlessWarning) {
                loggedHeadlessWarning = true;
                Heracles.LOGGER.info("File picker unavailable in this Gradle dev runtime (headless AWT). Put images in {} and use paths like assets/pictures/your_file.png or assets/pictures/your_file.jpg.", manualImportPathHint());
            }
            return Optional.empty();
        }
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Image Files (PNG, JPG, JPEG)", "png", "jpg", "jpeg"));
            chooser.setAcceptAllFileFilterUsed(false);
            int result = chooser.showOpenDialog(null);
            if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
                return Optional.empty();
            }
            return importImage(chooser.getSelectedFile().toPath());
        } catch (java.awt.HeadlessException ignored) {
            if (!loggedHeadlessWarning) {
                loggedHeadlessWarning = true;
                Heracles.LOGGER.info("File picker unavailable in current runtime (headless AWT). Put images in {} and use paths like assets/pictures/your_file.png or assets/pictures/your_file.jpg.", manualImportPathHint());
            }
            return Optional.empty();
        } catch (Throwable t) {
            Heracles.LOGGER.error("Failed to open file picker", t);
            return Optional.empty();
        }
    }

    public static Optional<String> pickAndImportPng() {
        return pickAndImportImage();
    }

    public static Optional<String> importImage(Path source) {
        if (source == null || !Files.isRegularFile(source)) return Optional.empty();
        String fileName = source.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return Optional.empty();
        String extension = fileName.substring(dot + 1).toLowerCase();
        if (!SUPPORTED_EXTENSIONS.contains(extension)) return Optional.empty();
        if (picturesDir == null) return Optional.empty();

        String base = sanitizeFileName(fileName.substring(0, Math.max(0, dot)));
        if (base.isBlank()) {
            base = "image";
        }

        try {
            Path target = picturesDir.resolve(base + "." + extension);
            int suffix = 1;
            while (Files.exists(target)) {
                target = picturesDir.resolve(base + "_" + suffix + "." + extension);
                suffix++;
            }
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            return Optional.of("assets/pictures/" + target.getFileName().toString().replace('\\', '/'));
        } catch (Exception e) {
            Heracles.LOGGER.error("Failed to import image {}", source, e);
            return Optional.empty();
        }
    }

    public static Optional<String> importPng(Path source) {
        return importImage(source);
    }

    public static ResourceLocation getTexture(String managedRelativePath) {
        if (managedRelativePath == null || managedRelativePath.isBlank()) return null;
        if (CACHE.containsKey(managedRelativePath)) {
            return CACHE.get(managedRelativePath);
        }
        Path path = resolveManagedPath(managedRelativePath);
        if (path == null || !Files.isRegularFile(path)) return null;
        try {
            NativeImage image = readNativeImage(path);
            if (image == null) return null;
            String hash = sha1(managedRelativePath);
            ResourceLocation location = new ResourceLocation(Heracles.MOD_ID, "custom/" + hash);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            CACHE.put(managedRelativePath, location);
            return location;
        } catch (Exception e) {
            Heracles.LOGGER.error("Failed to load custom image {}", managedRelativePath, e);
            return null;
        }
    }

    public static Vector2i getImageSize(String managedRelativePath) {
        Path path = resolveManagedPath(managedRelativePath);
        if (path == null || !Files.isRegularFile(path)) return null;
        try (NativeImage image = readNativeImage(path)) {
            if (image == null) return null;
            return new Vector2i(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()));
        } catch (Exception e) {
            Heracles.LOGGER.error("Failed to read image size for {}", managedRelativePath, e);
            return null;
        }
    }

    public static Path resolveManagedPath(String managedRelativePath) {
        if (picturesDir == null || managedRelativePath == null || managedRelativePath.isBlank()) return null;
        String normalized = managedRelativePath.replace('\\', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        Path root = picturesDir.getParent().getParent();
        if (root == null) return null;
        return root.resolve(normalized);
    }

    public static List<String> listManagedImages() {
        if (picturesDir == null || !Files.isDirectory(picturesDir)) return List.of();
        List<String> out = new ArrayList<>();
        try (var stream = Files.list(picturesDir)) {
            stream.filter(Files::isRegularFile)
                .filter(path -> hasSupportedImageExtension(path.getFileName().toString()))
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                .forEach(path -> out.add("assets/pictures/" + path.getFileName().toString().replace('\\', '/')));
        } catch (Exception e) {
            Heracles.LOGGER.error("Failed to list managed pictures from {}", picturesDir, e);
        }
        return out;
    }

    public static List<String> listManagedPngs() {
        return listManagedImages();
    }

    private static String sanitizeFileName(String value) {
        String out = value.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        while (out.contains("__")) out = out.replace("__", "_");
        return out;
    }

    private static String manualImportPathHint() {
        if (picturesDir == null) {
            return "config/" + Heracles.MOD_ID + "/assets/pictures";
        }
        return picturesDir.toAbsolutePath().toString();
    }

    private static boolean hasSupportedImageExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) return false;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return false;
        return SUPPORTED_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase());
    }

    private static NativeImage readNativeImage(Path path) throws Exception {
        try (InputStream stream = Files.newInputStream(path)) {
            NativeImage direct = NativeImage.read(stream);
            if (direct != null) return direct;
        } catch (Exception ignored) {
        }

        BufferedImage awt = ImageIO.read(path.toFile());
        if (awt == null) return null;
        int w = Math.max(1, awt.getWidth());
        int h = Math.max(1, awt.getHeight());
        NativeImage out = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = awt.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                out.setPixelRGBA(x, y, abgr);
            }
        }
        return out;
    }

    private static String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return Integer.toHexString(Mth.abs(value.hashCode()));
        }
    }
}
