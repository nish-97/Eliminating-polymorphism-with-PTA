package visitor;

import syntaxtree.*;

import java.io.FileWriter;
import java.util.*;
import visitor.MyVisitor;
// A basic block contains a list of instructions
// and a list of incoming/outgoing edges to BB's

public class BB {
    public ArrayList<Instruction> instructions;
    public Set<BB> incomingEdges, outgoingEdges;
    static int id = 0;
    public String name;

    static int line_num = 0;
    // public static Map<String, HashMap<String, ArrayList<String>>> obj_stack = new HashMap();
    // public static Map<String, HashMap<HashMap<String, String>, String>> obj_heap = new HashMap();
    // public Map<String, String> tempHashMap;
    public static Map<String, BBData> bbdata;
    public static Map<String, Map<String, Set<String>>> global_obj_stack_perbb = new HashMap();
    public static Map<String, Set<String>> global_obj_stack = new HashMap();
    public static Map<HashMap<String, String>, Set<String>> global_obj_heap = new HashMap();
    // public static Map<String, BBData> global_bbdata = new HashMap<>();

    public BB() {
        instructions = new ArrayList<>();
        incomingEdges = new HashSet<>();
        outgoingEdges = new HashSet<>();
        name = "bb" + (id++);
        // Add a dummy instruction to start of every block
        instructions.add(new Instruction(null));
    }

    public void pushInstruction(Instruction i) {
        instructions.add(i);
    }

    public void addIncomingEdge(BB incomingEdge) {
        incomingEdges.add(incomingEdge);
    }

    public void addOutgoingEdge(BB outgoingEdge) {
        outgoingEdges.add(outgoingEdge);
    }

    Set<String> getInSet() {
        return instructions.get(0).getInSet();
    }

    Set<String> getOutSet() {
        return instructions.get(instructions.size() - 1).getOutSet();
    }

