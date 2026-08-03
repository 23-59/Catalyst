import java.nio.file.*;
import java.util.*;

public class Fixer2 {
    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get("app/src/main/java/com/example/ui/TodoApp.kt"));
        List<String> newLines = new ArrayList<>();
        
        for (int i = 0; i < lines.size(); i++) {
            if (i == 4108) { // 4109 is index 4108
                continue;
            }
            newLines.add(lines.get(i));
        }
        
        Files.write(Paths.get("app/src/main/java/com/example/ui/TodoApp.kt"), newLines);
        System.out.println("Removed line 4109!");
    }
}
