package com.swingcraft4j.login;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import com.swingcraft4j.login.background.Palette.Accent;

/**
 * The scene, theme and remembered username, kept in .login3d.properties in the home folder.
 */
public class Settings {

    private static final File FILE = new File(System.getProperty("user.home"), ".login3d.properties");

    public int scene;
    public Accent accent = Accent.BLUE;
    public boolean dark = true;
    public boolean remember;
    public String username = "";

    public static Settings load() {
        Settings settings = new Settings();
        if (!FILE.isFile()) {
            return settings;
        }
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(FILE)) {
            properties.load(in);
        } catch (IOException e) {
            // start with the defaults
            return settings;
        }
        try {
            settings.scene = Integer.parseInt(properties.getProperty("scene", "0"));
        } catch (NumberFormatException e) {
            settings.scene = 0;
        }
        try {
            settings.accent = Accent.valueOf(properties.getProperty("accent", Accent.BLUE.name()));
        } catch (IllegalArgumentException e) {
            settings.accent = Accent.BLUE;
        }
        settings.dark = Boolean.parseBoolean(properties.getProperty("dark", "true"));
        settings.remember = Boolean.parseBoolean(properties.getProperty("remember", "false"));
        settings.username = settings.remember ? properties.getProperty("username", "") : "";
        return settings;
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("scene", String.valueOf(scene));
        properties.setProperty("accent", accent.name());
        properties.setProperty("dark", String.valueOf(dark));
        properties.setProperty("remember", String.valueOf(remember));
        properties.setProperty("username", remember ? username : "");
        try (OutputStream out = new FileOutputStream(FILE)) {
            properties.store(out, "Login 3D");
        } catch (IOException e) {
            // the choice is only kept for this run
        }
    }
}
