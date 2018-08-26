package fix.analyzefile;

import fix.entity.lock.ExistLock;
import fix.io.ExamplesIO;
import fix.run.Propagate;
import org.eclipse.jdt.core.dom.*;
import p_heu.entity.ReadWriteNode;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UseASTAnalysisClass {

    static String className = "";
    static boolean flagMember = false;//Is it a member variable
    static boolean flagConstruct = false;//In the constructor or not

    static boolean rw1Match = false;//whether The first read-write point match
    static boolean rw2Match = false;//whether The second read-write point match

    static ASTNode rw1Node = null;//Match the first read-write point
    static ASTNode rw2Node = null;//Match the second read-write point

    static boolean flagSameFunction = false;//whether in same function

    static LockLine lockLine = new LockLine();//Used to record the number of start and end rows for sync

    static ExistLock existLock = new ExistLock();//This is used to record the number of rows in an existing sync block, and finally get the name of the lock based on it

    private static String objectName;//The name of the added static variable

    static boolean flagInRun = false;

    static boolean flagField = false;//whether variable definition


    public static void main(String[] args) {
//        System.out.println(isConstructOrIsMemberVariable(11, 12, ImportPath.examplesRootPath + "\\exportExamples\\" + ImportPath.projectName + "\\Account.java"));
        /*List<ReadWriteNode> nodesList = new ArrayList<ReadWriteNode>();
        nodesList.add(new ReadWriteNode(1, "linkedlist.MyListNode@18d", "_next", "WRITE", "Thread-4", "linkedlist/MyLinkedList.java:52"));
        nodesList.add(new ReadWriteNode(2, "linkedlist.MyListNode@18d", "_next", "WRITE", "Thread-4", "linkedlist/MyLinkedList.java:53"));
        System.out.println(assertSameFunction(nodesList, ImportPath.examplesRootPath + "\\examples\\" + ImportPath.projectName + "\\MyLinkedList.java"));*/
        /*ReadWriteNode readWriteNode = new ReadWriteNode(1, "consisitency.Main", "a", "WRITE", "Thread-2", "consisitency/Main.java:15");
        ReadWriteNode readWriteNode1 = new ReadWriteNode(2, "consisitency.Main", "a", "WRITE", "Thread-3", "consisitency/Main.java:15");
        List<ReadWriteNode> rwl = new ArrayList<ReadWriteNode>();
        rwl.add(readWriteNode);
        rwl.add(readWriteNode1);
        System.out.println(assertSameFunction(rwl,ImportPath.examplesRootPath + "\\examples\\" + ImportPath.projectName + "\\CheckField.java"));*/
//        useASTCFindLockLine(readWriteNode, ImportPath.examplesRootPath + "\\exportExamples\\" + ImportPath.projectName + "\\Account.java");
        /*useASTChangeLine(49, 50, "D:\\Patch\\examples\\critical\\Critical.java");
        System.out.println(lockLine.getFirstLoc() + "," + lockLine.getLastLoc());*/
//        System.out.println(useASTToaddStaticObject(ImportPath.examplesRootPath + "\\examples\\" + ImportPath.projectName + "\\Main.java"));
//        System.out.println(useASTCheckWhetherLock(42, ImportPath.examplesRootPath + "\\examples\\" + ImportPath.projectName + "\\MyLinkedList.java"));

        /*lockLine = useASTCheckVariableInLock(444, 448, "D:/Patch/examples/stringbuffer/StringBuffer.java");
        System.out.println(lockLine.getFirstLoc());
        System.out.println(lockLine.getLastLoc());*/
        System.out.println(UseASTAnalysisClass.checkInRun(39, 40, "C:\\Users\\lhr\\Desktop\\pfix\\FixExamples\\examples\\airline\\Bug.java"));
    }

    //Whether the variable is in the judgment of if(),while(),for()
    public static LockLine changeLockLine(int firstLoc, int lastLoc, String filePath) {
//        System.out.println(firstLoc + "===========");
        lockLine.setFirstLoc(firstLoc);
        lockLine.setLastLoc(lastLoc);
        useASTChangeLine(firstLoc, lastLoc, filePath);
        return lockLine;
    }

    //Determines whether it is a member variable or a constructor
    public static boolean isConstructOrIsMemberVariable(int firstLoc, int lastLoc, String filePath) {
        //It needs to be reset every time
        flagConstruct = false;
        flagMember = false;
        useASTAnalysisConAndMem(firstLoc, lastLoc, filePath);
        return flagConstruct || flagMember;
    }

    //Will the variable be defined in the sync, but the variable iused outside
    public static LockLine useASTCheckVariableInLock(int lockStart, int locEnd, String filePath) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        lockLine.setFirstLoc(lockStart);
        lockLine.setLastLoc(locEnd);
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {

            //In the lock, what are the variables are defined
            Set<String> varDefInSync = new HashSet<String>();

            //The number of lines at the end of the function
            int functionEnd = 0;

            @Override
            public boolean visit(MethodDeclaration node) {
                if (cu.getLineNumber(node.getStartPosition()) <= lockStart && cu.getLineNumber(node.getStartPosition() + node.getLength()) >= locEnd) {
                    this.functionEnd = cu.getLineNumber(node.getStartPosition() + node.getLength());
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(VariableDeclarationFragment node) {
                if (cu.getLineNumber(node.getStartPosition()) >= lockStart && cu.getLineNumber(node.getStartPosition() + node.getLength()) <= locEnd) {
                    this.varDefInSync.add(node.getName().getIdentifier());
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(SimpleName node) {
                if (cu.getLineNumber(node.getStartPosition()) > locEnd && cu.getLineNumber(node.getStartPosition() + node.getLength()) <= this.functionEnd) {
                    if (this.varDefInSync.contains(node.getIdentifier())) {
                        lockLine.setLastLoc(cu.getLineNumber(node.getStartPosition() + node.getLength()));
                    }
                }
                return super.visit(node);
            }
        });
        return lockLine;
    }

    //Use the AST to synchronize the added objects with the original this synchronization
    public static LockLine useASTAdjustThisLock(int firstLoc, int lastLoc, String filePath) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(SynchronizedStatement node) {

                int start = cu.getLineNumber(node.getStartPosition());
                int end = cu.getLineNumber(node.getStartPosition() + node.getLength());

                boolean flagCross = true;
                //Find the uncrossed case
                if (firstLoc > end || lastLoc < start) {
                    flagCross = false;
                }
                //cross need to change line
                if (flagCross) {
                    lockLine.setFirstLoc(Math.min(firstLoc, start));
                    lockLine.setLastLoc(Math.max(lastLoc, end));
                }
                return super.visit(node);
            }
        });

        return lockLine;
    }

    //Use AST to add global static variables for adding global locks
    public static String useASTToaddStaticObject(String filePath) {
        //read file
        //Because it's going to have a bunch of comments in front of the class
        //When the ast calculates the number of rows, it starts with the comment
        BufferedReader br = null;
        String read = "";
        int index = 1;
        String[] split = filePath.split("/");
        String className = split[split.length - 1].split("\\.")[0];

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "GBK"));
            while (((read = br.readLine()) != null)) {
                if (read.contains("class") && read.contains(className)) {
                    break;
                } else {
                    index++;
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ExamplesIO examplesIO = ExamplesIO.getInstance();
        objectName = "objectFix";
        examplesIO.addStaticObject(index + 2, objectName, filePath);

        /*ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {

                int index = cu.getLineNumber(node.getStartPosition()) + 1;



                ExamplesIO examplesIO = ExamplesIO.getInstance();
                objectName = "objectFix";
                examplesIO.addStaticObject(index, objectName, filePath);
                return super.visit(node);
            }
        });*/
        return objectName;
    }

    //Use AST to find the line number of sync
    public static ExistLock useASTCFindLockLine(ReadWriteNode readWriteNode, String filePath) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName node) {


                if (readWriteNode.getField().equals(node.toString()) && Integer.parseInt(readWriteNode.getPosition().split(":")[1]) == cu.getLineNumber(node.getStartPosition())) {

                    ASTNode iNode = node.getParent();
                    while (!(iNode instanceof SynchronizedStatement)) {
                        iNode = iNode.getParent();
                        if (iNode == null) {
                            break;
                        }
                    }
                    existLock = new ExistLock("", cu.getLineNumber(iNode.getStartPosition()), cu.getLineNumber(iNode.getStartPosition() + iNode.getLength()));
                }
                return super.visit(node);
            }
        });
        return existLock;
    }

    //Use AST to change the sync position
    public static void useASTChangeLine(int firstLoc, int lastLoc, String filePath) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {

            @Override
            public boolean visit(InfixExpression node) {
                ASTNode parent = node.getParent();
                int start = cu.getLineNumber(parent.getStartPosition());
                int end = cu.getLineNumber(parent.getStartPosition() + parent.getLength());
                if ((firstLoc <= start && (lastLoc + 1) <= end && (lastLoc + 1) > start) || (firstLoc > start && firstLoc <= end && (lastLoc + 1) > end)) {//加锁区域与代码块交叉的里面
                    lockLine.setFirstLoc(Math.min(firstLoc, start));
                    lockLine.setLastLoc(Math.max(lastLoc, end));//Do not add 1 to lastloc because it is +1 when we lock it
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(SwitchStatement node) {
                int start = cu.getLineNumber(node.getStartPosition());
                int end = cu.getLineNumber(node.getStartPosition() + node.getLength());
                if ((firstLoc >= start && firstLoc <= end) || (((lastLoc + 1) >= start && (lastLoc + 1) <= end))) {//This is slightly different from the case of an if statement and so on
                    lockLine.setFirstLoc(Math.min(firstLoc, start));
                    lockLine.setLastLoc(Math.max(lastLoc, end));//Do not add 1 to lastloc because it is +1 when we lock it
                }
                return super.visit(node);
            }
        });
    }


    //Is it a defined position
    public static boolean useASTCheckDec(ReadWriteNode readWriteNode, String filePath) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(FieldDeclaration node) {
                //we compare the same number of rows without comparing the variable names
                if(cu.getLineNumber(node.getStartPosition()) == Integer.parseInt(readWriteNode.getPosition().split(":")[1])){
                    flagField = true;

                }
                return true;
            }
        });

        return flagField;
    }

    //whether same function
    public static boolean assertSameFunction(List<ReadWriteNode> nodesList, String filePath) {
        ReadWriteNode rwn1 = nodesList.get(0);
        ReadWriteNode rwn2 = nodesList.get(1);

        useASTAssertSameFun(rwn1, rwn2, filePath);
        return flagSameFunction;
    }

    //use AST to check whether same function
    private static void useASTAssertSameFun(ReadWriteNode rwn1, ReadWriteNode rwn2, String filePath) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {

            public boolean visit(TypeDeclaration node) {
                className = node.getName().toString();
                return true;
            }

            //define variable
            public boolean visit(VariableDeclarationFragment node) {

                return true;
            }

            //variable
            public boolean visit(SimpleName node) {

                if (node.toString().equals(rwn1.getField()) && cu.getLineNumber(node.getStartPosition()) == Integer.parseInt(rwn1.getPosition().split(":")[1])) {
                    rw1Match = true;
                    rw1Node = node;
                }
                if (node.toString().equals(rwn2.getField()) && cu.getLineNumber(node.getStartPosition()) == Integer.parseInt(rwn2.getPosition().split(":")[1])) {
                    rw2Match = true;
                    rw2Node = node;
                }

                if (rw1Match && rw2Match) {//When both read-write points are found
                    flagSameFunction = isSameFunction(rw1Node, rw2Node);
                }
                return true;
            }
        });
    }

    //whether two nodes in samefunction
    private static boolean isSameFunction(ASTNode rw1Node, ASTNode rw2Node) {
        //Find which function the first node is in
        ASTNode iNode = rw1Node.getParent();
        while (!(iNode instanceof MethodDeclaration)) {
            iNode = iNode.getParent();
        }
        //Find which function the second node is in
        ASTNode jNode = rw2Node.getParent();
        while (!(jNode instanceof MethodDeclaration)) {
            jNode = jNode.getParent();
        }
        if (iNode.equals(jNode)) {
            return true;
        } else {
            return false;
        }
    }

    //The file is processed into buffer array
    public static char[] getFileContents(File file) {
        // char array to store the file contents in
        char[] contents = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                // Additional content and missing new lines.
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

    public static void useASTAnalysisConAndMem(int firstLoc, int lastLoc, String filePath) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);


        cu.accept(new ASTVisitor() {


            public boolean visit(TypeDeclaration node) {
                className = node.getName().toString();
                return true;
            }

            //define variabnle
            public boolean visit(VariableDeclarationFragment node) {
                //check whether member varibale
                if (cu.getLineNumber(node.getStartPosition()) >= firstLoc && cu.getLineNumber(node.getStartPosition()) <= lastLoc) {
                    flagMember = isMemberVariable(node);
                }

                return true;
            }

            //variable
            public boolean visit(SimpleName node) {
                if (cu.getLineNumber(node.getStartPosition()) >= firstLoc && cu.getLineNumber(node.getStartPosition()) <= lastLoc) {
                    flagConstruct = isConstruct(node);
                }
                return true;
            }
        });
    }

    public static void addPropagateSet(int line,String filePath) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);


        cu.accept(new ASTVisitor() {

            //变量
            public boolean visit(SimpleName node) {
                if (cu.getLineNumber(node.getStartPosition()) == line) {
                    ASTNode parent = node.getParent();
                    while(!(parent instanceof MethodDeclaration)){
                        parent = parent.getParent();
                    }
                    int start = cu.getLineNumber(parent.getStartPosition());
                    int end = cu.getLineNumber(parent.getStartPosition() + parent.getLength());
                    Propagate.addSet(start, end);
                }
                return true;
            }
        });
    }

    //whether member variable
    private static boolean isMemberVariable(ASTNode node) {
        if (node.getParent().getParent() instanceof TypeDeclaration) {
            return true;
        } else {
            return false;
        }
    }

    //whether construct
    private static boolean isConstruct(ASTNode node) {
        /**
         * Check all the parent nodes of the node to see if there is a constructor
         * Determine that the node type is a function, and that the node name is the same as the class name, which is the constructor
         */
        while (!(node instanceof TypeDeclaration)) {
            if ((node instanceof MethodDeclaration) && (((MethodDeclaration) node).getName().toString().contains(className))) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    public static boolean checkInRun(int firstLoc, int lastLoc, String filepath) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filepath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);


        cu.accept(new ASTVisitor() {

            @Override
            public boolean visit(MethodDeclaration node) {

                if (node.getName().toString().equals("run")) {
                    int start = cu.getLineNumber(node.getStartPosition());
                    int end = cu.getLineNumber(node.getStartPosition() + node.getLength());
                    if ((firstLoc >= start && firstLoc <= end) && (lastLoc >= start && lastLoc <= end)) {
                        flagInRun = true;
                    } else {
                        flagInRun = false;
                    }
                }
                return super.visit(node);
            }

        });
        return flagInRun;
    }

    public static void useASTToaddVoli(String filePath) {
        //read file
        //Because it's going to have a bunch of comments in front of the class
        //When the ast calculates the number of rows, it starts with the comment
        BufferedReader br = null;
        String read = "";
        int index = 1;
        String[] split = filePath.split("/");
        String className = split[split.length - 1].split("\\.")[0];

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "GBK"));
            while (((read = br.readLine()) != null)) {
                if (read.contains("class") && read.contains(className)) {
                    break;
                } else {
                    index++;
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ExamplesIO examplesIO = ExamplesIO.getInstance();
        examplesIO.addVolatileDefine(index + 2, "volatile boolean flagFix = false;", filePath);
    }

    //sync line
    public static class LockLine {
        int firstLoc;
        int lastLoc;

        public int getFirstLoc() {
            return firstLoc;
        }

        public void setFirstLoc(int firstLoc) {
            this.firstLoc = firstLoc;
        }

        public int getLastLoc() {
            return lastLoc;
        }

        public void setLastLoc(int lastLoc) {
            this.lastLoc = lastLoc;
        }
    }
}
