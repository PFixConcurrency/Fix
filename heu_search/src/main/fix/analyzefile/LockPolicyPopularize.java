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

//add sync to relevent variable
public class LockPolicyPopularize {
    public static Set<String> relevantVariableSet = new HashSet<String>();
    public static int firstLoc = 0;
    public static int lastLoc = 0;
    public static String variableName = "";
    public static String lockName = "";
    public static boolean flagCross = false;//whether merge


    public static void fixRelevantVar(String filePath) {

//        if (!(lockName.equals("this") || lockName.equals("objectFix"))) {
//            return;
//        }

        //acquire them
        acquireRekevantVar(firstLoc, lastLoc, filePath);

//        for (String s : relevantVariableSet) {
//            System.out.println(s + "^");
//        }
//        System.exit(-1);
        if (relevantVariableSet.size() > 1) {
            //add sync to related variables
            //Two variables greater than or equal to the set need to be locked
            AddLockAfterAcquireVariable.lock(firstLoc, lastLoc, relevantVariableSet, lockName, filePath);
        }
    }

    private static Set<String> acquireRekevantVar(int firstLoc, int lastLoc, String threadName, String className) {
        //store relevent
        Set<String> relevantVariableSet = new HashSet<String>();
        //get sequence
        List<ReadWriteNode> nodeSequenceList = RecordSequence.getReadWriteNodeList();
        //Finds all Shared variables in the added lock, based on the start and end locations, plus the thread name
        //It's obviously in the sequence
        for (ReadWriteNode node : nodeSequenceList) {
            //get line number
            int poi = Integer.parseInt(node.getPosition().split(":")[1]);
            //get thread
            String nodeThread = node.getThread();
            //If the number of rows is between here and here, is the same thread
            //Consider threads because it is possible that other threads will operate here
            //Also consider whether it's the same Java file
            String nowCLassName = node.getPosition().split(":")[0].split("/")[1];
            if (poi >= firstLoc && poi <= lastLoc && nodeThread.equals(threadName) && nowCLassName.equals(className)) {

                relevantVariableSet.add(node.getField());
            } else {
                continue;
            }
        }
        return relevantVariableSet;
    }

    //Finally, decide to use the AST to check which variables are relevent
    private static void acquireRekevantVar(int firstLoc, int lastLoc, String filePath) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(getFileContents(new File(filePath)));
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {


            //In the lock, what are the variables are defined
            //On second thought, the associated variable must be compiled
            //It used to be like this, data.value is two variables,
            //Now we're just recording one data
            Set<String> varDefInSync = new HashSet<String>();

            @Override
            public boolean visit(VariableDeclarationFragment node) {
                this.varDefInSync.add(node.getName().getIdentifier());
                return super.visit(node);
            }

            @Override
            public boolean visit(SimpleName node) {

                if (this.varDefInSync.contains(node.getIdentifier())) {
                    if (cu.getLineNumber(node.getStartPosition()) >= firstLoc && cu.getLineNumber(node.getStartPosition() + node.getLength()) <= lastLoc) {
                        if (!node.getIdentifier().equals("objectFix")) {
                            relevantVariableSet.add(node.getIdentifier());
                        }
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
