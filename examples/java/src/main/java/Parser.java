import org.globalnames.parser.ScientificNameParser;

public class Parser {
    public static void main(String[] args) {
        String jsonStr = ScientificNameParser.instance()
                                             .fromString("Homo sapiens L.")
                                             .renderCompactJson();
        System.out.println(jsonStr);
    }
}
