# SourceCodeMinimaps — IntelliJ IDEA Plugin

IntelliJ IDEA plugin for the MinimapSignatures ecosystem
(https://github.com/minimap-project). It turns the active source file into a
128x128 grayscale minimap, sends it to the analysis backend, and shows the
predicted programming language, project, and author in a balloon notification.

## How it works

1. Captures up to 128 lines from the active editor.
2. Maps each character to a grayscale pixel (the same encoding used to train the
   model) and builds a 128x128 `BufferedImage`.
3. Encodes the image as a base64 PNG and sends it to the backend via HTTP POST.
4. Displays the ranked predictions returned by the backend.

## Project structure

- `actions.SendAction`: handles the UI interaction and coordinates the capture.
- `services.ImageService`: pixel mapping and image generation.
- `requests.PostImageRequest`: network communication with the backend.
- `dto.ImageDTO`: data object for image transmission.

## Requirements

- JDK 17 or newer.
- A running analysis backend. Start the `back-end-pytorch` service first; by
  default the plugin calls `http://localhost:8000/api/v1/analyze/image`.

## Running in development

1. Make sure the backend is running on `http://localhost:8000`.
2. Open the project in IntelliJ IDEA.
3. Use the Gradle `Run Plugin` run configuration to launch a development IDE
   instance with the plugin enabled.
4. Open a file and trigger the action from the editor popup menu (right-click)
   or the main toolbar.

## Configuration

The backend URL and API key are defined as constants at the top of
`src/main/java/requests/PostImageRequest.java` (`API_URL` and `API_KEY`). Change
`API_URL` if your backend runs on a different host or port.