    public static void printBB(BB startNode) {
        Set<BB> visitedBBs = new HashSet<>();
        Stack<BB> worklist = new Stack<>();

        worklist.push(startNode);

        while (worklist.size() > 0) {
            BB currentBB = worklist.pop();
            visitedBBs.add(currentBB);

            System.out.print(currentBB.name + " : [PRED] [");

            for (BB bbIncoming : currentBB.incomingEdges) {
                System.out.print(bbIncoming.name + " ");
            }

            System.out.println("]");
            

            StringBuilder sb = new StringBuilder();

            for (Instruction i : currentBB.instructions) {
                if (i.instructionNode != null) {
                    Node instruction = i.instructionNode;
                    if (instruction instanceof VarDeclaration) {
                        VarDeclaration curr = (VarDeclaration) instruction;
                        sb.append(getTypeString(curr.f0) + " " + curr.f1.f0.tokenImage + ";\n");
                    } else if (instruction instanceof AssignmentStatement) {
                        AssignmentStatement curr = (AssignmentStatement) instruction;
                        String lSide = curr.f0.f0.tokenImage;
                        String rSide = getExpressionString(curr.f2);
                        sb.append(lSide + " = " + rSide + ";\n");
                    } else if (instruction instanceof MethodDeclaration) {
                        MethodDeclaration node = (MethodDeclaration) i.instructionNode;
                        sb.append("return " + PEString(node.f10) + ";\n");
                    } else if (instruction instanceof PrintStatement) {
                        PrintStatement node = (PrintStatement) i.instructionNode;
                        sb.append("System.out.println(" + PEString(node.f2) + ");\n");
                    } else if(i.instructionNode instanceof ArrayAssignmentStatement) {
                        ArrayAssignmentStatement node = (ArrayAssignmentStatement) i.instructionNode;
                        String lSide = node.f0.f0.tokenImage + "_" + PEString(node.f2);
                        String offset = PEString(node.f2);
                        String rSide = PEString(node.f5);
                        sb.append(lSide + "[" + offset + "] = " + rSide + ";\n");
                    } else if(i.instructionNode instanceof FieldAssignmentStatement) {
                        FieldAssignmentStatement node = (FieldAssignmentStatement) i.instructionNode;
                        String lSide = node.f0.f0.tokenImage;
                        String field = node.f2.f0.tokenImage;
                        String rSide = PEString(node.f4);
                        sb.append(lSide + "." + field + " = " + rSide + ";\n");
                    } else if (i.instructionNode instanceof IfthenStatement) {
                        sb.append(((IfthenStatement)i.instructionNode).f2.f0.tokenImage + "\n");
                    } else if (i.instructionNode instanceof IfthenElseStatement) {
                        sb.append(((IfthenElseStatement)i.instructionNode).f2.f0.tokenImage + "\n");
                    } else if (i.instructionNode instanceof WhileStatement) {
                        sb.append(((WhileStatement)i.instructionNode).f2.f0.tokenImage + "\n");
                    } else if (i.instructionNode instanceof StoreStatement) {
                        sb.append(((StoreStatement)i.instructionNode).f0.f0.tokenImage + "\n");
                    }
                    else if (i.instructionNode instanceof ThisStoreStatement) {
                        sb.append("this" + ((ThisStoreStatement)i.instructionNode).f0.f0.tokenImage + "\n");
                    }
                    else {
                        throw new Error("Unhandled instruction in Basic Block: " + i.instructionNode);
                    }

                }
            }
            System.out.print(sb.toString());
            for (BB bbs : currentBB.outgoingEdges) {
                if (!visitedBBs.contains(bbs)) {
                    if (!worklist.contains(bbs)) {
                        worklist.push(bbs);
                    }
                }
            }
            System.out.print("[SUCC] [");
            for (BB bbIncoming : currentBB.outgoingEdges) {
                System.out.print(bbIncoming.name + " ");
            }
            System.out.println("]\n");

        }
    }
    public static void getVals(BB startNode) {
        Set<BB> visitedBBs = new HashSet<>();
        Queue<BB> worklist = new LinkedList<>();
        worklist.offer(startNode);

        // HashMap<String, BBData> globalbb = new HashMap<>();
        BBData prev = null;
        bbdata = new HashMap();
        while (worklist.size() > 0) {
            BB currentBB = worklist.remove();
            visitedBBs.add(currentBB);

            // System.out.print(currentBB.name + " : [PRED] [");

            for (BB bbIncoming : currentBB.incomingEdges) {
                // System.out.print(bbIncoming.name + " ");
                
                if(currentBB.incomingEdges.size() >= 2){
                    HashMap<String, Set<String>> tempmap = new HashMap<>();
                    for(BB s : currentBB.incomingEdges){
                    if(bbdata.get(s.name) != null){
                            // System.out.println("hiii");
                            for(Map.Entry<String, Set<String>> ma : (bbdata.get(s.name).obj_stack).entrySet()){
                                String a = ma.getKey();
                                Set<String> b = ma.getValue();
                                for(BB so : currentBB.incomingEdges){
                                    if(!bbdata.get(so.name).obj_stack.containsKey(a))
                                        tempmap.put(a, b);
                                    else{
                                        b.addAll(bbdata.get(so.name).obj_stack.get(a));
                                        tempmap.put(a, b);
                                    }
                                }   
                            }

                        }
                    }
                    global_obj_stack.putAll(tempmap);
                }
            }

            // System.out.println("]");
            

            StringBuilder sb = new StringBuilder();
            BBData bb = new BBData(currentBB.name);
            // globalbb.put(currentBB.name, bb);
            for (Instruction i : currentBB.instructions) {
                if (i.instructionNode != null) {
                    Node instruction = i.instructionNode;
                    
                    if(i.instructionNode instanceof AssignmentStatement){
                        AssignmentStatement curr = (AssignmentStatement) instruction;
                        String id = curr.f0.f0.tokenImage; //v
                        if(curr.f2.f0.which == 13){
                            PrimaryExpression pr = (PrimaryExpression)curr.f2.f0.choice;
                            if(pr.f0.which == 6){ //v = new A()
                                String obj_type = ((AllocationExpression) pr.f0.choice).f1.f0.tokenImage; //A
                                bb.add_new_stack_entry(id, "O"+line_num++);
                            }
                            else if(pr.f0.which == 3){ //v = w
                                String rparam = ((Identifier)pr.f0.choice).f0.tokenImage; //w
                                bb.update_stack_entry(id, rparam);
                            }
                        }
                        else if(curr.f2.f0.which == 12){ // v = w.f
                            LoadStatement ld = (LoadStatement) curr.f2.f0.choice;
                            PrimaryExpression pr = (PrimaryExpression) ld.f0;
                            PrimaryExpression pr1 = (PrimaryExpression) ld.f2;
                            if(pr.f0.which == 3 && pr1.f0.which == 3){
                                String a = ((Identifier)pr.f0.choice).f0.tokenImage; //w
                                String b = ((Identifier)pr1.f0.choice).f0.tokenImage; //f
                                bb.update_stack_entry_load(id, a, b);
                            }
                        }
                        
                    }
                    else if(i.instructionNode instanceof FieldAssignmentStatement) { 
                        FieldAssignmentStatement node = (FieldAssignmentStatement) i.instructionNode;
                        String lSide = node.f0.f0.tokenImage; //a
                        String field = node.f2.f0.tokenImage; //b
                        if(node.f4.f0.which == 3){  //a.b = c
                            // String rSide = PEString(node.f4);
                            String rSide = ((Identifier)node.f4.f0.choice).f0.tokenImage; //c
                            bb.add_new_heap_entry(lSide, field, rSide);

                        }
                        if(node.f4.f0.which == 6){ //a.b = new C()
                            String rSide = ((AllocationExpression)node.f4.f0.choice).f1.f0.tokenImage; //C
                            bb.add_new_heap_entry_alloca(lSide, field, rSide, "O"+line_num++);
                        }
                    } 
                    else {
                        // throw new Error("Unhandled instruction in Basic Block: " + i.instructionNode);
                    }
                    
                }
            }
            bbdata.put(currentBB.name, bb);
            if(bb.obj_heap != null)
            global_obj_heap.putAll(bb.obj_heap);
            // if(bb.obj_stack != null)
            // global_obj_stack.putAll(bb.obj_stack);
            if(global_obj_stack != null && global_obj_stack.size() != 0){
                HashMap<String, Set<String>> tempmap = new HashMap<>();
                for(Map.Entry<String, Set<String>> ma : (bb.obj_stack).entrySet()){
                    String a = ma.getKey();
                    Set<String> b = ma.getValue();
                    // System.out.println(b);
                    if(!global_obj_stack.containsKey(a))
                    tempmap.put(a, b);
                    else{
                        b.addAll(global_obj_stack.get(a));
                        tempmap.put(a, b);
                    }
                }
                global_obj_stack.putAll(tempmap);
                global_obj_stack_perbb.put(currentBB.name, global_obj_stack);
            }
            else{
                if(bb.obj_stack != null)
                global_obj_stack.putAll(bb.obj_stack);
                global_obj_stack_perbb.put(currentBB.name, global_obj_stack);

            }
            // System.out.println("Size of bbdata:" + bbdata.size());
            // if(bbdata.size() > 1){
                //     global_bbdata.putAll(bbdata);
                // }
                // for(int i = 0; i < bbdata.size(); i++){
                    // }
            // System.out.println("Stack entry: " + bb.obj_stack);
            // System.out.println("Heap entry: " + bb.obj_heap);
            // System.out.println("Global Stack per bb: " + global_obj_stack_perbb);
            // System.out.println("Global Stack entry: " + global_obj_stack);
            // System.out.println("Global Heap entry: " + global_obj_heap);
            prev = bb;
            // System.out.println("Global stack");
            // System.out.print(sb.toString());
            for (BB bbs : currentBB.outgoingEdges) {
                if (!visitedBBs.contains(bbs)) {
                    if (!worklist.contains(bbs)) {
                        worklist.offer(bbs);
                    }
                }
            }
            // System.out.print("[SUCC] [");
            for (BB bbIncoming : currentBB.outgoingEdges) {
                // System.out.print(bbIncoming.name + " ");
            }
            // System.out.println("]\n");

        }
    }
    public static String PEString(PrimaryExpression node)
    {

                // PrimaryExpression curr = (PrimaryExpression) node.f0.choice;
                switch (node.f0.which) {
                    case 0:
                        return ((IntegerLiteral) node.f0.choice).f0.tokenImage;
                    case 1:
                        return ((TrueLiteral) node.f0.choice).f0.tokenImage;
                    case 2:
                        return ((FalseLiteral) node.f0.choice).f0.tokenImage;
                    case 3:
                        return ((Identifier) node.f0.choice).f0.tokenImage;
                    case 4:
                        return ((ThisExpression) node.f0.choice).f0.tokenImage;
                    case 5:
                        // return "new int [" + ((ArrayAllocationExpression) curr.f0.choice).f3.f0.tokenImage + "]";
                        return "new int [" + PEString(((ArrayAllocationExpression)node.f0.choice).f3) + "]";
                    case 6:
                    {
                        // System.out.println("reached here");
                        return "new " + ((AllocationExpression) node.f0.choice).f1.f0.tokenImage + "()";
                    }
                        case 7: {
                        return "new " + ((NotExpression) node.f0.choice).f1.f0.tokenImage + "()";
                    }
                    default:
                        return "";
                }   
          
    }
    
