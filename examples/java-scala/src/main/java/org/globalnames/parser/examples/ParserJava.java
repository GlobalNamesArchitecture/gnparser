package org.globalnames.parser.examples;

import org.globalnames.parser.ScientificNameParserRenderer;

public class ParserJava {
    public static void main(String[] args) {
        String jsonStr = ScientificNameParserRenderer
                .instance()
                .fromString("Homo sapiens L.")
                .jsonRenderer()
                .renderCompactJson();
        System.out.println(jsonStr);
    }
}
