package com.swingcraft4j.net;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Blocking GET helpers shared by the demo API clients; call off the EDT.
 */
public final class Http {

    // some hosts stall or reject requests that carry Java's default User-Agent
    private static final String USER_AGENT = "Mozilla/5.0 (swingcraft4j-example)";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private Http() {
    }

    public static String text(URI uri) throws IOException, InterruptedException {
        return new String(bytes(uri), StandardCharsets.UTF_8);
    }

    public static BufferedImage image(String url) throws IOException, InterruptedException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes(URI.create(url))));
        if (image == null) {
            throw new IOException("Unsupported image format");
        }
        return image;
    }

    private static byte[] bytes(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + uri.getHost());
        }
        return response.body();
    }
}