    public static String getExpressionString(Expression node) {
        switch (node.f0.which) {
            case 0: {
                OrExpression curr = (OrExpression) node.f0.choice;
                String lBinop = PEString(curr.f0);
                String  rBinop = PEString(curr.f2);
                return  lBinop + " || " + rBinop;
            }
            case 1: {
                AndExpression curr = (AndExpression) node.f0.choice;
                // String lBinop = curr.f0.f0.tokenImage;
                // String rBinop = curr.f2.f0.tokenImage;
                String lBinop = PEString(curr.f0);
                String  rBinop = PEString(curr.f2);
                return  lBinop + " && " + rBinop;
            }
            case 2: {
                CompareExpression curr = (CompareExpression) node.f0.choice;
                // String lBinop = curr.f0.f0.tokenImage;
                String lBinop = PEString(curr.f0);
                String  rBinop = PEString(curr.f2);
                // String rBinop = curr.f2.f0.tokenImage;
                return  lBinop + " <= " + rBinop;
            }
            case 3: {
                neqExpression curr = (neqExpression) node.f0.choice;
                // String lBinop = curr.f0.f0.tokenImage;
                // String rBinop = curr.f2.f0.tokenImage;
                String lBinop = PEString(curr.f0);
                String  rBinop = PEString(curr.f2);
                return  lBinop + " != " + rBinop;
            }
            case 4: {
                PlusExpression curr = (PlusExpression) node.f0.choice;
                // String lBinop = curr.f0.f0.tokenImage;
                // String rBinop = curr.f2.f0.tokenImage;
                String lBinop = PEString(curr.f0);
                String  rBinop = PEString(curr.f2);
                return  lBinop + " + " + rBinop;
            }
            case 5: {
                MinusExpression curr = (MinusExpression) node.f0.choice;
                // String lBinop = curr.f0.f0.tokenImage;
                // String rBinop = curr.f2.f0.tokenImage;
                String lBinop = PEString(curr.f0);
                String  rBinop = PEString(curr.f2);
                return  lBinop + " - " + rBinop;
            }
            case 6: {
                TimesExpression curr = (TimesExpression) node.f0.choice;
                // String lBinop = curr.f0.f0.tokenImage;
                // String rBinop = curr.f2.f0.tokenImage;
                String lBinop = PEString(curr.f0);
                String  rBinop = PEString(curr.f2);
                return  lBinop + " * " + rBinop;
            }
            case 7: {
                DivExpression curr = (DivExpression) node.f0.choice;
                // String lBinop = curr.f0.f0.tokenImage;
                // String rBinop = curr.f2.f0.tokenImage;
                String lBinop = PEString(curr.f0);
                String  rBinop = PEString(curr.f2);
                return  lBinop + " / " + rBinop;
            }
            case 8: {
                ArrayLookup curr = (ArrayLookup) node.f0.choice;
                String lBinop = curr.f0.f0.tokenImage;
                // String rBinop = curr.f2.f0.tokenImage;
                String  rBinop = PEString(curr.f2);
                return  lBinop + "[" + rBinop + "]";
            }
            case 9: {
                ArrayLength curr = (ArrayLength) node.f0.choice;
                String lBinop = curr.f0.f0.tokenImage;
                return  lBinop + ".length";
            }
            case 10:
            {
                // TypeCast curr = (TypeCast) node.f0.choice;
                return "Typecast";
            }
            case 11: {
                MessageSend curr = (MessageSend) node.f0.choice;
                String obj = curr.f0.f0.tokenImage;
                String method = curr.f2.f0.tokenImage;
                StringBuilder sb = new StringBuilder();

                sb.append(obj + "." + method+"( ");

                if (curr.f4.present()) {
                    ArgList n = (ArgList) curr.f4.node;
                    sb.append(PEString(n.f0) + " ");
                    for (Enumeration<Node> e = n.f1.elements(); e.hasMoreElements(); ) {
                        ArgRest r = (ArgRest) e.nextElement();
                        sb.append(PEString(r.f1) + " ");
                    }
                }
                sb.append(")");
                return sb.toString();
            }
            case 12:{
                LoadStatement curr = (LoadStatement) node.f0.choice;
                String lBinop = PEString(curr.f0);
                String  rBinop = PEString(curr.f2);
                return lBinop + "." + rBinop;
            }
            case 13: {
                PrimaryExpression curr = (PrimaryExpression) node.f0.choice;
                return PEString(curr);

            }
        }
        return  "";
    }

