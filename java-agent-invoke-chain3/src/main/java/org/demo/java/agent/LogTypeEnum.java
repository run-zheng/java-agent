package org.demo.java.agent;

public enum LogTypeEnum {
    CONSOLE, LOG, FILE;

    public boolean isLog(){
        return this == LOG;
    }
    public boolean isFile(){
        return this == FILE;
    }
    public boolean isConsole(){
        return this == CONSOLE;
    }

    public static boolean isLog(String type){
        return LOG.name().equalsIgnoreCase(type);
    }

    public static boolean isFile(String type){
        return FILE.name().equalsIgnoreCase(type);
    }

    public static boolean isConsole(String type){
        return CONSOLE.name().equalsIgnoreCase(type);
    }
}
