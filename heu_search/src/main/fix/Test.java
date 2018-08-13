package fix;

import fix.analyzefile.UseASTAnalysisClass;
import fix.entity.ImportPath;
import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

public class Test {


    public static void main(String[] args) throws Exception {

        /*BufferedReader br = null;
        String read = "";//用来读
        String result = "";//用来处理
        int line = 0;
        int poi = 7;
        String filePath = ImportPath.examplesRootPath + "/exportExamples/" + "atmoerror/BankAccount.java";
        br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
        while (((read = br.readLine()) != null)) {
            line++;
            if (line == poi) {//找到哪一行
                String field = "total";//得的变量
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("^.*?(((\\w+\\.)+)" + field + ").*$");
                Matcher m = p.matcher(read);
                if (m.matches()) {
                    result = m.group(1);
                    int indexTemp = result.lastIndexOf('.');
                    if (indexTemp == -1) {
                        result = "this";
                    } else {
                        result = result.substring(0, indexTemp);
                    }
                } else {
                    result = "777";
                }
            }
        }

        System.out.println(result);*/
        Set s = new HashSet();
        s.add(1);
        s.add(2);
        s.add(3);

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

            //在锁中，有哪些变量被定义了
            Set<String> varDefInSync = new HashSet<String>();


            @Override
            public boolean visit(VariableDeclarationFragment node) {
                this.varDefInSync.add(node.getName().getIdentifier());
                return super.visit(node);
            }

            @Override
            public boolean visit(SimpleName node) {

//                if(this.varDefInSync.contains(node.getIdentifier())){
                System.out.println(node.getIdentifier());
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