    public static String check_mono(String l, String r, BB startNode){

        Set<BB> visitedBBs = new HashSet<>();
        Stack<BB> worklist = new Stack<>();

        worklist.push(startNode);

        while (worklist.size() > 0) {
            BB currentBB = worklist.pop();
            visitedBBs.add(currentBB);

            // System.out.print(currentBB.name + " : [PRED] [");

            // for (BB bbIncoming : currentBB.incomingEdges) {
            //     System.out.print(bbIncoming.name + " ");
            // }

            // System.out.println("]");
            

            StringBuilder sb = new StringBuilder();

            for (Instruction i : currentBB.instructions) {
                if (i.instructionNode != null) {
                    Node instruction = i.instructionNode;
                    // if (instruction instanceof VarDeclaration) {
                    //     VarDeclaration curr = (VarDeclaration) instruction;
                    //     // sb.append(getTypeString(curr.f0) + " " + curr.f1.f0.tokenImage + ";\n");
                    // } 
                    if (instruction instanceof AssignmentStatement) { //a = b.c(x, y)
                        AssignmentStatement curr = (AssignmentStatement) instruction;
                        String lSide = curr.f0.f0.tokenImage;
                        String rSide = getExpressionString(curr.f2);
                        if(lSide.equalsIgnoreCase(l) && rSide.equalsIgnoreCase(r)){
                            Map<String, Set<String>> mp = global_obj_stack_perbb.get(currentBB.name);
                            MessageSend curr1 = (MessageSend) curr.f2.f0.choice;
                            String obj = curr1.f0.f0.tokenImage;
                            if(mp.get(obj).size() == 1){
                                String it = mp.get(obj).iterator().next();
                                return it;
                            }
                            else 
                                return null;
                        }
                        // sb.append(lSide + " = " + rSide + ";\n");
                    } 
                    // else 
                    //     return false;
                    // else if (instruction instanceof MethodDeclaration) {
                    //     MethodDeclaration node = (MethodDeclaration) i.instructionNode;
                    //     // sb.append("return " + PEString(node.f10) + ";\n");
                    // } else if (instruction instanceof PrintStatement) {
                    //     PrintStatement node = (PrintStatement) i.instructionNode;
                    //     // sb.append("System.out.println(" + PEString(node.f2) + ");\n");
                    // } else if(i.instructionNode instanceof ArrayAssignmentStatement) {
                    //     ArrayAssignmentStatement node = (ArrayAssignmentStatement) i.instructionNode;
                    //     String lSide = node.f0.f0.tokenImage + "_" + PEString(node.f2);
                    //     String offset = PEString(node.f2);
                    //     String rSide = PEString(node.f5);
                    //     // sb.append(lSide + "[" + offset + "] = " + rSide + ";\n");
                    // } else if(i.instructionNode instanceof FieldAssignmentStatement) {
                    //     FieldAssignmentStatement node = (FieldAssignmentStatement) i.instructionNode;
                    //     String lSide = node.f0.f0.tokenImage;
                    //     String field = node.f2.f0.tokenImage;
                    //     String rSide = PEString(node.f4);
                    //     // sb.append(lSide + "." + field + " = " + rSide + ";\n");
                    // } else if (i.instructionNode instanceof IfthenStatement) {
                    //     // sb.append(((IfthenStatement)i.instructionNode).f2.f0.tokenImage + "\n");
                    // } else if (i.instructionNode instanceof IfthenElseStatement) {
                    //     // sb.append(((IfthenElseStatement)i.instructionNode).f2.f0.tokenImage + "\n");
                    // } else if (i.instructionNode instanceof WhileStatement) {
                    //     // sb.append(((WhileStatement)i.instructionNode).f2.f0.tokenImage + "\n");
                    // } else if (i.instructionNode instanceof StoreStatement) {
                    //     // sb.append(((StoreStatement)i.instructionNode).f0.f0.tokenImage + "\n");
                    // }
                    // else if (i.instructionNode instanceof ThisStoreStatement) {
                    //     // sb.append("this" + ((ThisStoreStatement)i.instructionNode).f0.f0.tokenImage + "\n");
                    // }
                    // else {
                    //     throw new Error("Unhandled instruction in Basic Block: " + i.instructionNode);
                    // }

                }
            }
            // System.out.print(sb.toString());
            for (BB bbs : currentBB.outgoingEdges) {
                if (!visitedBBs.contains(bbs)) {
                    if (!worklist.contains(bbs)) {
                        worklist.push(bbs);
                    }
                }
            }
            // System.out.print("[SUCC] [");
            // for (BB bbIncoming : currentBB.outgoingEdges) {
            //     System.out.print(bbIncoming.name + " ");
            // }
            // System.out.println("]\n");

        }
        return null;
    }

