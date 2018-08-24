package org.globalnames.parser.examples;

import org.globalnames.parser.ScientificNameParser;

public class ParserJava {
    public static void main(String[] args) {
        String jsonStr = ScientificNameParser
                .instance()
                .fromString("Homo sapiens L.")
                .renderJsonString(true);
        System.out.println(jsonStr);
    }
}
