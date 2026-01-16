package org.zerock.com.example.legal;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class MarkdownUtil {
    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().escapeHtml(true).build();

    public static String mdToHtml(String md) {
        if (md == null) return "";
        Node doc = PARSER.parse(md);
        return RENDERER.render(doc);
    }
}
