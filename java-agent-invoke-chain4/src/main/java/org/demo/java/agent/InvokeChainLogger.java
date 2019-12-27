package org.demo.java.agent;

import java.util.concurrent.atomic.AtomicInteger;

public class InvokeChainLogger {
    private static final ThreadLocal<AtomicInteger>  invokeDeeps = new ThreadLocal<>();
    private static final BootLogger log = BootLogger.getLogger(InvokeChainLogger.class.getName());
    private static InvokeChainConfig config;

    public static void init(InvokeChainConfig invokeChainConfig){
        config = invokeChainConfig;
    }

    public static void log(String className, String methodName, String lineNumberInfo){
        if(invokeDeeps.get() == null){
            invokeDeeps.set(new AtomicInteger(0));
        }
        long l = invokeDeeps.get().incrementAndGet();

        if(!config.isInvokeChainLogEnable()){
            return ;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l ; i++){
            sb.append("├");
        }


        String threadName = Thread.currentThread().getName();
        threadName = threadName.contains(")") ? threadName.replaceAll("\\)", " )"): threadName;

        lineNumberInfo = (lineNumberInfo!=null? lineNumberInfo:"");

        String methodSimpleName = methodName.substring(className.length());
        if(methodSimpleName.startsWith("(")){
            methodName = className + className.substring(className.lastIndexOf("."))+methodSimpleName;
        }


        String logInfo = methodName.substring(0, methodName.indexOf("("))+lineNumberInfo+methodName.substring(methodName.indexOf("("));
        if(config.getLogType().isFile()){
            FileAsyncWriter instance = FileAsyncWriter.getInstance(config);
            if(instance!=null) {
                instance.log(String.format("%-20s  %s\r\n", threadName, sb.toString() +" at "+ logInfo));
            }
        }else if(config.getLogType().isLog()){
            if(log != null) {
                log.info(String.format("%-20s  %s", threadName, sb.toString() + " at " + logInfo));
            }
        }else {
            //default console
            System.out.printf("%-20s  %s\r\n",threadName, sb.toString() +" at "+logInfo);
        }
    }


    public static void leave(){
        if(invokeDeeps.get() == null){
             return;
        }
       invokeDeeps.get().decrementAndGet();
    }
}