    public static String getTypeString(Type t) {
        switch (t.f0.which) {
            case 0:
                return "int []";
            case 1:
                return "boolean";
            case 2:
                return "int";
            case 3:
                return ((Identifier)t.f0.choice).f0.tokenImage;
        }
        return "";
    }

    

    public static void printBBToStreamAnalysisResult(BB startNode, StringBuilder sb, HashMap<Node, String> resultMap) {
        Set<BB> visitedBBs = new HashSet<>();
        Stack<BB> worklist = new Stack<>();

        worklist.push(startNode);

        while (worklist.size() > 0) {
            BB currentBB = worklist.pop();
            visitedBBs.add(currentBB);

            if (currentBB.outgoingEdges.size() > 0) {
                sb.append( "\n\"" + currentBB.name + "\" -> ");
                int i = 0;
                for (BB bbOut : currentBB.outgoingEdges) {
                    sb.append( "\"" + bbOut.name + "\"");
                    if (++i != currentBB.outgoingEdges.size()) {
                        sb.append(",");
                    }
                }
                sb.append(";\n");
            }
            if (currentBB.outgoingEdges.size() == 0) {
                sb.append("\n" + currentBB.name + " [style=\"rounded,filled\", shape=\"box\", fillcolor=\"orange\", fontname=\"monospace\", xlabel=\"End\", label=\"");
            } else if (currentBB.instructions.size() == 1) {
                sb.append("\n" + currentBB.name + " [fillcolor=\"gray\", style=\"filled\", shape=\"doublecircle\", fontname=\"monospace\", label=\"");
            } else if (currentBB.instructions.size() == 2 && currentBB.instructions.get(1).instructionNode instanceof Identifier) {
                sb.append("\n" + currentBB.name + " [fillcolor=\"gray\", style=\"rounded,filled\", shape=\"diamond\", fontname=\"monospace\", label=\"");
            } else {
                sb.append("\n" + currentBB.name + " [fillcolor=\"white\", style=\"filled\", shape=\"box\", fontname=\"monospace\", xlabel=\"" + currentBB.name + "\", label=\"");
            }

            for (Instruction i : currentBB.instructions) {
                if (i.instructionNode != null) {
                    Node instruction = i.instructionNode;
                    sb.append("[" + resultMap.get(instruction) + "]\n");
                }
            }

            sb.append("\"];");

            for (BB bbs : currentBB.outgoingEdges) {
                if (!visitedBBs.contains(bbs)) {
                    if (!worklist.contains(bbs)) {
                        worklist.push(bbs);
                    }
                }
            }

        }
    }

