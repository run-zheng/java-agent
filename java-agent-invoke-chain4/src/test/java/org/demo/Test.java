package org.demo;

public class Test {
    public static void main(String[] args) {
        String threadName  = "Pinpoint-Client-Boss(10-0)";

        threadName = threadName.contains(")") ? threadName.replaceAll("\\)", " )"): threadName;
        System.out.println(threadName + " ├ at com.navercorp.pinpoint.bootstrap.PinpointURLClassLoader.loadClass(com.navercorp.pinpoint.bootstrap.PinpointURLClassLoader:57)(java.lang.String,boolean)\n");

    }
}
