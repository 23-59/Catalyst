import java.nio.file.*;
import java.util.*;
public class Main {
    public static void main(String[] args) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("app/src/main/java/com/example/ui/TodoApp.kt")));
        String[] lines = content.split("\n");
        Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().startsWith("//")) continue;
            for (char c : line.toCharArray()) {
                if (c == '{') stack.push(i + 1);
                if (c == '}') {
                    if (!stack.isEmpty()) {
                        int start = stack.pop();
                        if (start == 2961) System.out.println("Box(2961) closes at: " + (i + 1));
                        if (start == 2928) System.out.println("Row(2928) closes at: " + (i + 1));
                        if (start == 3017) System.out.println("Inner Row(3017) closes at: " + (i + 1));
                        if (start == 3430) System.out.println("Composition(3430) closes at: " + (i + 1));
                    }
                }
            }
        }
    }
}
