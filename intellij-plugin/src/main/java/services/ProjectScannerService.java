package services;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ProjectScannerService {

    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "py", "java", "kt", "kts", "c", "cpp", "cc", "cxx", "h", "hpp",
            "cs", "go", "rs", "rb", "php", "js", "jsx", "ts", "tsx",
            "scala", "swift", "dart", "lua", "r", "sh", "bash", "sql",
            "html", "htm", "css", "scss", "sass", "less", "json", "yaml",
            "yml", "xml", "toml", "md", "markdown", "ini", "properties",
            "clj", "coffee", "cmake", "graphql", "groovy", "jl", "m", "mm",
            "pl", "pm", "ps1", "rst", "vue"
    );

    public static final Set<String> IGNORED_DIRS = Set.of(
            "node_modules", ".git", ".svn", ".hg", ".vscode", ".idea",
            ".gradle", "build", "dist", "out", "target", "venv", ".venv",
            "env", ".env", "__pycache__", "sourcecodeminimaps"
    );

    public static boolean isEligibleFile(VirtualFile file) {
        if (file == null || !file.isValid() || file.isDirectory()) {
            return false;
        }
        String name = file.getName().toLowerCase();
        if (name.startsWith(".")) {
            return false;
        }
        String ext = file.getExtension();
        if (ext != null && SUPPORTED_EXTENSIONS.contains(ext.toLowerCase())) {
            return true;
        }
        return name.equals("dockerfile") || name.equals("makefile");
    }

    public static List<VirtualFile> findEligibleFiles(VirtualFile root) {
        List<VirtualFile> eligibleFiles = new ArrayList<>();
        if (root == null || !root.isValid()) {
            return eligibleFiles;
        }

        VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (file.isDirectory()) {
                    String name = file.getName().toLowerCase();
                    if (name.startsWith(".") || IGNORED_DIRS.contains(name)) {
                        return false; // Não entra em diretórios ignorados
                    }
                    return true;
                }
                if (isEligibleFile(file)) {
                    eligibleFiles.add(file);
                }
                return true;
            }
        });

        eligibleFiles.sort(Comparator.comparing(VirtualFile::getPath));
        return eligibleFiles;
    }

    public static Map<String, List<VirtualFile>> groupByExtension(List<VirtualFile> files) {
        Map<String, List<VirtualFile>> map = new HashMap<>();
        for (VirtualFile f : files) {
            String ext = f.getExtension();
            String key = (ext != null && !ext.isEmpty()) ? "." + ext.toLowerCase() : f.getName().toLowerCase();
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(f);
        }
        return map;
    }
}
