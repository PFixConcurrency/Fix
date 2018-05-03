package fix.analyzefile;

import fix.entity.lock.ExistLock;
import fix.io.ExamplesIO;
import org.eclipse.jdt.core.dom.*;
import p_heu.entity.ReadWriteNode;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UseASTAnalysisClass {

    static String className = "";//类的名字，以后用来比较用
    static boolean flagMember = false;//是不是成员变量
    static boolean flagConstruct = false;//是不是构造函数

    static boolean rw1Match = false;//第一个读写点有没有匹配
    static boolean rw2Match = false;//第二个读写点有没有匹配

    static ASTNode rw1Node = null;//匹配第一个读写点
    static ASTNode rw2Node = null;//匹配第二个读写点

    static boolean flagSameFunction = false;//是不是在一个函数中

    static LockLine lockLine = new LockLine();//用来记录加锁的起始和终止行数

    static ExistLock existLock = new ExistLock();//用来记录已有的sync块所在的行数，最后根据它获取锁的名称

    private static String objectName;//加的静态变量的名称



    public static void main(String[] args) {
//        System.out.println(isConstructOrIsMemberVariableOrReturn(11, 12, ImportPath.examplesRootPath + "\\exportExamples\\" + ImportPath.projectName + "\\Account.java"));
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
        lockLine = useASTCheckVariableInLock(444, 448, "D:/Patch/examples/stringbuffer/StringBuffer.java");
        System.out.println(lockLine.getFirstLoc());
        System.out.println(lockLine.getLastLoc());
    }

    //判断变量是不是在if(),while(),for()的判断中
    //注意是判断中，就是圆括号中
    //如果是的话，要稍微修改一下加锁的函数
    public static LockLine changeLockLine(int firstLoc, int lastLoc, String filePath) {
//        System.out.println(firstLoc + "===========");
        lockLine.setFirstLoc(firstLoc);
        lockLine.setLastLoc(lastLoc);
        useASTChangeLine(firstLoc, lastLoc, filePath);
        return lockLine;
    }

    //判断是不是成员变量或者构造函数
    public static boolean isConstructOrIsMemberVariableOrReturn(int firstLoc, int lastLoc, String filePath) {
        useASTAnalysisConAndMem(firstLoc, lastLoc, filePath);
        return flagConstruct || flagMember;
    }

    //利用AST来检查加锁之后，会不会出现变量定义在锁内，但是变量使用在锁歪的情况
    public static LockLine useASTCheckVariableInLock(int lockStart, int locEnd, String filePath) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        lockLine.setFirstLoc(lockStart);
        lockLine.setLastLoc(locEnd);
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {

            //在锁中，有哪些变量被定义了
            Set<String> varDefInSync = new HashSet<String>();

            //函数结束的行数
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

    //利用AST将添加的对象同步与原来的this同步合并
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
                //先找不交叉的情况
                if(firstLoc > end || lastLoc < start){
                    flagCross = false;
                }
                //交叉需要改变行数
                if (flagCross) {
                    lockLine.setFirstLoc(Math.min(firstLoc,start));
                    lockLine.setLastLoc(Math.max(lastLoc,end));
                }
                return super.visit(node);
            }
        });

        return lockLine;
    }

    //利用AST来添加全局静态变量，用于加全局锁
    public static String useASTToaddStaticObject(String filePath) {
        //读取文件
        //因为会出现在类前面有一大堆注释的情况
        //ast计算行数的时候会从注释开始算
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
        examplesIO.addStaticObject(index + 1, objectName, filePath);

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

    //利用AST来寻找加锁的行数
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

    //利用AST来改变加锁位置
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
                    lockLine.setLastLoc(Math.max(lastLoc, end));//此处lastloc不要加1，因为加锁的时候已经是+1了
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(SwitchStatement node) {
                int start = cu.getLineNumber(node.getStartPosition());
                int end = cu.getLineNumber(node.getStartPosition() + node.getLength());
                if ((firstLoc >= start && firstLoc <= end) || (((lastLoc + 1) >= start && (lastLoc + 1) <= end))) {//此处与if语句等情况稍微有一些不同
                    lockLine.setFirstLoc(Math.min(firstLoc, start));
                    lockLine.setLastLoc(Math.max(lastLoc, end));//此处lastloc不要加1，因为加锁的时候已经是+1了
                }
                return super.visit(node);
            }
        });
    }


    //是不是在一个函数中
    public static boolean assertSameFunction(List<ReadWriteNode> nodesList, String filePath) {
        ReadWriteNode rwn1 = nodesList.get(0);
        ReadWriteNode rwn2 = nodesList.get(1);

        //判断是不是在一个函数中
        useASTAssertSameFun(rwn1, rwn2, filePath);
        return flagSameFunction;
    }

    //用AST来判别是不是在一个函数中
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

            //定义变量
            public boolean visit(VariableDeclarationFragment node) {

                return true;
            }

            //变量
            public boolean visit(SimpleName node) {

                if (node.toString().equals(rwn1.getField()) && cu.getLineNumber(node.getStartPosition()) == Integer.parseInt(rwn1.getPosition().split(":")[1])) {
                    rw1Match = true;
                    rw1Node = node;
                }
                if (node.toString().equals(rwn2.getField()) && cu.getLineNumber(node.getStartPosition()) == Integer.parseInt(rwn2.getPosition().split(":")[1])) {
                    rw2Match = true;
                    rw2Node = node;
                }

                if (rw1Match && rw2Match) {//两个读写点都找到的时候
                    flagSameFunction = isSameFunction(rw1Node, rw2Node);
                }
                return true;
            }
        });
    }

    //判断两个结点是不是在一个函数中
    private static boolean isSameFunction(ASTNode rw1Node, ASTNode rw2Node) {
        //找到第一个结点在哪个函数中
        ASTNode iNode = rw1Node.getParent();
        while (!(iNode instanceof MethodDeclaration)) {
            iNode = iNode.getParent();
        }
        //找到第二个结点在哪个函数中
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

    //将文件处理成buffer array
    public static char[] getFileContents(File file) {
        // char array to store the file contents in
        char[] contents = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                // 附加内容和丢失的新行。
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

            //定义变量
            public boolean visit(VariableDeclarationFragment node) {
                //判断是不是成员变量
                if (cu.getLineNumber(node.getStartPosition()) >= firstLoc && cu.getLineNumber(node.getStartPosition()) <= lastLoc) {
                    flagMember = isMemberVariable(node);
                }

                return true;
            }

            //变量
            public boolean visit(SimpleName node) {
                if (cu.getLineNumber(node.getStartPosition()) >= firstLoc && cu.getLineNumber(node.getStartPosition()) <= lastLoc) {
                    flagConstruct = isConstruct(node);
                }
                return true;
            }
        });
    }

    //判断是不是成员变量
    private static boolean isMemberVariable(ASTNode node) {
        if (node.getParent().getParent() instanceof TypeDeclaration) {
            return true;
        } else {
            return false;
        }
    }

    //判断是不是构造函数
    private static boolean isConstruct(ASTNode node) {
        /**
         * 检查结点的所有父节点，看看有没有一个是构造函数
         * 判断节点类型是函数，节点名字与类名相同，则是构造函数
         */
        while (!(node instanceof TypeDeclaration)) {
            if ((node instanceof MethodDeclaration) && (((MethodDeclaration) node).getName().toString().equals(className))) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    //表示加锁行数
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
