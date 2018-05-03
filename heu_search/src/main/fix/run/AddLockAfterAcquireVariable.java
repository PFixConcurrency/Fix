package fix.run;

import fix.entity.ImportPath;
import fix.entity.MatchVariable;
import fix.io.InsertCode;
import org.eclipse.jdt.core.dom.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AddLockAfterAcquireVariable {
    static String dirPath = ImportPath.examplesRootPath + "\\exportExamples\\" + ImportPath.projectName;
    static Set<String> variableSet = new HashSet<String>();
    static String className = "";//类的名字，以后用来比较用

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

    public static void main(String[] args) {

        /*//获取相关变量
        AcquireVariableInSameLock acquireVariableInSameLock = new AcquireVariableInSameLock();
        Vector<String> v = acquireVariableInSameLock.getOneLockfieldVector();
        for (String s : v)
            variableSet.add(s);

        //单例
        ExamplesIO examplesIO = ExamplesIO.getInstance();
        //将项目从examples复制到exportExamples，并且修改当前dirPath路径
        dirPath = examplesIO.copyFromOneDirToAnotherAndChangeFilePath("examples","exportExamples", dirPath);
        //对目录下的每个文件，都执行一次lock
        File file = new File(dirPath);
        File[] fileArr = file.listFiles();
        for(File f : fileArr){
            String listFile = f.getPath();
            lock(listFile);
        }*/
    }

    //    public static void lock(String filePath) {//原来的删除了
    public static void lock(int firstLoc, int lastLoc, Set<String> relevantVariabSet, String lockName, String filePath) {
        //得到关联变量
        for (String s : relevantVariabSet)
            variableSet.add(s);

        MatchVariable matchVariable = new MatchVariable();

//        InsertCode.insert(3, "import java.util.concurrent.locks.ReentrantLock;" + '\n', filePath);//原本是可重入锁的定义，现在可以删
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {

            Set<String> names = new HashSet<String>();//存放实际使用的变量，不这样做会有System等变量干扰

            public boolean visit(TypeDeclaration node) {
                className = node.getName().toString();
                return true;
            }

            //定义变量
            public boolean visit(VariableDeclarationFragment node) {
                SimpleName name = node.getName();
                this.names.add(name.getIdentifier());

                return true; // do not continue to avoid usage info
            }

            //变量
            public boolean visit(SimpleName node) {
                if (this.names.contains(node.getIdentifier())) {
//                    System.out.println("Usage of '" + node + "' at line " +	cu.getLineNumber(node.getStartPosition()));
                    boolean flag = false;
                    int nodeStart = cu.getLineNumber(node.getStartPosition());
                    int nodeEnd = cu.getLineNumber(node.getStartPosition() + node.getLength());
                    if(!(nodeStart >= firstLoc && nodeStart <= lastLoc)){
                        for (String s : variableSet) {
                            if (s.equals(node.getIdentifier()))
                                flag = true;
                        }
                    }
                    if (flag) {
                        if (matchVariable.matchSetIsEmpty()) {
                            matchVariable.addMatchSet(node);
                            matchVariable.setSameFatherNode(node.getParent());
                        } else {
                            //如果有数据则需要往里面添加数据，对于相同的变量，后面的一个变量应该覆盖前面的，在此处使用改变父节点的方法
                            if (matchVariable.getMatchSet().contains(node)) {
                                matchVariable.setSameFatherNode(node.getParent());
                            } else {
                                matchVariable.addMatchSet(node);
//                                System.out.println("test" + node.getParent());
                                matchVariable.searchSame(node.getParent());
                            }
                        }
                        if (matchVariable.equalTarget(variableSet)) {
//                           System.out.println("匹配成功");
//                           System.out.println("开始" + cu.getLineNumber(matchVariable.getStartLine()));//下一行
//                           System.out.println("结束" + cu.getLineNumber(matchVariable.getEndLine() + 1));

                            //不能整个类加锁，变量不是在构造函数里
                            if (!(isMemberVariable(matchVariable)) && !(isConstruct(matchVariable.getSameFatherNode().getParent()))) {
                                //加锁
                                /*InsertCode.insert(cu.getLineNumber(matchVariable.getStartLine()), "ReentrantLock lock" + matchVariable.getLockNum() + " = new ReentrantLock(true);lock" + matchVariable.getLockNum() + ".lock();"
                                        + " synchronized (lock" + matchVariable.getLockNum() + "){ ", filePath);*/
                                InsertCode.insert(cu.getLineNumber(matchVariable.getStartLine()), "synchronized (" + lockName + "){ ", filePath);
                                InsertCode.insert(cu.getLineNumber(matchVariable.getEndLine() + 1), " }", filePath);
                                //更新锁
                                matchVariable.update();
                            }
                            //清空
                            matchVariable.clear();
                        }
                    }
                    flag = false;
                }
                return true;
            }
        });
    }

    //检测是不是成员变量
    private static boolean isMemberVariable(MatchVariable matchVariable) {

        //是类的类型
        if (matchVariable.getSameFatherNode().getParent() instanceof TypeDeclaration)
            return true;

        return false;
    }

    //检测是不是构造函数里的变量
    private static boolean isConstruct(ASTNode parent) {

        /**
         * 检查结点的所有父节点，看看有没有一个是构造函数
         * 判断节点类型是函数，节点名字与类名相同，则是构造函数
         */
        while (!(parent instanceof TypeDeclaration)) {
            if ((parent instanceof MethodDeclaration) && (((MethodDeclaration) parent).getName().toString().equals(className))) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }
}
