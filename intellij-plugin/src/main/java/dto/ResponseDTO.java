package dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record ResponseDTO(String hash, List<TargetPrediction> predict) {
    public record TargetPrediction(String target, List<Prediction> predictions) {}
    public record Prediction(
        @SerializedName("class") String className,
        double confidence
    ) {}
}
