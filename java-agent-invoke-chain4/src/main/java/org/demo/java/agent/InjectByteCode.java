package org.demo.java.agent;

import javassist.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InjectByteCode {
    private static final BootLogger log = BootLogger.getLogger(InjectByteCode.class.getName());
    private static final ConcurrentHashMap<ClassLoader, ClassPool> CLASS_POOL_MAP = new ConcurrentHashMap<ClassLoader, ClassPool> ();
    private static final ClassLoader DEFAULT_CLASS_LOADER_KEY = new ClassLoader(){

    };
    private static final Set<String> hasInjectClass = new HashSet<>();

    private boolean myResult;
    private byte[] classfileBuffer;
    private String packageClass;
    private byte[] bytes;
    private InvokeChainConfig config;
    private ClassLoader classLoader;
    private String className;
    public InjectByteCode(InvokeChainConfig config,String className
            , byte[] classfileBuffer, String packageClass, ClassLoader classLoader) {
        this.classfileBuffer = classfileBuffer;
        this.packageClass = packageClass;
        this.config = config;
        this.classLoader = classLoader;
        this.className = className;
    }

    boolean is() {
        return myResult;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public InjectByteCode invoke() {
        ClassPool pool = getClassPool(classLoader/* == null ?  Thread.currentThread().getContextClassLoader():classLoader*/);
        CtClass ctClass = null;
        try {
            pool.importPackage("org.demo.java.agent");
            pool.importPackage("org.demo.java.agent.InvokeChainLogger");
            ctClass = pool.get(className);
            if(ctClass == null) {
                ctClass = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
            }
            String className = ctClass.getName();
            boolean hasInject = false;

            if(!ctClass.isInterface() &&  !hasInjectClass.contains(className)  && ! (className.indexOf("$") > 0) ){
                CtBehavior[] declaredBehaviors = ctClass.getDeclaredBehaviors();
                String methodName = null;
                boolean isIgnoreLog = false;
                for (CtBehavior behavior: declaredBehaviors) {
                    if (!Modifier.isAbstract(behavior.getModifiers())
                            && !Modifier.isNative(behavior.getModifiers())
                           ) {
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
                                behavior.insertBefore("try{ " +
                                        "    org.demo.java.agent.InvokeChainLogger.log(\"" + className + "\", \"" + behavior.getLongName() + "\",\"" + sb.toString() + "\");" +
                                        //"}catch(Throwable ex){  }");
                                        "}catch(Throwable ex){  System.err.println(ex.getMessage() + \" " + className + "\"+ \" " + behavior.getLongName() + "\"+\" " + sb.toString() + "\"); }");
                                behavior.insertAfter("try{ " +
                                        "    org.demo.java.agent.InvokeChainLogger.leave();" +
                                        //"}catch(Throwable ex){ }");
                                        "}catch(Throwable ex){  System.err.println(ex.getMessage() + \" " + className + "\"+ \" " + behavior.getLongName() + "\"+\" " + sb.toString() + "\"); }");
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
                File dir = new File(config.getDumpClassPath());
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

    private ClassPool getClassPool(ClassLoader classLoader) {
        ClassPool pool = null;
        if (classLoader == null) {
            classLoader = DEFAULT_CLASS_LOADER_KEY;
        }
        pool = CLASS_POOL_MAP.get(classLoader);
        if (null == pool) {
            if(classLoader == DEFAULT_CLASS_LOADER_KEY){
                pool = ClassPool.getDefault();
                appendJarToBootstrapClassLoader(config, pool);
                CLASS_POOL_MAP.put(classLoader, pool);
            }else {
                ClassPool parentPool = getClassPool(classLoader.getParent());
                pool = new ClassPool(parentPool);
                //pool.childFirstLookup = true;
                pool.appendSystemPath();
                pool.appendClassPath(new LoaderClassPath(classLoader));
                appendJarToBootstrapClassLoader(config, pool);
                CLASS_POOL_MAP.put(classLoader, pool);
            }
        }
        return pool;
    }


    private static void appendJarToBootstrapClassLoader(InvokeChainConfig config, ClassPool pool) {
        List<String> appendToBootstrapClassLoaderPathList = config.getAppendToBootstrapClassLoaderPathList();
        appendToBootstrapClassLoaderPathList.add(config.getAgentJarFilePath());
        if(appendToBootstrapClassLoaderPathList!=null && appendToBootstrapClassLoaderPathList.size() > 0 ){
            File pathDir = null;
            for (String path : appendToBootstrapClassLoaderPathList){
                pathDir = new File(path);
                if(pathDir.exists() ){
                    try {
                        pool.appendClassPath(
                                (path.endsWith(".jar") || path.endsWith(".zip"))  ?
                                    path : path+"/*");
                    } catch (NotFoundException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }

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