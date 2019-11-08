package org.demo.java.agent;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.Modifier;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

public class Agent {
    private static final Set<String> excludeSet = new HashSet<>();
    static {
        excludeSet.add("java.");
        excludeSet.add("sun.");
    }
    /**
     * package name
     *
     * @param args
     * @param inst
     */
    public static void premain(String args, Instrumentation inst){
        System.out.println("================================Java agent premain instrument======================");
        System.out.println("agent  args: " + args);


        final Set<String> packageSet = new HashSet<String>();
        if(args != null && args.trim().length() > 0 ) {
            String[] packages = args.split(":");
            for (String packageName : packages) {
                packageSet.add(packageName);
            }
        }
        String showChainPackages = System.getProperty("show.chain.package");
        if(showChainPackages != null && showChainPackages.trim().length() > 0 ){
            String[] packages = showChainPackages.split(":");
            for (String packageName : packages) {
                packageSet.add(packageName);
            }
        }
        String execludePackages = System.getProperty("exclude.package");
        if(execludePackages != null && execludePackages.trim().length() > 0 ){
            String[] packages = execludePackages.split(":");
            for (String packageName : packages) {
                excludeSet.add(packageName);
            }
        }
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                String packageClass = className.replaceAll("/", ".");
                //System.out.printf("className: %-100s loader: %-100s \r\n", packageClass, (loader != null ? loader.getClass().getName() : "<null>"));
                if (packageSet == null || packageSet.size() <= 0){
                    if(!packageClass.startsWith("org.demo.java.agent")
                        && !excludeSet.contains(packageClass)) {
                        boolean isExclude  = false;
                        for (String packageName : excludeSet) {
                            if (packageClass.startsWith(packageName) ) {
                                isExclude = true;
                            }
                        }
                        if(!isExclude){
                            InjectByteCode injectByteCode = new InjectByteCode(classfileBuffer, packageClass).invoke();
                            if (injectByteCode.is()) {
                                return injectByteCode.getBytes();
                            }
                        }
                    }
                }else {
                    for (String packageName : packageSet) {
                        if (packageClass.startsWith(packageName) &&
                                !packageClass.startsWith("org.demo.java.agent")) {
                            InjectByteCode injectByteCode = new InjectByteCode(classfileBuffer, packageClass).invoke();
                            if (injectByteCode.is()) {
                                return injectByteCode.getBytes();
                            }
                            break;
                        }
                    }
                }
                return classfileBuffer;
            }
        });
    }

    public static void premain(String args){
        System.out.println("================================Java agent premain======================");
        System.out.println("agent  args: " + args);
    }

    private static class InjectByteCode {
        private boolean myResult;
        private byte[] classfileBuffer;
        private String packageClass;
        private byte[] bytes;

        public InjectByteCode(byte[] classfileBuffer, String packageClass) {
            this.classfileBuffer = classfileBuffer;
            this.packageClass = packageClass;
        }

        boolean is() {
            return myResult;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public InjectByteCode invoke() {
            CtClass ctClass = null;
            try {
                ClassPool pool = ClassPool.getDefault();
                pool.importPackage("org.demo.java.agent");
                ctClass =pool.makeClass(new ByteArrayInputStream(classfileBuffer));
                boolean hasInject = false;
                if(!ctClass.isInterface()){
                    CtBehavior[] declaredBehaviors = ctClass.getDeclaredBehaviors();
                    for (CtBehavior behavior: declaredBehaviors) {
                        if(!Modifier.isAbstract(behavior.getModifiers())
                            && !Modifier.isNative(behavior.getModifiers())) {
                            //System.out.println("Inject byte code class: "+ packageClass + " method: " + behavior.getName());
                            CtClass[] parameterTypes = behavior.getParameterTypes();
                            StringBuilder sb = new StringBuilder();
                            if (parameterTypes != null) {
                                for (CtClass parameter : parameterTypes) {
                                    sb.append(parameter.getName()).append(",");
                                }
                            }
                            behavior.insertBefore("InvokeChainLogger.log(\""+ behavior.getLongName() + "\");");
                            behavior.insertAfter("InvokeChainLogger.leave();");
                            hasInject = true;
                        }
                    }
                }
                if(hasInject) {
                    bytes = ctClass.toBytecode();
                }else {
                    bytes = classfileBuffer;
                }
                myResult = true;
                return this;
            }catch(Throwable tx ){
                tx.printStackTrace();
            }finally{
                if(ctClass != null){
                    ctClass.detach();
                }
            }
            myResult = false;
            return this;
        }
    }
}
