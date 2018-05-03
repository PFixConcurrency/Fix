package fix.analyzefile;

import fix.run.AddLockAfterAcquireVariable;
import org.eclipse.jdt.core.dom.*;
import p_heu.entity.ReadWriteNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//加锁策略的推广
//就是关联变量加同步
public class LockPolicyPopularize {
    static Set<String> relevantVariableSet = new HashSet<String>();

    public static void fixRelevantVar(int firstLoc, int lastLoc, String threadName, String className, String lockName, String filePath) {
        //获取到关联变量
//        Set<String> relevantVariabSet = acquireRekevantVar(firstLoc, lastLoc, threadName, className);
        acquireRekevantVar(firstLoc, lastLoc, filePath);

        /*System.out.println("关联变量");
        for (String s : relevantVariableSet) {
            System.out.println(s);
        }
        System.exit(-1);*/
        if (relevantVariableSet.size() > 1) {
            //对相关变量加锁
            //set中大于等于两个变量才有加锁的意义
            //不然加锁没意义
            AddLockAfterAcquireVariable.lock(firstLoc, lastLoc, relevantVariableSet, lockName, filePath);
        }
    }

    private static Set<String> acquireRekevantVar(int firstLoc, int lastLoc, String threadName, String className) {
        //存放关联变量
        Set<String> relevantVariableSet = new HashSet<String>();
        //拿到sequence序列
        List<ReadWriteNode> nodeSequenceList = RecordSequence.getReadWriteNodeList();
        //根据起始和终止位置，加上线程名，找出加的锁中的所有共享变量
        //很显然是在原来的sequence序列中找
        for (ReadWriteNode node : nodeSequenceList) {
            //首先得到行数
            int poi = Integer.parseInt(node.getPosition().split(":")[1]);
            //然后得到线程
            String nodeThread = node.getThread();
            //判断行数在不在这之间，是不是同一个线程
            //考虑线程是因为有可能会有其他线程在这里操作
            //还有考虑是不是同一个java文件，之前就因为没考虑这个而出错
            String nowCLassName = node.getPosition().split(":")[0].split("/")[1];
            if (poi >= firstLoc && poi <= lastLoc && nodeThread.equals(threadName) && nowCLassName.equals(className)) {
//                System.out.println(node + "只是测试");
                relevantVariableSet.add(node.getField());
            } else {
                continue;
            }
        }
        return relevantVariableSet;
    }

    //最后决定使用AST检查有哪些变量是关联变量，不用jpf
    private static void acquireRekevantVar(int firstLoc, int lastLoc, String filePath) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {


            //在锁中，有哪些变量被定义了
            //后来仔细想想，关联变量肯定是编译后的
            //以前会出现这样的情况，data.value算两个变量，
            //现在我们只记录一个data
            Set<String> varDefInSync = new HashSet<String>();

            @Override
            public boolean visit(VariableDeclarationFragment node) {
                this.varDefInSync.add(node.getName().getIdentifier());
                return super.visit(node);
            }

            @Override
            public boolean visit(SimpleName node) {

                if (this.varDefInSync.contains(node.getIdentifier())) {
                    if(cu.getLineNumber(node.getStartPosition()) >= firstLoc && cu.getLineNumber(node.getStartPosition() + node.getLength()) <= lastLoc){
                        relevantVariableSet.add(node.getIdentifier());
                    }
                }
                return super.visit(node);
            }

        });
    }

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
