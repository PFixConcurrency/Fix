package p_heu.entity.filter;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

public class Filter {
    private String regex;
    private java.util.regex.Pattern pat;

    public Filter(String regex) {
        this.regex = regex;
        this.pat = java.util.regex.Pattern.compile(regex);
    }

    public String getRegex() {
        return regex;
    }

    public boolean filter(String param) {
        return this.pat.matcher(param).matches();
    }

    public static Filter createFilePathFilter() {
        String systemType = System.getProperty("os.name");
        if (systemType.contains("Mac")) {
            //在main文件夹下和test文件夹下调用，user.dir的属性，因此需要检查并修改path的值使之始终保持一致
            String path = System.getProperty("user.dir");

            if (!path.endsWith("src")) {
                path += "/heu_search/src";
            }

            File exampleDir = new File(path + "/examples");
            StringBuilder regexBuilder = new StringBuilder("^(");
//        String[] files = exampleDir.list();
//        for (String file : files) {
//            System.out.println(file);
//        }
            Set<String> set = searchFiles(exampleDir);

            boolean start = true;
            for (String str : set) {
//            System.out.println(str);
                java.util.regex.Pattern pat = java.util.regex.Pattern.compile("^" + exampleDir.getPath() + "/(.*)$");

//            System.out.println(pat.toString());
                Matcher matcher = pat.matcher(str);
//            System.out.println(matcher);
                if (matcher.find()) {
//                System.out.println("find");
                    if (start) {
                        start = false;
                    }
                    else {
                        regexBuilder.append("|");
                    }
                    if(!matcher.group(1).equals(".DS_Store")){
                        regexBuilder.append(matcher.group(1));
                    }
                }
            }
            regexBuilder.append("):\\d+$");
            String regex = regexBuilder.toString();
            return new Filter(regex);
        }
        else if (systemType.contains("Windows")) {
            //在main文件夹下和test文件夹下调用，user.dir的属性，因此需要检查并修改path的值使之始终保持一致
            String path = System.getProperty("user.dir");

            if (!path.endsWith("src")) {
                path += "\\heu_search\\src";
            }

            File exampleDir = new File(path + "\\examples");
            StringBuilder regexBuilder = new StringBuilder("^(");
//        String[] files = exampleDir.list();
//        for (String file : files) {
//            System.out.println(file);
//        }
            Set<String> set = searchFiles(exampleDir);

            boolean start = true;
            for (String str : set) {
//            System.out.println(str);
                java.util.regex.Pattern pat = java.util.regex.Pattern.compile(
                        "^" + exampleDir.getPath().replaceAll("\\\\", "\\\\\\\\") + "\\\\(.*)$"
                );

//            System.out.println(pat.toString());
                Matcher matcher = pat.matcher(str);
//            System.out.println(matcher);
                if (matcher.find()) {
//                System.out.println("find");
                    if (start) {
                        start = false;
                    }
                    else {
                        regexBuilder.append("|");
                    }
                    regexBuilder.append(matcher.group(1)
                            .replaceAll("\\\\", "/").replaceAll("\\.", "\\\\."));
                }
            }
            regexBuilder.append("):\\d+$");
            String regex = regexBuilder.toString();
            return new Filter(regex);
        }
        else {
            throw new RuntimeException("unknown system type");
        }
    }

    private static Set<String> searchFiles(File dir) {
        File[] children = dir.listFiles();
        Set<String> set = new HashSet<>();
        for (File file : children) {
            if (file.isDirectory()) {
                set.addAll(searchFiles(file));
            }
            else {
                set.add(file.getPath());
            }
        }
        return set;
    }
}
