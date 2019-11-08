package org.demo.java.agent;

import java.lang.instrument.Instrumentation;

public class Agent {
    public static void premain(String args, Instrumentation inst){
        int loadedClass = inst.getAllLoadedClasses().length;

        System.out.println("================================Java agent premain instrument======================");
        System.out.println("agent  args: " + args);
        System.out.println("isRetransformClassesSupported: " + inst.isRetransformClassesSupported());
        System.out.println("isRedefineClassesSupported: " + inst.isRedefineClassesSupported());
        System.out.println("isNativeMethodPrefixSupported: " + inst.isNativeMethodPrefixSupported());
        System.out.println("agent's ClassLoader:  " + Agent.class.getClassLoader().getClass().getName());
        System.out.println("getAllLoadedClasses: ");
        Class[] allLoadedClasses = inst.getAllLoadedClasses();
        for (Class clazzz: allLoadedClasses) {
            System.out.printf("  className: %-100s  loader: %-50s\r\n" , clazzz.getName() ,
                    clazzz.getClassLoader() != null ? clazzz.getClassLoader().getClass().getName() : "<NULL>");
        }
        System.out.println("Premain start loaded classes: " + loadedClass + " premain end loaded classes: "+ inst.getAllLoadedClasses().length);
    }

    public static void premain(String args){
        System.out.println("================================Java agent premain======================");
        System.out.println("agent  args: " + args);
    }
}
