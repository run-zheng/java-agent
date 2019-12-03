package org.demo.java.agent;

import javassist.*;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LineNumberAttribute;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

/**
 * -javaagent:.../java-agent-invoke-chain3.jar
 * -Dinvoke.chain.show.package=com.test:com.demo
 * -Dinvoke.chain.exclude.package=com.demo.trace:org.springframework
 * -Dinvoke.chain.exclude.ignore-default=false   default=false
 *
 * -Dinvoke.chain.log-type=log | console | file  default=console
 * -Dinvoke.chain.log-file=...  default=invoke_chain_logfile.log
 *
 * -Dinvoke.chain.dump-inner-class=...  default=false
 * -Dinvoke.chain.dump-inner-class-path=... default=dump-inner-class
 *
 * -Dinvoke.chain.ignore=...,...
 *     none | getter | setter | enums-constructor | tostring | hashcode | equals
 *     default=getter,setter,enums-constructor,tostring,hashcode,equals
 *
 */
public class Agent {
    private static final Set<String> hasInjectClass = new HashSet<>();
    /**
     * package name
     *
     * @param args
     * @param inst
     */
    public static void premain(String args, Instrumentation inst){
        System.out.println("================================Invoke chain java agent premain instrument======================");
        final InvokeChainConfig config = new InvokeChainConfig();
        config.init(args);
        InvokeChainLogger.init(config);
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                String packageClass = className.replaceAll("/", ".");
                if (config.getShowPackageSet() == null || config.getShowPackageSet().size() <= 0){
                    if(!packageClass.startsWith("org.demo.java.agent")
                        && !config.getExcludePackageSet().contains(packageClass)) {
                        boolean isExclude = isExclude(packageClass,config.getExcludePackageSet());
                        if(!isExclude){
                            InjectByteCode injectByteCode = new InjectByteCode(config,classfileBuffer, packageClass).invoke();
                            if (injectByteCode.is()) {
                                return injectByteCode.getBytes();
                            }
                        }
                    }
                }else {
                    for (String packageName : config.getShowPackageSet()) {
                        if (packageClass.startsWith(packageName) &&
                                !packageClass.startsWith("org.demo.java.agent")
                                &&  !config.getExcludePackageSet().contains(packageClass)) {
                            boolean isExclude = isExclude(packageClass, config.getExcludePackageSet());
                            if(!isExclude) {
                                InjectByteCode injectByteCode = new InjectByteCode(config,classfileBuffer, packageClass).invoke();
                                if (injectByteCode.is()) {
                                    return injectByteCode.getBytes();
                                }
                            }
                            break;
                        }
                    }
                }
                return classfileBuffer;
            }
        });
    }

    private static boolean isExclude(String packageClass, Set<String> excludeSet) {
        boolean isExclude = false;
        for (String excludePackageName : excludeSet) {
            if (packageClass.startsWith(excludePackageName)) {
                isExclude = true;
            }
        }
        return isExclude;
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
        private InvokeChainConfig config;
        public InjectByteCode(InvokeChainConfig config,byte[] classfileBuffer, String packageClass) {
            this.classfileBuffer = classfileBuffer;
            this.packageClass = packageClass;
            this.config = config;
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
                String className = ctClass.getName();
                boolean hasInject = false;

                if(!ctClass.isInterface() &&  !hasInjectClass.contains(className)  && ! (className.indexOf("$") > 0) ){
                    CtBehavior[] declaredBehaviors = ctClass.getDeclaredBehaviors();
                    String methodName = null;
                    boolean isIgnoreLog = false;
                    for (CtBehavior behavior: declaredBehaviors) {
                        if (!Modifier.isAbstract(behavior.getModifiers())
                                && !Modifier.isNative(behavior.getModifiers())) {
                            //System.out.println("Inject byte code class: "+ packageClass + " method: " + behavior.getName());
                            methodName = behavior.getName();
                            isIgnoreLog = false;
                            if(methodName.startsWith("get") && methodName.length() > 0 ){
                                isIgnoreLog = isIgnoreLogGetterOrSetter(ctClass, methodName, "get");
                            }else if(methodName.startsWith("set") && methodName.length() > 0 ){
                                isIgnoreLog = isIgnoreLogGetterOrSetter(ctClass, methodName, "set");
                            }else if(ctClass.isEnum() ){
                                if("<init>".equalsIgnoreCase(methodName)
                                        || ctClass.getSimpleName().equalsIgnoreCase(methodName)) {
                                    if (config.getIgnoreLogMethodSet().contains("enums-constructor")) {
                                        isIgnoreLog = true;
                                    }
                                }
                            }else {
                                if(config.getIgnoreLogMethodSet().contains(methodName)){
                                    isIgnoreLog = true;
                                }
                            }
                            if(!isIgnoreLog) {
                                StringBuilder sb = new StringBuilder();
                                final int lineNumber = behavior.getMethodInfo().getLineNumber(0);
                                if (lineNumber > 0) {
                                    sb.append("(").append(ctClass.getName()).append(":").append(lineNumber).append(")");
                                } else {
                                    sb.append("");
                                }
                                behavior.insertBefore("InvokeChainLogger.log(\"" + className + "\", \"" + behavior.getLongName() + "\",\"" + sb.toString() + "\");");
                                behavior.insertAfter("InvokeChainLogger.leave();");
                                hasInject = true;
                            }
                        }
                    }
                }else if(className.indexOf("$") > 0 && config.isDumpInnerClass()){
                    File dir = new File(Thread.currentThread().getContextClassLoader().getResource("").getPath()+config.getDumpClassPath());
                    if(!dir.exists()){
                        dir.mkdirs();
                    }
                    ctClass.writeFile(dir.getPath());
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

        private boolean isIgnoreLogGetterOrSetter(CtClass ctClass, String methodName, String prefix) throws NotFoundException {
            boolean isIgnoreLog = false;
            String  fieldName = methodName.replace(prefix, "");
            if(fieldName.trim().length() > 0) {
                fieldName = String.valueOf(fieldName.subSequence(0, 1)).toLowerCase()
                        + (fieldName.length() > 1? fieldName.substring(1):"");
                try {
                    CtField field = ctClass.getDeclaredField(fieldName);
                    if (field != null && config.getIgnoreLogMethodSet().contains(prefix + "ter")) {
                        isIgnoreLog = true;
                    }
                } catch (NotFoundException e) {
                    //ignore
                }
            }
            return isIgnoreLog;
        }
    }
}
