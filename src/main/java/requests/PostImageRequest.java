package requests;

import dto.ImageDTO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class PostImageRequest {
    public static HttpResponse<String> uploadImage(ImageDTO image) throws Exception {
        String url = "http://98.85.228.131:8000/api/v1/analyze/image";
        String apiKey = System.getenv("MINIMAPS_API");
        String jsonBody = String.format(
                "{\"project\": \"%s\", \"hash\": \"%s\", \"image\": \"%s\"}",
                image.projectName().replace("\"", "\\\""),
                image.imageHash(),
                image.base64Image()
        );

        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("Coloque a key da api nos variaveis do sistema.");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
