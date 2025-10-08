import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    // Utility class for reading text files line by line
    // It returns all non-empty lines that do not start with '#' (comments)

    public static List<String> readAllLines(File f) throws IOException {
        List<String> out = new ArrayList<>();

        // Open the file and automatically close it after use
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;

            // Read the file line by line
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and lines starting with '#'
                if (!line.isEmpty() && !line.startsWith("#")) out.add(line);
            }
        }
        // Return all valid (non-empty, non-comment) lines
        return out;
    }
}
