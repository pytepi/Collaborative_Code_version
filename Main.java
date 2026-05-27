import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
  public static void main(String[] args) {
    if (args.length == 0) {
      String[] tests = findTests();
      for (int i = 0; i < tests.length; i++) {
        String testFileNam = tests[i];
        System.out.println("Running test: " + testFileNam);
        Tableaux tabealux = doTableauxToExprfromFile("tests\\" + testFileNam);
        if (tabealux.rootFormula != null) {
          analyzetableaux(tabealux, false);
        }
        System.out.println("");
      }
    } else {
      Tableaux tableaux = doTableauxToExprfromFile(args[0]);
      if (tableaux.rootFormula != null) {
        analyzetableaux(tableaux, true);
      }
    }
  }

  public static void analyzetableaux(Tableaux t, boolean verbose) {
    System.out.println("Tableaux for formula: " + t.rootFormula);
    if (verbose) {
      System.out.println("\n"+t+"\n");
    }
    System.out.println("Formua is satisfiable: " + t.isSatisfiable());
  }

  public static Tableaux doTableauxToExprfromFile(String in) {
    /* Start the parser */
    try {
      Lexer l = new Lexer(new FileReader(in));
      // SymbolFactory sf = new ComplexSymbolFactory();
      parser p = new parser(l /* , sf */);
      Object result = p.parse().value;
      if (result instanceof Expression) {
        @SuppressWarnings("unchecked")
        Expression res = (Expression) result;
        Tableaux t = new Tableaux();
        t.doTableaux(res);
        return t;
      } else return new Tableaux();
    } catch (Exception e) {
      /* do cleanup here -- possibly rethrow e */
      e.printStackTrace();
      return new Tableaux();
    }
  }

  public static String[] findTests() {
    // Find all text.txt files in test dir
    try {
      File dir = new File("tests");

      FileFilter dir_file_filter = new FileFilter() {
        @Override
        // we do not want to test the template
        public boolean accept(File pathname) {
          return !pathname.getName().contains("template");
        }
      };

      File[] test_list_as_file = dir.listFiles(dir_file_filter);
      String[] test_file_names = new String[test_list_as_file.length];
      for (int i = 0; i < test_list_as_file.length; i++) {
        test_file_names[i] = test_list_as_file[i].getName();
      }
      return test_file_names;
    } catch (Exception e) {
      System.err.println(e);
      return new String[0];
    }
  }
}
