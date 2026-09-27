package com.swingcraft4j;

import com.swingcraft4j.loading.LoadingPainter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Demo helper: finds every {@link LoadingPainter} class under the painter package, grouped by sub-package.
 */
final class PainterCatalog {

    private static final String PACKAGE = "com.swingcraft4j.loading.painter";

    private PainterCatalog() {
    }

    /**
     * One instance of each painter, keyed by sub-package name (e.g. "arc") and sorted by class name.
     */
    static Map<String, List<LoadingPainter>> load() {
        Map<String, List<LoadingPainter>> groups = new TreeMap<>();
        for (String className : classNames()) {
            LoadingPainter painter = instantiate(className);
            if (painter != null) {
                String pkg = className.substring(0, className.lastIndexOf('.'));
                String group = pkg.equals(PACKAGE) ? "other" : pkg.substring(PACKAGE.length() + 1);
                groups.computeIfAbsent(group, k -> new ArrayList<>()).add(painter);
            }
        }
        groups.values().forEach(list -> list.sort(Comparator.comparing(p -> p.getClass().getSimpleName())));
        return groups;
    }

    /**
     * Top-level class names under the package, from class folders and jars on the classpath.
     */
    private static List<String> classNames() {
        String path = PACKAGE.replace('.', '/');
        List<String> names = new ArrayList<>();
        try {
            Enumeration<URL> urls = PainterCatalog.class.getClassLoader().getResources(path);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                switch (url.getProtocol()) {
                    case "file" -> {
                        Path root = Path.of(url.toURI());
                        try (Stream<Path> files = Files.walk(root)) {
                            files.map(file -> root.relativize(file).toString().replace(root.getFileSystem().getSeparator(), "/"))
                                    .filter(PainterCatalog::isTopLevelClass)
                                    .forEach(file -> names.add(PACKAGE + "." + toClassName(file)));
                        }
                    }
                    case "jar" -> {
                        JarURLConnection connection = (JarURLConnection) url.openConnection();
                        connection.setUseCaches(false);
                        try (JarFile jar = connection.getJarFile()) {
                            jar.stream().map(JarEntry::getName)
                                    .filter(name -> name.startsWith(path + "/"))
                                    .map(name -> name.substring(path.length() + 1))
                                    .filter(PainterCatalog::isTopLevelClass)
                                    .forEach(name -> names.add(PACKAGE + "." + toClassName(name)));
                        }
                    }
                    default -> {
                        // other class loaders (e.g. jrt or custom) aren't scanned
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return names;
    }

    private static boolean isTopLevelClass(String relativePath) {
        return relativePath.endsWith(".class") && !relativePath.contains("$");
    }

    private static String toClassName(String relativePath) {
        return relativePath.substring(0, relativePath.length() - ".class".length()).replace('/', '.');
    }

    /**
     * New instance if the class is a concrete, public {@link LoadingPainter} with a public no-arg constructor.
     */
    private static LoadingPainter instantiate(String className) {
        try {
            Class<?> type = Class.forName(className, false, PainterCatalog.class.getClassLoader());
            int modifiers = type.getModifiers();
            if (!LoadingPainter.class.isAssignableFrom(type) || type.isInterface()
                    || Modifier.isAbstract(modifiers) || !Modifier.isPublic(modifiers)) {
                return null;
            }
            return (LoadingPainter) type.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            // helpers and painters without a no-arg constructor are skipped
            return null;
        }
    }
}
