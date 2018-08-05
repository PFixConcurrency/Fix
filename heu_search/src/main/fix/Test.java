package fix;

import fix.analyzefile.UseASTAnalysisClass;
import org.eclipse.jdt.core.dom.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Test {


    public static void main(String[] args) {
//        useASTChangeLine(444, 448, 439, 451, "D:/Patch/examples/stringbuffer/StringBuffer.java");
//        System.out.println(CheckWhetherLocked.check("wrongLock/WrongLock.java:30", "value", ImportPath.examplesRootPath + "/out/production/Patch", "D:\\Patch\\examples\\wrongLock\\WrongLock.java"));
//        useASTChangeLine("D:/Patch/examples/wrongLock/wrongLock.java");
        /*ReadWriteNode node = new ReadWriteNode(1,"accountsubtype.BusinessAccount@169","amount","READ","Thread-1","accountsubtype/PersonalAccount.java:11");
        System.out.println(Fix.acquireLockName(node));*/

//        String read = "tmp = new MyListNode(x, p._current._next);";

        /*Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("123",1);
        map.put("123",1);*/
       /* Set s = new HashSet();
        s.add(1);
        s.add(1);
        s.add(2);
        for(Object o : s)
            System.out.println(o);
        System.out.println(s.contains(3));*/
//        useASTCFindLockLine("C:\\Users\\lhr\\Desktop\\pfix\\FixExamples\\exportExamples\\atmoerror\\BankAccount.java");

        Set s = new HashSet();
        s.add(100);
        for(int i = 1;i < 4;i++)
            s.add(i);
        s.add(0);
        s.add(1);
        for(Object o : s)
            System.out.println(o);

    }


    public static void useASTCFindLockLine(String filePath) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(FieldDeclaration node) {
                System.out.println(node+ "===========");
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
