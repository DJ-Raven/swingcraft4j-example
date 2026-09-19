package com.swingcraft4j.treehoveraction.model;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;

/**
 * A tree node's payload: a display value plus an optional {@link Type} icon. Meant to be used
 * as the {@code userObject} of a {@link javax.swing.tree.DefaultMutableTreeNode}.
 */
public class Item {

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Item(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public Item(String value) {
        this(Type.DEFAULT, value);
    }

    private Type type;
    private String value;

    @Override
    public String toString() {
        return value;
    }

    /**
     * The icon an item displays. {@code DEFAULT} has none; every other constant lazily loads an
     * SVG resource named after its own lowercase name (e.g. {@code JAVA} -> {@code java.svg}).
     */
    public enum Type {

        DEFAULT, PYTHON, FLUTTER, C, C_SHARP, CPP, DOCKER, JAVA, NODEJS, HTML_5, MARIADB, MYSQL;

        private final Icon icon;

        Type() {
            String name = this.toString().toLowerCase();
            if (name.equals("default")) {
                icon = null;
            } else {
                icon = new FlatSVGIcon("com/swingcraft4j/treehoveraction/images/" + this.toString().toLowerCase() + ".svg");
            }
        }

        public Icon getIcon() {
            return icon;
        }
    }
}
