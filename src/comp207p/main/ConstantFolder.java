package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;

import org.apache.bcel.generic.*;
import java.util.*;

public class ConstantFolder {
    ClassParser parser = null;
    ClassGen gen = null;

    JavaClass original = null;
    JavaClass optimized = null;

    public ConstantFolder(String classFilePath) {
        try {
            this.parser = new ClassParser(classFilePath);
            this.original = this.parser.parse();
            this.gen = new ClassGen(this.original);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void optimizeMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method) {
        HashMap < Integer, Number > stackNumValID = new HashMap < Integer, Number > ();

        System.out.println("---Start Method ----");
        Code code = method.getCode();
        InstructionList instList = new InstructionList(code.getCode());
        InstructionList instList1 = new InstructionList(code.getCode());
        // Initialise a method generator with the original method as the baseline   
        MethodGen methodGen = new MethodGen(method.getAccessFlags(), method.getReturnType(), method.getArgumentTypes(), null, method.getName(), cgen.getClassName(), instList, cpgen);
        System.out.println(method);

        System.out.println(code);
        InstructionHandle handle1[] = instList1.getInstructionHandles();
        // InstructionHandle is a wrapper for actual Instructions
        int counter = 0;
        for (InstructionHandle handle: instList.getInstructionHandles()) {
            InstructionHandle handleLoop[] = instList.getInstructionHandles();
            Instruction currentInstruc = handle.getInstruction();
            //System.out.println(handle.getPosition() + " INSTRUCT: " + currentInstruc.getName());
            if (currentInstruc instanceof IINC){
                System.out.println("IINC");
                int v = getPushValue(cpgen,handle).intValue();
                v = v + 1;
                IINC inc = (IINC) currentInstruc;
                int value = v;
                int index = inc.getIndex();
                System.out.println("value " + value + "Index " + index);
                instList.insert(handle, new BIPUSH((byte) value));
                InstructionHandle pushedValue = handle.getPrev();
                instList.insert(handle, new ILOAD(index));
                instList.insert(handle, new IADD());
                instList.insert(handle, new ISTORE(index));
                try{
                    instList.redirectBranches(handle, pushedValue);
                    instList.delete(handle);
                } catch (Exception e){
                    System.out.println("COULD REDIRECT OR DELETR");
                }
                instList.setPositions();
             }
            else if (currentInstruc instanceof INVOKEVIRTUAL){
                System.out.println("INVOKE" + getPushValue(cpgen,handle));
                InstructionHandle prev = handle.getPrev().getPrev();
                System.out.println(prev);
                Number value = getPushValue(cpgen,handle.getNext());
                System.out.println(value);
                while (!(prev.getInstruction() instanceof GETSTATIC)){
                    deleteInstruc(instList,handle.getPrev());
                    prev = handle.getPrev().getPrev();
                }      
            } 
            else if (currentInstruc instanceof GOTO) {
                System.out.println("GOTO" + handle.getPrev());
                GOTO g = (GOTO) currentInstruc;
                InstructionHandle target = g.getTarget();
                int currentPos = handle.getPosition();
                int targetPos = target.getPosition();
                System.out.println(currentPos + " " + targetPos);
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof ArithmeticInstruction) {
                InstructionHandle prevStack1 = handle.getPrev();
                Instruction prevInstruc1 = prevStack1.getInstruction();
                InstructionHandle prevStack2 = prevStack1.getPrev();
                Instruction prevInstruc2 = prevStack2.getInstruction();

                Number stackVal2 = getPushValue(cpgen, handle);
                Number stackVal1 = getPushValue(cpgen, prevStack1);
                if (currentInstruc instanceof IMUL) {
                    int calculation = (int) stackVal1 * (int) stackVal2;
                    System.out.println("IMUL: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                    cpgen.addInteger(calculation);
                } else if (currentInstruc instanceof IDIV) {
                    int calculation = (int) stackVal1 / (int) stackVal2;

                    System.out.println("IDIV: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                    cpgen.addInteger(calculation);
                } else if (currentInstruc instanceof ISUB) {
                    int calculation = (int) stackVal1 - (int) stackVal2;

                    System.out.println("ISUB: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                    cpgen.addInteger(calculation);
                } else if (currentInstruc instanceof IADD) {
                    int calculation = (int) stackVal1 + (int) stackVal2;
                    System.out.println("IADD: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                    cpgen.addInteger(calculation);
                } else if (currentInstruc instanceof DMUL) {
                    double calculation = stackVal1.doubleValue() * stackVal2.doubleValue();

                    System.out.println("DMUL: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof DDIV) {
                    double calculation = stackVal1.doubleValue() / stackVal2.doubleValue();

                    System.out.println("DDIV: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof DSUB) {
                    double calculation = stackVal1.doubleValue() - stackVal2.doubleValue();
                    System.out.println("DSUB: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof DADD) {
                    double calculation = stackVal1.doubleValue() + stackVal2.doubleValue();
                    System.out.println("DADD: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof FMUL) {
                    float calculation = (float) stackVal1 * (float) stackVal2;

                    System.out.println("FMUL: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof FDIV) {
                    float calculation = (float) stackVal1 / (float) stackVal2;

                    System.out.println("FDIV: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof FSUB) {
                    float calculation = (float) stackVal1 - (float) stackVal2;

                    System.out.println("FSUB: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof FADD) {
                    float calculation = (float) stackVal1 + (float) stackVal2;
                    System.out.println("FADD: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof LMUL) {
                    long calculation = (long) stackVal1 * (long) stackVal2;

                    System.out.println("LMUL: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof LDIV) {
                    long calculation = (long) stackVal1 / (long) stackVal2;

                    System.out.println("LDIV: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof LSUB) {
                    long calculation = (long) stackVal1 - (long) stackVal2;

                    System.out.println("LSUB: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                } else if (currentInstruc instanceof LADD) {
                    long calculation = (long) stackVal1 + (long) stackVal2;

                    System.out.println("LADD: " + calculation);
                    instList.insert(handle, new PUSH(cpgen, calculation));
                }
                deleteInstruc(instList, handle);
                deleteInstruc(instList, prevStack1);
                deleteInstruc(instList, prevStack2);
            } else if (currentInstruc instanceof ISTORE) {
                System.out.println("ISTORE" +handle.getPrev().getPrev());
                Integer indexVal = ((ISTORE) currentInstruc).getIndex();
                Number stackVal = getPushValue(cpgen, handle);
                stackNumValID.put(indexVal, stackVal);
                System.out.println("ISTORE:: Index: " + indexVal + " Val: " + stackVal);
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof DSTORE) {
                Integer indexVal = ((DSTORE) currentInstruc).getIndex();
                Number stackVal = getPushValue(cpgen, handle);

                stackNumValID.put(indexVal, stackVal);
                System.out.println("DSTORE:: Index: " + indexVal + " Val: " + stackVal);
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof LSTORE) {
                Integer indexVal = ((LSTORE) currentInstruc).getIndex();
                Number stackVal = getPushValue(cpgen, handle);
                stackNumValID.put(indexVal, stackVal);
                System.out.println("LSTORE:: Index: " + indexVal + " Val: " + stackVal);
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof FSTORE) {
                Integer indexVal = ((FSTORE) currentInstruc).getIndex();
                Number stackVal = getPushValue(cpgen, handle);
                stackNumValID.put(indexVal, stackVal);
                System.out.println("FSTORE:: Index: " + indexVal + " Val: " + stackVal);
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof ILOAD) {
                System.out.println(currentInstruc);
                Integer indexVal = ((ILOAD) currentInstruc).getIndex();
                Number storedValue = stackNumValID.get(indexVal);
                System.out.println("ILOAD:: Index: " + indexVal + " Val: " + storedValue);
                instList.insert(handle, new PUSH(cpgen, storedValue.intValue()));
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof DLOAD) {
                Integer indexVal = ((DLOAD) currentInstruc).getIndex();
                Number storedValue = stackNumValID.get(indexVal);
                System.out.println("DLOAD:: Index: " + indexVal + " Val: " + storedValue);
                instList.insert(handle, new PUSH(cpgen, storedValue));
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof LLOAD) {
                Integer indexVal = ((LLOAD) currentInstruc).getIndex();
                Number storedValue = stackNumValID.get(indexVal);
                System.out.println("LLOAD:: Index: " + indexVal + " Val: " + storedValue);
                instList.insert(handle, new PUSH(cpgen, storedValue));
                deleteInstruc(instList, handle);

            } else if (currentInstruc instanceof FLOAD) {
                Integer indexVal = ((FLOAD) currentInstruc).getIndex();
                Number storedValue = stackNumValID.get(indexVal);
                System.out.println("FLOAD:: Index: " + indexVal + " Val: " + storedValue);
                instList.insert(handle, new PUSH(cpgen, storedValue));
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof ICONST) {
                Number instrucValue = ((ICONST) currentInstruc).getValue();
                System.out.println("Const Value: " + instrucValue);
                instList.insert(handle, new PUSH(cpgen, instrucValue));
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof DCONST) {
                Number instrucValue = ((DCONST) currentInstruc).getValue();
                System.out.println("Const Value " + instrucValue);
                instList.insert(handle, new PUSH(cpgen, instrucValue));
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof LCONST) {
                Number instrucValue = ((LCONST) currentInstruc).getValue();
                System.out.println("Const Value " + instrucValue);
                instList.insert(handle, new PUSH(cpgen, instrucValue));
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof FCONST) {
                Number instrucValue = ((FCONST) currentInstruc).getValue();
                System.out.println("Const Value " + instrucValue);
                instList.insert(handle, new PUSH(cpgen, instrucValue));
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof ConversionInstruction) {
                Number temp = getPushValue(cpgen, handle);
                Number stackConvertValue = getCastedNumber(currentInstruc, temp);
                System.out.println("Conversion Value " + stackConvertValue);
                deleteInstruc(instList, handle.getPrev());
                instList.append(handle, new PUSH(cpgen, stackConvertValue));
                deleteInstruc(instList, handle);
            } else if (currentInstruc instanceof IF_ICMPLE) {
                System.out.println("COM");
                IF_ICMPLE ife = (IF_ICMPLE) currentInstruc;
                InstructionHandle prevStack1 = handle.getPrev();
                Instruction prevInstruc1 = prevStack1.getInstruction();
                InstructionHandle prevStack2 = prevStack1.getPrev();
                Instruction prevInstruc2 = prevStack2.getInstruction();
                Number stackVal2 = getPushValue(cpgen, handle);
                Number stackVal1 = getPushValue(cpgen, prevStack1);
                int a = stackVal1.intValue();
                int b = stackVal2.intValue();
                System.out.println(stackVal1 + "  " + stackVal2);
                InstructionHandle delete = ife.getTarget();
                if (a > b) {
                    System.out.println("TRUE");
                    instList.insert(delete.getNext(), new PUSH(cpgen, true));

                } else if (a == b) {
                    instList.insert(handle, new PUSH(cpgen, 0));
                } else {
                    System.out.println("FASLE");
                    instList.insert(delete.getNext(), new PUSH(cpgen, false));
                }
                //instList.insert(handle, new PUSH(cpgen, calculation));
                deleteInstruc(instList, handle);
                deleteInstruc(instList, prevStack1);
                deleteInstruc(instList, prevStack2);
            } else if (currentInstruc instanceof IF_ICMPGE) {
                System.out.println("COM");
                IF_ICMPGE ife = (IF_ICMPGE) currentInstruc;
                InstructionHandle prevStack1 = handle.getPrev();
                Instruction prevInstruc1 = prevStack1.getInstruction();
                InstructionHandle prevStack2 = prevStack1.getPrev();
                Instruction prevInstruc2 = prevStack2.getInstruction();
                Number stackVal2 = getPushValue(cpgen, handle);
                Number stackVal1 = getPushValue(cpgen, prevStack1);
                int a = stackVal1.intValue();
                int b = stackVal2.intValue();
                System.out.println(stackVal1 + "  " + stackVal2);
                InstructionHandle delete = ife.getTarget();
                if (a < b) {
                    System.out.println("TRUE");
                    instList.insert(delete.getNext(), new PUSH(cpgen, true));

                } else if (a == b) {
                    instList.insert(handle, new PUSH(cpgen, 0));
                } else {
                    System.out.println("FASLE");
                    instList.insert(delete.getNext(), new PUSH(cpgen, false));
                }
                //instList.insert(handle, new PUSH(cpgen, calculation));
                deleteInstruc(instList, handle);
                deleteInstruc(instList, prevStack1);
                deleteInstruc(instList, prevStack2);
            } else if (currentInstruc instanceof IF_ICMPGT) {
                System.out.println("COM");
                IF_ICMPGT ife = (IF_ICMPGT) currentInstruc;
                InstructionHandle prevStack1 = handle.getPrev();
                Instruction prevInstruc1 = prevStack1.getInstruction();
                InstructionHandle prevStack2 = prevStack1.getPrev();
                Instruction prevInstruc2 = prevStack2.getInstruction();
                Number stackVal2 = getPushValue(cpgen, handle);
                Number stackVal1 = getPushValue(cpgen, prevStack1);
                int a = stackVal1.intValue();
                int b = stackVal2.intValue();
                System.out.println(stackVal1 + "  " + stackVal2);
                InstructionHandle delete = ife.getTarget();
                if (a <= b) {
                    System.out.println("TRUE");
                    
                    instList.insert(delete.getNext(), new PUSH(cpgen, true));
                } else {
                    System.out.println("FASLE");
                    instList.insert(delete.getNext(), new PUSH(cpgen, false));
                }
                //instList.insert(handle, new PUSH(cpgen, calculation));
                deleteInstruc(instList, handle);
                deleteInstruc(instList, prevStack1);
                deleteInstruc(instList, prevStack2);
            } else if (currentInstruc instanceof IF_ICMPLT) {
                System.out.println("COM");
                IF_ICMPLT ife = (IF_ICMPLT) currentInstruc;
                InstructionHandle prevStack1 = handle.getPrev();
                Instruction prevInstruc1 = prevStack1.getInstruction();
                InstructionHandle prevStack2 = prevStack1.getPrev();
                Instruction prevInstruc2 = prevStack2.getInstruction();
                Number stackVal2 = getPushValue(cpgen, handle);
                Number stackVal1 = getPushValue(cpgen, prevStack1);
                int a = stackVal1.intValue();
                int b = stackVal2.intValue();
                System.out.println(stackVal1 + "  " + stackVal2);
                InstructionHandle delete = ife.getTarget();
                if (a >= b) {
                    System.out.println("TRUE");
                    
                    instList.insert(delete.getNext(), new PUSH(cpgen, true));
                } else {
                    System.out.println("FASLE");
                    instList.insert(delete.getNext(), new PUSH(cpgen, false));
                }
                //instList.insert(handle, new PUSH(cpgen, calculation));
                deleteInstruc(instList, handle);
                deleteInstruc(instList, prevStack1);
                deleteInstruc(instList, prevStack2);
            } else if (currentInstruc instanceof IFLE) {
                InstructionHandle prevHandle = handle.getPrev();
                if (prevHandle.getInstruction() instanceof LCMP) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFLE ife = (IFLE) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    long a = stackVal1.longValue();
                    long b = stackVal2.longValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a > b) {
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else if (a == b) {
                        instList.insert(handle, new PUSH(cpgen, 0));
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                } else if (prevHandle.getInstruction() instanceof DCMPL) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFLE ife = (IFLE) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    double a = stackVal1.doubleValue();
                    double b = stackVal2.doubleValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a > b) {
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else if (a == b) {
                        instList.insert(handle, new PUSH(cpgen, 0));
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                } else if (prevHandle.getInstruction() instanceof FCMPL) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFLE ife = (IFLE) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    float a = stackVal1.floatValue();
                    float b = stackVal2.floatValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a > b) {

                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else if (a == b) {
                        instList.insert(handle, new PUSH(cpgen, 0));
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                }
            } else if (currentInstruc instanceof IFGE) {
                InstructionHandle prevHandle = handle.getPrev();
                if (prevHandle.getInstruction() instanceof LCMP) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFGE ife = (IFGE) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    long a = stackVal1.longValue();
                    long b = stackVal2.longValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a < b) {
                     
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else if (a == b) {
                        instList.insert(handle, new PUSH(cpgen, 0));
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                } else if (prevHandle.getInstruction() instanceof DCMPG) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFGE ife = (IFGE) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    double a = stackVal1.doubleValue();
                    double b = stackVal2.doubleValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a < b) {
                        
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else if (a == b) {
                        instList.insert(handle, new PUSH(cpgen, 0));
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                } else if (prevHandle.getInstruction() instanceof FCMPG) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFGE ife = (IFGE) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    float a = stackVal1.floatValue();
                    float b = stackVal2.floatValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a < b) {
                        //InstructionHandle delete = ife.getTarget();
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else if (a == b) {
                        instList.insert(handle, new PUSH(cpgen, 0));
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                } 
            } else if (currentInstruc instanceof IFGT) {
                InstructionHandle prevHandle = handle.getPrev();
                if (prevHandle.getInstruction() instanceof LCMP) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFGT ife = (IFGT) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    long a = stackVal1.longValue();
                    long b = stackVal2.longValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a <= b) {
                        
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                } else if (prevHandle.getInstruction() instanceof DCMPG) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFGT ife = (IFGT) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    double a = stackVal1.doubleValue();
                     double b = stackVal2.doubleValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a <= b) {
                        
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                } else if (prevHandle.getInstruction() instanceof FCMPG) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFGT ife = (IFGT) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    float a = stackVal1.floatValue();
                    float b = stackVal2.floatValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a <= b) {
                        
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                }
            } else if (currentInstruc instanceof IFLT) {
                InstructionHandle prevHandle = handle.getPrev();
                if (prevHandle.getInstruction() instanceof LCMP) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFLT ife = (IFLT) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    long a = stackVal1.longValue();
                    long b = stackVal2.longValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a >= b) {
                        
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                } else if (prevHandle.getInstruction() instanceof DCMPL) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFLT ife = (IFLT) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    double a = stackVal1.doubleValue();
                    double b = stackVal2.doubleValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a >= b) {
                        
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                } else if (prevHandle.getInstruction() instanceof FCMPL) {
                    System.out.println("YES" + handle.getInstruction().getName());
                    IFLT ife = (IFLT) currentInstruc;
                    System.out.println(ife.getTarget());
                    InstructionHandle prevStack1 = prevHandle.getPrev();
                    Instruction prevInstruc1 = prevStack1.getInstruction();
                    InstructionHandle prevStack2 = prevStack1.getPrev();
                    Instruction prevInstruc2 = prevStack2.getInstruction();
                    Number stackVal2 = getPushValue(cpgen, prevHandle);
                    Number stackVal1 = getPushValue(cpgen, prevStack1);

                    float a = stackVal1.floatValue();
                    float b = stackVal2.floatValue();
                    System.out.println(stackVal1 + "  " + stackVal2);
                    InstructionHandle delete = ife.getTarget();
                    if (a >= b) {
                        instList.insert(delete.getNext(), new PUSH(cpgen, true));
                        //deleteInstruc(instList,delete);
                    } else {
                        instList.insert(delete.getNext(), new PUSH(cpgen, false));
                    }
                    //instList.insert(handle, new PUSH(cpgen, calculation));
                    deleteInstruc(instList, prevHandle);
                    deleteInstruc(instList, prevStack1);
                    deleteInstruc(instList, prevStack2);
                    deleteInstruc(instList, handle);

                }

            } else {
                handle.getNext();
            }
            counter++;
        }
        instList.setPositions(true);
        methodGen.setMaxStack();
        methodGen.setMaxLocals();
        Method newMethod = methodGen.getMethod();
        cgen.replaceMethod(method, newMethod);
        Code code1 = newMethod.getCode();
    }

    public void optimize() {
        ClassGen cgen = new ClassGen(original); //Template Class to build Java Class
        ConstantPoolGen cpgen = cgen.getConstantPool();

        // Implement your optimization here
        Method[] methods = cgen.getMethods();
        for (int i = 0; i < methods.length; i++) {
            optimizeMethod(cgen, cpgen, methods[i]);
        }
        //this.optimized = gen.getJavaClass();  //This File had this code, but the reference given "gen" seems wrong
        this.optimized = cgen.getJavaClass();
    }

    public void deleteInstruc(InstructionList instList, InstructionHandle handle) {
        instList.redirectBranches(handle, handle.getPrev());
        try {
            // delete the old one
            if (handle != null) {
                instList.delete(handle);
                //System.out.println("Deleted");
            }
        } catch (TargetLostException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }

    public void deleteBulkInstruc(InstructionList instList, InstructionHandle start, InstructionHandle end) {
        //instList.redirectBranches(handle, handle.getPrev());
        try {
            // delete the old one
            instList.delete(start, end);
        } catch (TargetLostException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }


    public InstructionList getLoopInstructions(InstructionList instList, int stackStartPos, int stackEndPos) {
        InstructionHandle handle[] = instList.getInstructionHandles();
        InstructionList newList = new InstructionList();
        //for (int i = (stackStartPos); i< (stackEndPos+1); i++){
        //System.out.println("start: End ");
        int end = findInstruction(instList, stackEndPos, 1) + 1;
        int start = findInstruction(instList, stackStartPos, 1) - 2;
        //int maxLength = findInstruction(instList, handle.length, 1);
        int counter = stackStartPos;
        //System.out.println("start: " + start + "End " + end + " mAX Length: "+ (handle.length - 1));
        //instList.redirectBranches(handle[], handle[].getPrev()); 
        //System.out.println("Name "+ handle[handle.length - 1].getInstruction().getName() );

        deleteBulkInstruc(instList, handle[0], handle[start]);
        deleteBulkInstruc(instList, handle[end], handle[handle.length - 1]);
        //System.out.println("Length " + instList.getLength());

        newList.insert(instList);
        //System.out.println("New Length " + newList.getLength());
        System.out.println("Test1");
        return newList;
    }

    public int findInstruction(InstructionList instList, int findPos, int returnVariable) {
        InstructionHandle handle[] = instList.getInstructionHandles();
        //InstructionList newList = new InstructionList();
        int targetPosition = 0;
        System.out.println("CHeck");
        //for (int i = 0; i< (findPos+1); i++){
        int i = 0;
        int counter = 0;
        while (counter != (findPos)) {
            //System.out.println("Enters");
            InstructionHandle newHandle = handle[i];
            Instruction currentInstruc = newHandle.getInstruction();
            //System.out.println("Enters1");
            if (currentInstruc instanceof GOTO) {
                targetPosition = ((GOTO) currentInstruc).getTarget().getPosition();
            }
            //System.out.println("CHeck1: " + counter + ", Name "+ currentInstruc.getName() + " Positiim " + newHandle.getPosition());
            counter = newHandle.getPosition();
            //System.out.println("Enters2");
            i++;
        }
        System.out.println("CHeck target" + targetPosition);
        if (returnVariable == 1) {
            return i;
        } else {
            return targetPosition;
        }
    }

    public Number getPushValue(ConstantPoolGen cpgen, InstructionHandle handle) {
        InstructionHandle prevStack = handle.getPrev();
        Instruction prevInstruc = prevStack.getInstruction();
        Number stackValue = null;
        if (handle != null && prevStack != null) {
            if (prevInstruc instanceof BIPUSH) {
                stackValue = ((BIPUSH) prevInstruc).getValue();
            } else if (prevInstruc instanceof SIPUSH) {
                stackValue = ((SIPUSH) prevInstruc).getValue();
            } else if (prevInstruc instanceof LDC2_W) {
                stackValue = (Number)((LDC2_W) prevInstruc).getValue(cpgen);
            } else if (prevInstruc instanceof LDC) {
                stackValue = (Number)((LDC) prevInstruc).getValue(cpgen);
            } else if (prevInstruc instanceof ICONST) {
                stackValue = (Number)((ICONST) prevInstruc).getValue();
            } else if (prevInstruc instanceof LCONST) {
                stackValue = (Number)((LCONST) prevInstruc).getValue();
            } else if (prevInstruc instanceof LCONST) {
                stackValue = (Number)((LCONST) prevInstruc).getValue();
            } else if (prevInstruc instanceof DCONST) {
                stackValue = (Number)((DCONST) prevInstruc).getValue();
            } else if (prevInstruc instanceof ConversionInstruction) {
                Number temp = getPushValue(cpgen, prevStack);
                stackValue = getCastedNumber(prevStack.getInstruction(), temp);
            } else {
                System.out.println("Not Null " + prevInstruc.getName());
            }
        }
        System.out.println(stackValue);
        return stackValue;
    }

    private Number getCastedNumber(Instruction instruc, Number stackVal) {
        if (instruc instanceof I2D) {
            return (double)((int) stackVal);
        } else if (instruc instanceof D2F) {
            return (float)((double) stackVal);
        } else if (instruc instanceof D2I) {
            return (int)((double) stackVal);
        } else if (instruc instanceof D2L) {
            return (long)((double) stackVal);
        } else if (instruc instanceof F2D) {
            return (double)((float) stackVal);
        } else if (instruc instanceof F2I) {
            return (int)((float) stackVal);
        } else if (instruc instanceof F2L) {
            return (long)((float) stackVal);
            //} else if (instruc instanceof I2B) { //Not using byte and short types
            //  return (byte)((int) stackVal);
        } else if (instruc instanceof I2D) {
            return (double)((int) stackVal);
        } else if (instruc instanceof I2F) {
            return (float)((int) stackVal);
        } else if (instruc instanceof I2L) {
            return (long)((int) stackVal);
            //} else if (instruc instanceof I2S) {
            //  return (short)((int) stackVal);
        } else if (instruc instanceof L2D) {
            return (double)((long) stackVal);
        } else if (instruc instanceof L2F) {
            return (float)((long) stackVal);
        } else if (instruc instanceof L2I) {
            return (int)((long) stackVal);
        } else if (instruc instanceof L2F) {
            return (float)((long) stackVal);
        }
        return null;
    }

    public void write(String optimisedFilePath) {
        this.optimize();
        try {
            FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
            this.optimized.dump(out);
        } catch (FileNotFoundException e) {
            // Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void push(InstructionList instList, ConstantPoolGen cpgen, Number num, InstructionHandle handle) {
        if (num instanceof Integer) {
            instList.insert(handle, new LDC(cpgen.addInteger((int) num)));
            instList.setPositions();
        }
        if (num instanceof Float) {
            instList.insert(handle, new LDC(cpgen.addFloat((float) num)));
            instList.setPositions();
        }
        if (num instanceof Double) {
            instList.insert(handle, new LDC2_W(cpgen.addDouble((double) num)));
            instList.setPositions();
        }
        if (num instanceof Long) {
            instList.insert(handle, new LDC2_W(cpgen.addLong((Long) num)));
            instList.setPositions();
        }
    }
}