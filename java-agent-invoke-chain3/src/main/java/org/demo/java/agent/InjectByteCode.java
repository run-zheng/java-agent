package org.demo.java.agent;

import javassist.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class InjectByteCode {
    private static final BootLogger log = BootLogger.getLogger(InjectByteCode.class.getName());

    private static final Set<String> hasInjectClass = new HashSet<>();

    private boolean myResult;
    private byte[] classfileBuffer;
    private String packageClass;
    private byte[] bytes;
    private InvokeChainConfig config;
    private ClassLoader classLoader;
    public InjectByteCode(InvokeChainConfig config,byte[] classfileBuffer, String packageClass, ClassLoader classLoader) {
        this.classfileBuffer = classfileBuffer;
        this.packageClass = packageClass;
        this.config = config;
        this.classLoader = classLoader;
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
                            try {
                                behavior.insertBefore("InvokeChainLogger.log(\"" + className + "\", \"" + behavior.getLongName() + "\",\"" + sb.toString() + "\");");
                                behavior.insertAfter("InvokeChainLogger.leave();");

                                hasInject = true;
                            }catch(Exception ex){
                                log.error(ex.getMessage(), ex);
                                if(ex instanceof javassist.CannotCompileException
                                    && ex.getCause() != null
                                    && ex.getCause() instanceof javassist.NotFoundException ){
                                    ClassLoader cl = classLoader;
                                    if(cl == null){
                                        cl = Thread.currentThread().getContextClassLoader();
                                    }
                                    try{
                                        cl.loadClass(ex.getCause().getMessage());
                                    }catch(Exception lx){
                                        log.error(lx.getMessage(), lx);
                                        //ignore
                                        lx = null;
                                    }
                                }
                            }
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