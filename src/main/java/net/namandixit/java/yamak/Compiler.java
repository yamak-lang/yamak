package net.namandixit.java.yamak;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Compiler {

    public boolean runString(String file, String src) {
        Lexer lexer = new Lexer(file, src);
        List<Token> tokens = lexer.scanTokens();

        for (Token token : tokens) {
            report(token.toString(), false);
        }

        if (lexer.hadError()) {
            report("Errors", true);
            var errs = lexer.getErrors();
            for (var e : errs) {
                report(e.toString(), true);
            }
            return false;
        }

        return true;
    }

    // TODO(naman): Replace this with displaying in the GUI later
    public void report(String msg, boolean err) {
        if (err) {
            System.err.println(msg);
        } else {
            System.out.println(msg);
        }
    }
}
