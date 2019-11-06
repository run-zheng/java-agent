package org.demo.java.agent;

import java.util.concurrent.atomic.AtomicLong;

public class InvokeChainLogger {
    private static final ThreadLocal<AtomicLong>  invokeDeeps = new ThreadLocal<>();

    public static void log(String methodName){
        if(invokeDeeps.get() == null){
            invokeDeeps.set(new AtomicLong(0));
        }
        long l = invokeDeeps.get().incrementAndGet();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l ; i++){
            sb.append("-");
        }
        String threadName = Thread.currentThread().getName();
        System.out.printf("%-20s  %s\r\n",threadName, sb.toString()+methodName );
    }


    public static void leave(){
        if(invokeDeeps.get() == null){
             return;
        }
       invokeDeeps.get().decrementAndGet();
    }
}
