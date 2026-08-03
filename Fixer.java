import java.nio.file.*;
import java.util.*;

public class Fixer {
    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get("app/src/main/java/com/example/ui/TodoApp.kt"));
        List<String> newLines = new ArrayList<>();
        
        for (int i = 0; i < lines.size(); i++) {
            if (i == 3425) { // 3426 is index 3425
                continue; // skip line 3426
            }
            if (i == 3609) { // 3610 is index 3609
                continue; // skip extra brace I added
            }
            newLines.add(lines.get(i));
        }
        
        Files.write(Paths.get("app/src/main/java/com/example/ui/TodoApp.kt"), newLines);
        System.out.println("Done!");
    }
}
