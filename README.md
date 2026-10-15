# MinimapSignatures — IntelliJ IDEA Plugin

IntelliJ IDEA plugin that converts the active source file into a 128×128 grayscale minimap, sends it to the analysis backend, and shows the predicted programming language, project, and author in a balloon notification.

## Paper

> **MinimapSignatures: Source-Code Identification via Grayscale Minimap Images**
> CBSoft 2026 — [paper.pdf](paper.pdf)

## Contents

```
intellij-plugin/
  src/                 — Java source code
  build.gradle.kts     — Gradle build script
  gradle/              — Gradle wrapper
  gradlew / gradlew.bat
LICENSE
README.md
paper.pdf
```

## Requirements

- JDK 17+
- IntelliJ IDEA 2023.1+ (Community or Ultimate)
- MinimapSignatures backend running on `http://localhost:8000`

## Installation

```bash
# open intellij-plugin/ in IntelliJ IDEA, then run:
./gradlew runIde
```

A sandbox IntelliJ IDE will open with the plugin enabled. Open any source file, right-click in the editor, and select **MinimapSignatures: Analyse**. A balloon notification with the top-3 predictions should appear.

The backend URL and API key are defined as constants at the top of `src/main/java/requests/PostImageRequest.java`.

## License

MIT — see [LICENSE](LICENSE).
