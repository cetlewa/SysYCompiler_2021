import java.io.*;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) {
        String encoding = "utf-8";
        String filePath = "testfile.txt";
        String code = file2string(filePath, encoding);
        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer.getTokenList());
        Interpreter interpreter = new Interpreter(parser.getCompUnit());
        InternCode2MIPS code2mips = new InternCode2MIPS(interpreter.getCodeList());
        list2file(code2mips.getMipsCode());
        System.out.println(">>>>>>>>>> Compiler End <<<<<<<<<<");
    }

    public static String file2string(String filePath, String encoding) {
        try {
            File file = new File(filePath);
            StringBuffer sb = new StringBuffer();
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                while ((lineTxt = bufferedReader.readLine()) != null) {
                    sb.append(lineTxt).append("\n");
                }
                read.close();
                return sb.toString();
            } else {
                System.out.println("Cannot find the specified file.");
            }
        } catch (Exception e) {
            System.out.println("Error: cannot read the content of the file.");
            e.printStackTrace();
        }
        return null;
    }

    public static void list2file(ArrayList<mipsCode> list) {
        File file = new File("mips.txt");
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            for (int i = 0; i < list.size(); i++) {
                bw.write(list.get(i).mips());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


