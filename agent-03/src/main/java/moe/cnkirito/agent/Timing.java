package moe.cnkirito.agent;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
 
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.Type;
 
public class Timing implements ClassFileTransformer {
 
    private String methodName;
 
    private Timing(String methodName) {
        this.methodName = methodName;
    }
 
    public byte[] transform(ClassLoader loader, String className, Class cBR,
            java.security.ProtectionDomain pD, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        if (!className.equalsIgnoreCase("moe/cnkirito/agent/Dog")) {
            return null;
        }
        try {
            ClassParser cp = new ClassParser(new java.io.ByteArrayInputStream(
                    classfileBuffer), className + ".java"); 
            JavaClass jclas = cp.parse();
            ClassGen cgen = new ClassGen(jclas);
            Method[] methods = jclas.getMethods();
            int index;
            for (index = 0; index < methods.length; index++) {
                if (methods[index].getName().equals(methodName)) {
                    break;
                }
            }
            if (index < methods.length) {
                addTimer(cgen, methods[index]);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                cgen.getJavaClass().dump(bos);
                return bos.toByteArray();
            }
            System.err.println("Method " + methodName + " not found in " 
                    + className);
            System.exit(0);
 
        } catch (IOException e) {
            System.err.println(e);
            System.exit(0);
        }
        return null; // No transformation required
    }
 
    private static void addTimer(ClassGen cgen, Method method) {
         
        // set up the construction tools
        InstructionFactory ifact = new InstructionFactory(cgen);
        InstructionList ilist = new InstructionList();
        ConstantPoolGen pgen = cgen.getConstantPool();
        String cname = cgen.getClassName();
        MethodGen wrapgen = new MethodGen(method, cname, pgen);
        wrapgen.setInstructionList(ilist);
         
        // rename a copy of the original method
        MethodGen methgen = new MethodGen(method, cname, pgen);
        cgen.removeMethod(method);
        String iname = methgen.getName() + "_timing";
        methgen.setName(iname);
        cgen.addMethod(methgen.getMethod());
        Type result = methgen.getReturnType();
         
        // compute the size of the calling parameters
        Type[] parameters = methgen.getArgumentTypes();
        int stackIndex = methgen.isStatic() ? 0 : 1;
        for (int i = 0; i < parameters.length; i++) {
            stackIndex += parameters[i].getSize();
        }
         
        // save time prior to invocation
        ilist.append(ifact.createInvoke("java.lang.System",
            "currentTimeMillis", Type.LONG, Type.NO_ARGS, 
            Constants.INVOKESTATIC));
        ilist.append(InstructionFactory.
            createStore(Type.LONG, stackIndex));
         
        // call the wrapped method
        int offset = 0;
        short invoke = Constants.INVOKESTATIC;
        if (!methgen.isStatic()) {
            ilist.append(InstructionFactory.
                createLoad(Type.OBJECT, 0));
            offset = 1;
            invoke = Constants.INVOKEVIRTUAL;
        }
        for (int i = 0; i < parameters.length; i++) {
            Type type = parameters[i];
            ilist.append(InstructionFactory.
                createLoad(type, offset));
            offset += type.getSize();
        }
        ilist.append(ifact.createInvoke(cname, 
            iname, result, parameters, invoke));
         
        // store result for return later
        if (result != Type.VOID) {
            ilist.append(InstructionFactory.
                createStore(result, stackIndex+2));
        }
         
        // print time required for method call
        ilist.append(ifact.createFieldAccess("java.lang.System",
            "out",  new ObjectType("java.io.PrintStream"),
            Constants.GETSTATIC));
        ilist.append(InstructionConstants.DUP);
        ilist.append(InstructionConstants.DUP);
        String text = "Call to method " + methgen.getName() +
            " took ";
        ilist.append(new PUSH(pgen, text));
        ilist.append(ifact.createInvoke("java.io.PrintStream",
            "print", Type.VOID, new Type[] { Type.STRING },
            Constants.INVOKEVIRTUAL));
        ilist.append(ifact.createInvoke("java.lang.System", 
            "currentTimeMillis", Type.LONG, Type.NO_ARGS, 
            Constants.INVOKESTATIC));
        ilist.append(InstructionFactory.
            createLoad(Type.LONG, stackIndex));
        ilist.append(InstructionConstants.LSUB);
        ilist.append(ifact.createInvoke("java.io.PrintStream",
            "print", Type.VOID, new Type[] { Type.LONG },
            Constants.INVOKEVIRTUAL));
        ilist.append(new PUSH(pgen, " ms."));
        ilist.append(ifact.createInvoke("java.io.PrintStream",
            "println", Type.VOID, new Type[] { Type.STRING },
            Constants.INVOKEVIRTUAL));
             
        // return result from wrapped method call
        if (result != Type.VOID) {
            ilist.append(InstructionFactory.
                createLoad(result, stackIndex+2));
        }
        ilist.append(InstructionFactory.createReturn(result));
         
        // finalize the constructed method
        wrapgen.stripAttributes(true);
        wrapgen.setMaxStack();
        wrapgen.setMaxLocals();
        cgen.addMethod(wrapgen.getMethod());
        ilist.dispose();
    }
 
    public static void premain(String options, Instrumentation ins) {
        if (options != null) {
            ins.addTransformer(new Timing(options));
        } else {
            System.out
                    .println("Usage: java -javaagent:Timing.jar=\"class:method\""); 
            System.exit(0);
        }
 
    }
}