package com.swingcraft4j.animal;

import java.awt.image.BufferedImage;

/**
 * Full animal details with the decoded profile photo (null when the taxon has none).
 */
public record Animal(int id, String commonName, String scientificName, String group, String rank,
                     String conservation, int observations, String description, BufferedImage photo) {
}