    public static void printBBToStream(BB startNode, StringBuilder sb, boolean dot) {
        Set<BB> visitedBBs = new HashSet<>();
        Stack<BB> worklist = new Stack<>();

        worklist.push(startNode);

        while (worklist.size() > 0) {
            BB currentBB = worklist.pop();
            visitedBBs.add(currentBB);

            if (currentBB.outgoingEdges.size() > 0) {
                sb.append( "\n\"" + currentBB.name + "\" -> ");
                int i = 0;
                for (BB bbOut : currentBB.outgoingEdges) {
                    sb.append( "\"" + bbOut.name + "\"");
                    if (++i != currentBB.outgoingEdges.size()) {
                        sb.append(",");
                    }
                }
                sb.append(";\n");
            }
            if (currentBB.outgoingEdges.size() == 0) {
                sb.append("\n" + currentBB.name + " [style=\"rounded,filled\", shape=\"box\", fillcolor=\"orange\", fontname=\"monospace\", xlabel=\"End\", label=\"");
            } else if (currentBB.instructions.size() == 1) {
                sb.append("\n" + currentBB.name + " [fillcolor=\"gray\", style=\"filled\", shape=\"doublecircle\", fontname=\"monospace\", label=\"");
            } else if (currentBB.instructions.size() == 2 && currentBB.instructions.get(1).instructionNode instanceof Identifier) {
                sb.append("\n" + currentBB.name + " [fillcolor=\"gray\", style=\"rounded,filled\", shape=\"diamond\", fontname=\"monospace\", label=\"");
            } else {
                sb.append("\n" + currentBB.name + " [fillcolor=\"white\", style=\"filled\", shape=\"box\", fontname=\"monospace\", xlabel=\"" + currentBB.name + "\", label=\"");
            }

            for (Instruction i : currentBB.instructions) {
                if (i.instructionNode != null) {
                    Node instruction = i.instructionNode;
                    if (instruction instanceof VarDeclaration) {
                        VarDeclaration curr = (VarDeclaration) instruction;
                        sb.append(getTypeString(curr.f0) + " " + curr.f1.f0.tokenImage + ";\n");
                    } else if (instruction instanceof AssignmentStatement) {
                        AssignmentStatement curr = (AssignmentStatement) instruction;
                        String lSide = curr.f0.f0.tokenImage;
                        String rSide = getExpressionString(curr.f2);
                        sb.append(lSide + " = " + rSide + ";\n");

                    } else if (instruction instanceof MethodDeclaration) {
                        MethodDeclaration node = (MethodDeclaration) i.instructionNode;
                        sb.append("return " + PEString(node.f10)+ ";\n");
                    } else if (instruction instanceof PrintStatement) {
                        PrintStatement node = (PrintStatement) i.instructionNode;
                        sb.append("System.out.println(" + PEString(node.f2) + ");\n");
                    } else if(i.instructionNode instanceof ArrayAssignmentStatement) {
                        ArrayAssignmentStatement node = (ArrayAssignmentStatement) i.instructionNode;
                        String lSide = node.f0.f0.tokenImage + "_" + PEString(node.f2);
                        String offset = PEString(node.f2);
                        String rSide = PEString(node.f5);
                        sb.append(lSide + "[" + offset + "] = " + rSide + ";\n");
                    } else if(i.instructionNode instanceof FieldAssignmentStatement) {
                        FieldAssignmentStatement node = (FieldAssignmentStatement) i.instructionNode;
                        String lSide = node.f0.f0.tokenImage;
                        String field = node.f2.f0.tokenImage;
                        String rSide = PEString(node.f4);
                        sb.append(lSide + "." + field + " = " + rSide + ";\n");
                    } else if (i.instructionNode instanceof IfthenStatement) {
                        sb.append(((IfthenStatement)i.instructionNode).f2.f0.tokenImage + "\n");
                    } else if (i.instructionNode instanceof IfthenElseStatement) {
                        sb.append(((IfthenElseStatement)i.instructionNode).f2.f0.tokenImage + "\n");
                    } else if (i.instructionNode instanceof WhileStatement) {
                        sb.append(((WhileStatement)i.instructionNode).f2.f0.tokenImage + "\n");
                    } 
                    else if (i.instructionNode instanceof StoreStatement) {
                        sb.append(((StoreStatement)i.instructionNode).f0.f0.tokenImage + "\n");
                    }
                    else if (i.instructionNode instanceof ThisStoreStatement) {
                        sb.append("this" + ((ThisStoreStatement)i.instructionNode).f0.f0.tokenImage + "\n");
                    }
                    else {
                        throw new Error("Unhandled instruction in Basic Block: " + i.instructionNode);
                    }

                }
            }

            sb.append("\"];");

            for (BB bbs : currentBB.outgoingEdges) {
                if (!visitedBBs.contains(bbs)) {
                    if (!worklist.contains(bbs)) {
                        worklist.push(bbs);
                    }
                }
            }

        }
    }

