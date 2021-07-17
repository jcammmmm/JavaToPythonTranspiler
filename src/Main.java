import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.FileOutputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 0)
            transpilePsicodeSource("../in/t42.psi");
        else {
            String[] samples = {
                "in/JavaSourceSample.java"
            };
            for (String filename : samples) {
                System.out.println("#######################################################");
                System.out.println("#######################################################");
                System.out.println("#######################################################");
                System.out.println("#######################################################");
                System.out.println("#######################################################");
                System.out.println("PSICODER SOURCE: " + filename);
                String pythonSource = transpilePsicodeSource(filename);

                filename = filename.replace("java", "py").replace("in", "res");
                FileOutputStream outputStream = new FileOutputStream(filename);
                byte[] strToBytes = pythonSource.getBytes();
                outputStream.write(strToBytes);

                outputStream.close();

                Thread.sleep(1000L);
            }
        }
    }

    private static String transpilePsicodeSource(String filename) throws IOException {
        Java8Lexer lexer = new Java8Lexer(CharStreams.fromFileName(filename));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        Java8Parser parser = new Java8Parser(tokenStream);
        ParseTree tree = parser.compilationUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        PythonTranspiler pythonTranspiler = new PythonTranspiler(filename.substring(3, filename.length() - 5));
        walker.walk(pythonTranspiler, tree);
        return pythonTranspiler.getTranspiledSource();
    }
}