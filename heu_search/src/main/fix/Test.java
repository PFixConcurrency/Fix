package fix;

import fix.analyzefile.UseASTAnalysisClass;
import fix.entity.ImportPath;
import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

public class Test {


    public static void main(String[] args) throws Exception {

System.out.println(123);

    }


    public static void useASTCFindLockLine(String filePath) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(FieldDeclaration node) {
                System.out.println(node + "===========");
                return super.visit(node);
            }
        });

    }

    static void test() {
        while (true) {
            System.out.println("----------==========");
        }
    }

    static void listFiles(List<File> files, File dir) {
        File[] listFiles = dir.listFiles();
        for (File f : listFiles) {
            if (f.isFile()) {
                files.add(f);
            } else if (f.isDirectory()) {
                listFiles(files, f);
            }
        }
    }


    public static void useASTChangeLine(String filePath) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {


            Set<String> varDefInSync = new HashSet<String>();


            @Override
            public boolean visit(VariableDeclarationFragment node) {
                this.varDefInSync.add(node.getName().getIdentifier());
                return super.visit(node);
            }

            @Override
            public boolean visit(SimpleName node) {

//                if(this.varDefInSync.contains(node.getIdentifier())){
                System.out.println(node.toString());
                System.out.println(cu.getLineNumber(node.getStartPosition()));
//                }
                return super.visit(node);
            }

        });
    }

    //chanage file content to buffer array
    public static char[] getFileContents(File file) {
        // char array to store the file contents in
        char[] contents = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                // append the content and the lost new line.
                sb.append(line + "\n");
            }
            contents = new char[sb.length()];
            sb.getChars(0, sb.length() - 1, contents, 0);

            assert (contents.length > 0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return contents;
    }
}