    // A basic block with no outgoing edges is the end BB
    public static BB getEndBB(BB start) {
        Set<BB> visitedBBs = new HashSet<>();
        Stack<BB> worklist = new Stack<>();

        worklist.push(start);

        while (worklist.size() > 0) {
            BB currentBB = worklist.pop();
            visitedBBs.add(currentBB);

            if (currentBB.outgoingEdges.size() == 0) return currentBB;

            for (BB bbs : currentBB.outgoingEdges) {
                if (!visitedBBs.contains(bbs)) {
                    if (!worklist.contains(bbs)) {
                        worklist.push(bbs);
                    }
                }
            }

        }
        return null;
    }

    public static void printBBDOT(ProgramCFG programCFG) {
        StringBuilder sbMain = new StringBuilder();
        String mName = programCFG.mainMethod;
        BB mBB = programCFG.methodBBSet.get(mName);
        sbMain.append("digraph {\n");
        sbMain.append("rankdir=TB");
        printBBToStream(mBB, sbMain, true);
        sbMain.append("\n}");
        try {
            FileWriter myWriter = new FileWriter(mName + ".DOT");
            myWriter.write(sbMain.toString());
            myWriter.close();
        } catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        for (String className : programCFG.classMethodList.keySet()) {
            Set<String> methodList = programCFG.classMethodList.get(className);
            StringBuilder sb = new StringBuilder();
            for (String methodName : methodList) {
                BB currentMethodBB = programCFG.methodBBSet.get(methodName);
                sb.append("digraph {\n");
                sb.append("rankdir=TB");
                printBBToStream(currentMethodBB, sb, true);
                sb.append("\n}");
                try {
                    FileWriter myWriter = new FileWriter(methodName + ".DOT");
                    myWriter.write(sb.toString());
                    myWriter.close();
                } catch (Exception e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            }
        }
    }
}

class BBData{
    String BBname;
    public Map<String, Set<String>> obj_stack = new HashMap();
    public Map<HashMap<String, String>, Set<String>> obj_heap = new HashMap();

