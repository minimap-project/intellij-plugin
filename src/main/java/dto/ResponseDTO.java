package dto;

import java.util.List;

public record ResponseDTO(String hash, List<String> predict) {
}
