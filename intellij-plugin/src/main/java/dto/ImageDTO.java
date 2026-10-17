package dto;

import com.google.gson.annotations.SerializedName;

public record ImageDTO(
        @SerializedName("project") String projectName,
        @SerializedName("hash") String imageHash,
        @SerializedName("image") String base64Image
) {}
