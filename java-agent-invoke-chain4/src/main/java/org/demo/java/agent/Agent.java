package org.demo.java.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
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
    private static final BootLogger log = BootLogger.getLogger(Agent.class.getName());

    private static final Pattern DEFAULT_AGENT_PATTERN = Pattern.compile("java-agent-invoke-chain4.jar");

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
            throw new IllegalArgumentException("Read "+jarFilePath+" fail! "+ e.getMessage(), e);
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
            throw new IllegalArgumentException("Connot find java-agent-invoke-chain4.jar in classpath!");
        }
        String agentJarName =  classPath.substring(matcher.start(), matcher.end());
        String agentJarFullPath = parseAgentJarPath(classPath, agentJarName);

        final InvokeChainConfig config = new InvokeChainConfig();
        config.init(args);
        config.setAgentJarFilePath(agentJarFullPath);

        /*JarFile agentJar = getJarFile(agentJarFullPath);
        inst.appendToBootstrapClassLoaderSearch(agentJar);*/

        InvokeChainLogger.init(config);
        inst.addTransformer(new TargetClassFileTransformer(config), true);
    }

    public static void premain(String args){
        System.out.println("================================Java agent premain======================");
        System.out.println("agent  args: " + args);
    }
}
