import syntaxtree.*;
import visitor.*;
import java.util.*;


public class Main {
   public static void main(String [] args) {
      try {
         // Node root = new a4javaout(System.in).Goal();
         // Object o = root.accept(new GJNoArguDepthFirst());
         // System.out.println("Program Parsed Sucessfully.");

         //Code from A2
         Node root = new a4javaout(System.in).Goal();
         CFGGen cfgGen = new CFGGen();
         root.accept(cfgGen);
         ProgramCFG programCFG = cfgGen.getCFG();
         
         // Printing the DOT file
         BB.printBBDOT(programCFG);
         
         // For iterating over the program
         for (String className : programCFG.classMethodList.keySet()) {
            Set<String> methodList = programCFG.classMethodList.get(className);
            // System.out.println("Class: " + className);
            for (String methodName : methodList) {
               // System.out.println("Method: " + methodName);
               BB currentMethodBB = programCFG.methodBBSet.get(methodName);
               // BB.printBB(currentMethodBB);
               BB.getVals(currentMethodBB);
            }
         }

         // Call your visitor here
         root.accept(new MyVisitor<>(programCFG));
         root.accept(new ResultPrinter<>());
      }
      catch (ParseException e) {
         System.out.println(e.toString());
      }
   }
} 
