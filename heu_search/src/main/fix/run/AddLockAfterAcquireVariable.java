package fix.run;

import fix.analyzefile.LockPolicyPopularize;
import fix.analyzefile.UseASTAnalysisClass;
import fix.entity.ImportPath;
import fix.entity.MatchVariable;
import fix.entity.type.RelevantVarLockLine;
import fix.io.InsertCode;
import fix.io.MergePro;
import org.eclipse.jdt.core.dom.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddLockAfterAcquireVariable {
    static String dirPath = ImportPath.examplesRootPath + "\\exportExamples\\" + ImportPath.projectName;
    static Set<String> variableSet = new HashSet<String>();
    static String className = "";//类的名字，以后用来比较用
    static List<RelevantVarLockLine> relevantVarLockLineList = new ArrayList<RelevantVarLockLine>();

    static Set numSet = new HashSet();

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

                    //不在原来的地方才要加锁
                    if (!(nodeStart >= firstLoc && nodeStart <= lastLoc)) {
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
                                boolean already = false;
                                //没有数据直接加锁
                                //有数据需要判定一下
                                if (relevantVarLockLineList.size() != 0) {
                                    for (RelevantVarLockLine rvl : relevantVarLockLineList) {
                                        if (rvl.startLine == cu.getLineNumber(matchVariable.getStartLine()) &&
                                                rvl.endLine == (cu.getLineNumber(matchVariable.getEndLine() + 1))) {
                                            already = true;
                                            break;
                                        }
                                    }
                                }
                                if (!already) {
//                                    int start = cu.getLineNumber(matchVariable.getStartLine());
//                                    int end = cu.getLineNumber(matchVariable.getEndLine() + 1);
                                    int start = Integer.MAX_VALUE, end = 0;
                                    for (ASTNode nn : matchVariable.getMatchSet()) {

                                        start = Math.min(cu.getLineNumber(nn.getStartPosition()), start);
                                        end = Math.max(cu.getLineNumber(nn.getStartPosition() + nn.getLength()), end);
                                    }

                                    //判断加锁会不会和for循环等交叉
                                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(start, end, filePath);
                                    start = lockLine.getFirstLoc();
                                    end = lockLine.getLastLoc();

                                    //检查会不会定义变量在锁内，使用变量在锁外
                                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(start, end, filePath);
                                    start = lockLine.getFirstLoc();
                                    end = lockLine.getLastLoc();

                                    end++;


                                    if (!(start >= firstLoc && start <= lastLoc) || (end >= firstLoc && end <= lastLoc)) {
                                        if (numSet.contains(start) && numSet.contains(end)) {
                                            //新加的锁包含在已加的锁里，直接不加
                                        } else if (numSet.contains(start) && !numSet.contains(end)) {
                                            //新加的锁与已加的锁交叉
                                            merge1(start, end, lockName, filePath);
                                        } else if (!numSet.contains(start) && numSet.contains(end)) {
                                            merge2(start, end, lockName, filePath);
                                        } else {
                                            //加锁
                                            InsertCode.insert(start, "synchronized (" + lockName + "){ ", filePath);
                                            InsertCode.insert(end, " }", filePath);

                                            //将数据放到list里面
                                            RelevantVarLockLine r = new RelevantVarLockLine(start, end);
                                            relevantVarLockLineList.add(r);
                                        }
                                        for (int indexNum = start; indexNum <= end; indexNum++) {
                                            numSet.add(indexNum);
                                        }
                                    }
                                }


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

    private static void merge1(int start, int end, String lockName, String filePath) {
        int oldStart = 0, oldEnd = 0;
        end--;
        boolean flagSearch = false;
        int now = 0, last = 0;
        List<Integer> list = new ArrayList<Integer>();
        for (Object o : numSet) {
            list.add((int) o);
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            //找到与其重合的
            if (!flagSearch) {
                now = list.get(i);
                if (now >= start && now <= (end + 1)) {
                    flagSearch = true;
                    oldEnd = now;
                }
            } else {
                last = list.get(i);
                if ((now - last) != 1) {
                    oldStart = last;
                } else {
                    now = last;
                }
            }
        }

        //只有一个
        if (oldStart == 0)
            oldStart = now;

        start = Math.min(start, oldStart);
        end = Math.max(end, oldEnd);
        oldEnd--;
        MergePro.merge(start, end, oldStart, oldEnd, lockName, filePath);
    }

    private static void merge2(int start, int end, String lockName, String filePath) {
        int oldStart = 0, oldEnd = 0;
        end--;
        boolean flagSearch = false;
        int now = 0, next = 0;
        for (Object o : numSet) {
            //找到与其重合的
            if (!flagSearch) {
                now = (int) o;
                if (now >= start && now <= (end + 1)) {
                    flagSearch = true;
                    oldStart = now;
                }
            } else {
                next = (int) o;
                if ((next - now) != 1) {
                    oldEnd = now;
                } else {
                    now = next;
                }
            }
        }

        //oldStart是最后一个元素
        if (oldEnd == 0)
            oldEnd = now;

        start = Math.min(start, oldStart);
        end = Math.max(end, oldEnd);

        MergePro.merge(start, end, oldStart, oldEnd, lockName, filePath);
    }

    //检测是不是成员变量
    private static boolean isMemberVariable(MatchVariable matchVariable) {


        //是类的类型
        if (matchVariable.getSameFatherNode().getParent() instanceof TypeDeclaration) {
            return true;
        }
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
