package dto;

import java.util.Base64;

public record ImageDTO(String projectName,String imageHash, String base64Image) {
}
