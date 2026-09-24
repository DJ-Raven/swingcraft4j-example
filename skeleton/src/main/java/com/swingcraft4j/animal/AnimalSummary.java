package com.swingcraft4j.animal;

/**
 * A search hit: enough to list the animal and to fetch its details.
 */
public record AnimalSummary(int id, String commonName, String scientificName, String conservation, String thumbUrl) {
}
