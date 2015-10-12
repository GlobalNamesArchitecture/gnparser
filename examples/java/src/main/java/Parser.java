import org.globalnames.parser.ScientificNameParser;

public class Parser {
    public static void main(String[] args) {
        ScientificNameParser.Result result =
            ScientificNameParser.instance().fromString("Homo sapiens L.");
        String jsonStr = ScientificNameParser.instance().renderCompactJson(result);
        System.out.println(jsonStr);
    }
}
