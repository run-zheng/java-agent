package org.demo.java.agent;

import java.util.HashSet;
import java.util.Set;

public class InvokeChainConfig {
    private Set<String> showPackageSet;
    private Set<String> excludePackageSet ;
    private boolean isIgnoreDefaultExcludePackage;

    private String invokeChainLogFile;
    private LogTypeEnum logType;

    private boolean isDumpInnerClass;
    private String dumpClassPath ;

    private Set<String> ignoreLogMethodSet;

    public void init(String args){
        this.showPackageSet = initShowPackageSet(args);
        this.isIgnoreDefaultExcludePackage = "true".equalsIgnoreCase(System.getProperty(
                "invoke.chain.exclude.ignore-default", "false").trim());
        this.excludePackageSet = initExcludePackageSet();
        this.isDumpInnerClass ="true".equalsIgnoreCase(System.getProperty("invoke.chain.chain.dump-inner-class","false").trim());
        this.dumpClassPath = System.getProperty("invoke.chain.dump-inner-class-path", "dump-inner-class").trim();
        this.invokeChainLogFile = System.getProperty("invoke.chain.log-file", "invoke_chain_logfile.log").trim();
        String logType = System.getProperty("invoke.chain.log-type", "console").trim();
        if("file".equalsIgnoreCase(logType)){
            this.logType = LogTypeEnum.FILE;
        }else if("log".equalsIgnoreCase(logType)){
            this.logType = LogTypeEnum.LOG;
        }else {
            this.logType = LogTypeEnum.CONSOLE;
        }
        this.ignoreLogMethodSet = initIgnoreLogMethodSet();
    }

    public boolean isInvokeChainLogEnable() {
        return Boolean.parseBoolean(String.valueOf(System.getProperty("invoke.chain.log.enable", "true")).toLowerCase());
    }

    private Set<String> initIgnoreLogMethodSet() {
        Set<String> ignoreLogMethodSet = new HashSet<> ();
        String ignoreLogMethods  = System.getProperty("invoke.chain.ignore", "getter,setter,enums-constructor,tostring,hashcode,equals");
        if(ignoreLogMethods != null){
            String[] ignoreLogMethodList = ignoreLogMethods.split(",");
            if(ignoreLogMethodList != null && ignoreLogMethodList.length > 0 ){
                for (String ignoreLogMethod : ignoreLogMethodList){
                    if(ignoreLogMethod != null && !"".equalsIgnoreCase(ignoreLogMethod.trim())) {
                        ignoreLogMethodSet.add(ignoreLogMethod.trim());
                    }
                }
            }
        }
        return ignoreLogMethodSet;
    }

    private Set<String> initExcludePackageSet() {
        Set<String> excludeSet = new HashSet<>();
        excludeSet.add("java.");
        excludeSet.add("sun.");
        excludeSet.add("javax.");
        if(this.isIgnoreDefaultExcludePackage){
            excludeSet.clear();
        }
        parsePackageSet(excludeSet, "invoke.chain.exclude.package");
        return excludeSet;
    }

    private Set<String> initShowPackageSet(String args) {
        final Set<String> packageSet = new HashSet<String>();
        if(args != null && args.trim().length() > 0 ) {
            String[] packages = args.split(":");
            addToSet(packageSet, packages);
        }
        parsePackageSet(packageSet, "invoke.chain.show.package");
        return packageSet;
    }

    private void parsePackageSet(Set<String> packageSet, String s) {
        String packagesProperty = System.getProperty(s);
        if (packagesProperty != null && packagesProperty.trim().length() > 0) {
            String[] packages = packagesProperty.split(":");
            addToSet(packageSet, packages);
        }
    }

    private void addToSet(Set<String> packageSet, String[] packages) {
        for (String packageName : packages) {
            if (packageName != null && !"".equalsIgnoreCase(packageName)) {
                packageSet.add(packageName);
            }
        }
    }

    public Set<String> getIgnoreLogMethodSet() {
        return ignoreLogMethodSet;
    }

    public String getInvokeChainLogFile() {
        return invokeChainLogFile;
    }

    public LogTypeEnum getLogType() {
        return logType;
    }

    public boolean isIgnoreDefaultExcludePackage() {
        return isIgnoreDefaultExcludePackage;
    }

    public Set<String> getShowPackageSet() {
        return showPackageSet;
    }

    public boolean isDumpInnerClass() {
        return isDumpInnerClass;
    }

    public String getDumpClassPath() {
        return dumpClassPath;
    }

    public Set<String> getExcludePackageSet() {
        return excludePackageSet;
    }
}
