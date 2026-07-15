# MinimapSignatures — IntelliJ IDEA Plugin

IntelliJ IDEA plugin for the MinimapSignatures ecosystem. It converts the
active source file into a 128×128 grayscale minimap image, sends it to the
analysis backend, and displays the top predictions for programming language,
project, and author in a balloon notification.

## Paper

This artifact is associated with the following accepted paper:

> **MinimapSignatures: Source-Code Identification via Grayscale Minimap Images**  
> CBSoft 2026  
> [📄 paper.pdf](paper.pdf)

## Repository Organisation

```
intellij-plugin/   ← source code (Gradle project, IntelliJ plugin)
LICENSE            ← MIT License
README.md          ← this file
paper.pdf          ← accepted paper
```

The `intellij-plugin/` subdirectory contains:
- `build.gradle.kts` — Gradle build script (IntelliJ Plugin Gradle DSL)
- `settings.gradle.kts` / `gradle.properties` — Gradle configuration
- `gradlew` / `gradlew.bat` — Gradle wrapper scripts
- `src/` — Java source code
  - `actions/SendAction.java` — UI interaction and capture coordination
  - `services/ImageService.java` — pixel mapping and image generation
  - `requests/PostImageRequest.java` — HTTP communication with the backend
  - `dto/ImageDTO.java` — data object for image transmission

## Requirements

- **JDK 17** or newer (must be set as the project SDK in IntelliJ IDEA)
- **IntelliJ IDEA** (Community or Ultimate, 2023.1+)
- A running instance of the **MinimapSignatures backend** on
  `http://localhost:8000` (see the `back-end` artifact)

## Installation

### Running in development mode

```bash
# 1. Clone / open the intellij-plugin/ folder in IntelliJ IDEA

# 2. Make sure the backend is running on http://localhost:8000

# 3. Run the plugin in a sandbox IDE using the Gradle wrapper:
./gradlew runIde
```

Alternatively, open the project in IntelliJ IDEA and use the pre-configured
**Run Plugin** run configuration (`.run/`).

**Verify the installation:**

1. The sandbox IntelliJ IDE opens.
2. Open any source file.
3. Right-click in the editor and select the **MinimapSignatures: Analyse**
   action (or trigger it from the main toolbar).
4. A balloon notification appears with the top-3 predictions for language,
   project, and author.

### Configuration

The backend URL and API key are constants at the top of
`intellij-plugin/src/main/java/requests/PostImageRequest.java`
(`API_URL` and `API_KEY`). Change `API_URL` if your backend runs on a
different host or port.

## License

This artifact is distributed under the **MIT License** — see [`LICENSE`](LICENSE).
