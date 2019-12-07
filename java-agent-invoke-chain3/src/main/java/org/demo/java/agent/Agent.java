package org.demo.java.agent;

import javassist.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern DEFAULT_AGENT_PATTERN = Pattern.compile("java-agent-invoke-chain3.jar");


    private static String parseAgentJarPath(String classPath, String agentJar) {
        String[] classPathList = classPath.split(File.pathSeparator);
        for (String findPath : classPathList) {
            boolean find = findPath.contains(agentJar);
            if (find) {
                return findPath;
            }
        }
        return null;
    }


    private static JarFile getJarFile(String jarFilePath) {
        try {
            return new JarFile(jarFilePath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Read java-agent-invoke-chain3.jar fail! "+ e.getMessage(), e);
        }
    }
    /**
     * package name
     *
     * @param args
     * @param inst
     */
    public static void premain(String args, Instrumentation inst){
        System.out.println("================================Invoke chain java agent premain instrument======================");

        String classPath = System.getProperty("java.class.path");
        Matcher matcher = DEFAULT_AGENT_PATTERN.matcher(classPath);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Connot find java-agent-invoke-chain3.jar in classpath!");
        }
        String agentJarName =  classPath.substring(matcher.start(), matcher.end());

        String agentJarFullPath = parseAgentJarPath(classPath, agentJarName);
        JarFile agentJar = getJarFile(agentJarFullPath);
        inst.appendToBootstrapClassLoaderSearch(agentJar);


        final InvokeChainConfig config = new InvokeChainConfig();
        config.init(args);
        InvokeChainLogger.init(config);
        inst.addTransformer(new TargetClassFileTransformer(config));

        /*appendJarToBootstrapClassLoader(inst, config);*/
    }
/*
    private static void appendJarToBootstrapClassLoader(Instrumentation inst, InvokeChainConfig config) {
        List<String> appendToBootstrapClassLoaderPathList = config.getAppendToBootstrapClassLoaderPathList();
        if(appendToBootstrapClassLoaderPathList!=null&& appendToBootstrapClassLoaderPathList.size() > 0 ){
            File pathDir = null;
            for (String path : appendToBootstrapClassLoaderPathList){
                pathDir = new File(path);
                if(pathDir.exists() && pathDir.isDirectory()){
                    File[] files = pathDir.listFiles();
                    for (File file: files) {
                        if(file.getName().endsWith(".jar") &&  file.isFile() ){
                            inst.appendToBootstrapClassLoaderSearch(getJarFile(file.getAbsolutePath()));
                        }
                    }
                }else if(path.endsWith(".jar") && pathDir.exists() && pathDir.isFile() ){
                    inst.appendToBootstrapClassLoaderSearch(getJarFile(pathDir.getAbsolutePath()));
                }
            }
        }
    }*/



    public static void premain(String args){
        System.out.println("================================Java agent premain======================");
        System.out.println("agent  args: " + args);
    }
}