    public BBData(String val){
        BBname = val;
    }
    public String get_BB_name(){
        return BBname;
    }
    public void add_new_stack_entry(String a, String obj_name){ //a = new A()
        Set<String> arr = new HashSet<>();
        arr.add(obj_name);
        obj_stack.put(a, arr);
    }
    public void update_stack_entry(String lparam, String rparam){
        // v = w; // Copy
        // Stack[v] = Stack[w]
        Set<String> arr;
        arr = obj_stack.get(rparam);
        obj_stack.replace(lparam, arr);
    }
    public void update_stack_entry_load(String a, String b, String c){
        // v = w.f; // Field load
        // Stack[v] = {}
        // forall Ow in Stack[w]:
        // Stack[v] ∪= Heap[Ow,f]
        Set<String> arr = getArrayList(a); //v
        Set<String> arr1 = getArrayList(b); //w
        HashMap<String, String> tempMap;
        if(arr1 != null){
            for(String s : arr1){
                tempMap = new HashMap<>();
                tempMap.put(s, c);
                if(obj_heap.containsKey(tempMap)){
                    // System.out.println("hi");
                    Set<String> temp = arr;
                    if(temp != null)
                        temp.addAll(obj_heap.get(tempMap));
                    else{
                        temp = new HashSet();
                        temp.addAll(obj_heap.get(tempMap));
                    }
                    obj_stack.put(a, temp);
                }
                else{
                    // System.out.println("hio");
                    // Set<String> temp = arr;
                    // arr.addAll(temp);
                    obj_stack.put(a, arr);
                    // temp.add(c)
                    // arr.addAll(tempMap);
                    // obj_heap.put(tempMap, arr1);
                }
            }
        }

    }
    public Set<String> getArrayList(String id){
        Set arr = obj_stack.get(id);
        return arr;
    }
    public void add_new_heap_entry(String a, String b, String c){
        // v.f = w; // Field store
        // forall Ov in Stack[v]:
        // Heap[Ov,f]U = Stack[w]
        String id1 = a;
        String id2 = b;
        String id3 = c;
        Set<String> arr = getArrayList(id1); //v
        Set<String> arr1 = getArrayList(id3); //w
        HashMap<String, String> tempMap;
        for(String s : arr){
            tempMap = new HashMap<>();
            tempMap.put(s, id2);
            if(obj_heap.containsKey(tempMap)){
                // obj_heap.put(tempMap, arr1);
                Set<String> i = obj_heap.get(tempMap);
                i.addAll(arr1);
                obj_heap.put(tempMap, i);
            }
            else{
                obj_heap.put(tempMap, arr1);
            }
        }
    }
    public void add_new_heap_entry_alloca(String a, String b, String c, String val){
        // v.f = new C(); // Field allocation
        // forall Ov in Stack[v]:
        // Heap[Ov,f]U = Ox (x is line num)
        String id1 = a;
        String id2 = b;
        String id3 = c;
        Set<String> arr = getArrayList(id1); //v
        Set<String> arr1 = getArrayList(id3); //c
        HashMap<String, String> tempMap;
        for(String s : arr){
            tempMap = new HashMap<>();
            tempMap.put(s, id2);
            if(obj_heap.containsKey(tempMap)){
                Set<String> i = obj_heap.get(tempMap);
                // obj_heap.put(tempMap, arr1);
                i.add(val);
                obj_heap.put(tempMap, i);
            }
            else{
                Set<String> j = new HashSet();
                j.add(val);
                obj_heap.put(tempMap, j);
            }
        }
    }
}
