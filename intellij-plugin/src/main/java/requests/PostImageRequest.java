package requests;

import com.google.gson.Gson;
import dto.ImageDTO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PostImageRequest {
    private static final String API_URL = "http://localhost:8000/api/v1/analyze/image";
    private static final String API_KEY = "m1n1m4p-pesquisa-2025";
    private static final Gson GSON = new Gson();

    public static HttpResponse<String> uploadImage(ImageDTO image) throws Exception {
        String jsonBody = GSON.toJson(image);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
