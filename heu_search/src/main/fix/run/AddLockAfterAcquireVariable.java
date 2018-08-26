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
    static String className = "";//The name of the class for later comparison purposes
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

        /*
        AcquireVariableInSameLock acquireVariableInSameLock = new AcquireVariableInSameLock();
        Vector<String> v = acquireVariableInSameLock.getOneLockfieldVector();
        for (String s : v)
            variableSet.add(s);

        //
        ExamplesIO examplesIO = ExamplesIO.getInstance();

        dirPath = examplesIO.copyFromOneDirToAnotherAndChangeFilePath("examples","exportExamples", dirPath);

        File file = new File(dirPath);
        File[] fileArr = file.listFiles();
        for(File f : fileArr){
            String listFile = f.getPath();
            lock(listFile);
        }*/
    }

    public static void lock(int firstLoc, int lastLoc, Set<String> relevantVariabSet, String lockName, String filePath) {
        //get relevantVariabSet
        for (String s : relevantVariabSet)
            variableSet.add(s);

        MatchVariable matchVariable = new MatchVariable();

//        InsertCode.insert(3, "import java.util.concurrent.locks.ReentrantLock;" + '\n', filePath);
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {

            Set<String> names = new HashSet<String>();//Store actual used variables. If you do not do this, variables like System will interfere

            public boolean visit(TypeDeclaration node) {
                className = node.getName().toString();
                return true;
            }

            //defined variable
            public boolean visit(VariableDeclarationFragment node) {
                SimpleName name = node.getName();
                this.names.add(name.getIdentifier());

                return true; // do not continue to avoid usage info
            }

            //variable
            public boolean visit(SimpleName node) {
                if (this.names.contains(node.getIdentifier())) {
//                    System.out.println("Usage of '" + node + "' at line " +	cu.getLineNumber(node.getStartPosition()));
                    boolean flag = false;

                    int nodeStart = cu.getLineNumber(node.getStartPosition());

                    //Lock them out of their original place
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
                            //If we have data, we need to add data to it. For the same variable, the last variable should overwrite the previous one
                            if (matchVariable.getMatchSet().contains(node)) {
                                matchVariable.setSameFatherNode(node.getParent());
                            } else {
                                matchVariable.addMatchSet(node);
//                                System.out.println("test" + node.getParent());
                                matchVariable.searchSame(node.getParent());
                            }
                        }

                        //The matched variables are contained in the associated variables
                        if (matchVariable.equalTarget(variableSet)) {
//                        if (matchVariable.containsIn(variableSet)) {

                            //Cannot lock the entire class, the variable is not in the constructor
                            if (!(isMemberVariable(matchVariable)) && !(isConstruct(matchVariable.getSameFatherNode().getParent()))) {
                                boolean already = false;
                                //No data is locked directly
                                //There is data to determine
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

                                    //Determine if the lock will cross the for loop, etc
                                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(start, end, filePath);
                                    start = lockLine.getFirstLoc();
                                    end = lockLine.getLastLoc();

                                    //Will the variable be defined inside the lock and used outside
                                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(start, end, filePath);
                                    start = lockLine.getFirstLoc();
                                    end = lockLine.getLastLoc();

                                    end++;


                                    if (!(start >= firstLoc && start <= lastLoc) || (end >= firstLoc && end <= lastLoc)) {
                                        if (numSet.contains(start) && numSet.contains(end)) {
                                            //The newly added lock is contained in the added lock, directly unadded
                                        } else if (numSet.contains(start) && !numSet.contains(end)) {
                                            //The new lock intersects the added lock
                                            merge1(start, end, lockName, filePath);
                                        } else if (!numSet.contains(start) && numSet.contains(end)) {
                                            merge2(start, end, lockName, filePath);
                                        } else {
                                            //add sync
                                            InsertCode.insert(start, "synchronized (" + lockName + "){ ", filePath);
                                            InsertCode.insert(end, " }", filePath);

                                            //put data into list
                                            RelevantVarLockLine r = new RelevantVarLockLine(start, end);
                                            relevantVarLockLineList.add(r);
                                        }
                                        for (int indexNum = start; indexNum <= end; indexNum++) {
                                            numSet.add(indexNum);
                                        }
                                    }
                                }


                            }

                            //clear out
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
            //Find the overlap
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

        //only one
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
            //find overlap
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

        //oldStart is the last element
        if (oldEnd == 0)
            oldEnd = now;

        start = Math.min(start, oldStart);
        end = Math.max(end, oldEnd);

        MergePro.merge(start, end, oldStart, oldEnd, lockName, filePath);
    }

    //isMemberVariable
    private static boolean isMemberVariable(MatchVariable matchVariable) {


        //class type
        if (matchVariable.getSameFatherNode().getParent() instanceof TypeDeclaration ||
                matchVariable.getSameFatherNode().getParent().getParent() instanceof TypeDeclaration) {
            return true;
        }
        return false;
    }

    //is in Construct
    private static boolean isConstruct(ASTNode parent) {

        /**
         * Check all the parent nodes of the node to see if there is a constructor
         * Determine that the node type is a function, and that the node name is the same as the class name, which is the constructor
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
