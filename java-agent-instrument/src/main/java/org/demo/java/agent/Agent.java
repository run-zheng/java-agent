package org.demo.java.agent;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import	java.util.Set;
import	java.util.HashMap;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Agent {
    /*int loadedClass = inst.getAllLoadedClasses().length;

        System.out.println("================================Java Agent premain instrument======================");
        System.out.println("Agent  args: " + args);
        System.out.println("isRetransformClassesSupported: " + inst.isRetransformClassesSupported());
        System.out.println("isRedefineClassesSupported: " + inst.isRedefineClassesSupported());
        System.out.println("isNativeMethodPrefixSupported: " + inst.isNativeMethodPrefixSupported());
        System.out.println("Agent's ClassLoader:  " + Agent.class.getClassLoader().getClass().getName());
        System.out.println("getAllLoadedClasses: ");
        Class[] allLoadedClasses = inst.getAllLoadedClasses();
        for (Class clazzz: allLoadedClasses) {
            System.out.printf("  className: %-100s  loader: %-50s\r\n" , clazzz.getName() ,
                    clazzz.getClassLoader() != null ? clazzz.getClassLoader().getClass().getName() : "<NULL>");
        }
        System.out.println("Premain start loaded classes: " + loadedClass + " premain end loaded classes: "+ inst.getAllLoadedClasses().length);*/

    /**
     *  args格式： className1:methodName11:methodName12;class2Name:methodName21
     * @param args
     * @param inst
     */
    public static void premain(String args, Instrumentation inst){
        System.out.println("================================Java Agent premain instrument======================");
        System.out.println("Agent  args: " + args);
        String[] classMethods = args.split(";");

        final Map<String, Set<String>> classMethodMap = new HashMap<String, Set<String> > ();
        for (String classMethodList: classMethods) {
            int indexOfClass =  classMethodList.indexOf(":");
            String className = classMethodList.substring(0, indexOfClass);
            String[] methods = classMethodList.substring(indexOfClass+1).split(":");
            Set<String> methodSet = new HashSet<>();
            for (String methodName: methods) {
                methodSet.add(methodName);
            }
            classMethodMap.put(className, methodSet);
        }

        /*inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                String packageClass = className.replaceAll("/", ".");
                System.out.printf("Transform class: %-100s  loader: %-50s\r\n" , packageClass ,
                        loader != null ? loader.getClass().getName() : "<NULL>");
                return classfileBuffer;
            }
        });*/
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                String packageClass = className.replaceAll("/", ".");
                if(classMethodMap.containsKey(packageClass)){
                    CtClass ctClass = null;
                    Set<String> methodSet = classMethodMap.get(packageClass);
                    try {
                        ctClass = ClassPool.getDefault().makeClass(new ByteArrayInputStream(classfileBuffer));
                        if(!ctClass.isInterface()){
                            CtBehavior[] declaredBehaviors = ctClass.getDeclaredBehaviors();
                            for (CtBehavior behavior: declaredBehaviors) {
                                if(methodSet.contains(behavior.getName())){
                                    System.out.println("Inject byte code class: "+ packageClass + " method: " + behavior.getName());

                                    behavior.addLocalVariable("start", CtClass.longType);
                                    behavior.insertBefore("start = System.currentTimeMillis();");
                                    behavior.insertAfter("System.out.println(\"Method cost by agent...method: "+
                                            behavior.getName() + " cost: \" + (System.currentTimeMillis() - start ));");
                                }
                            }
                        }
                        return ctClass.toBytecode();
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }finally{
                        if(ctClass != null){
                            ctClass.detach();
                        }
                    }
                }
                return classfileBuffer;
            }
        });
    }

    public static void premain(String args){
        System.out.println("================================Java Agent premain======================");
        System.out.println("Agent  args: " + args);
    }
}
