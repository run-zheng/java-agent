package org.demo.java.agent;

import java.util.*;

/**
 * 调用链跟踪配置类
 * @author zhengrun
 */
public class InvokeChainConfig {
    public static final String INVOKE_CHAIN_SHOW_PACKAGE = "invoke.chain.show.package";
    public static final String INVOKE_CHAIN_EXCLUDE_PACKAGE="invoke.chain.exclude.package";
    public static final String INVOKE_CHAIN_EXCLUDE_IGNORE_DEFAULT="invoke.chain.exclude.ignore-default";
    public static final String INVOKE_CHAIN_DUMP_INNER_CLASS = "invoke.chain.dump-inner-class";
    public static final String INVOKE_CHAIN_DUMP_INNER_CLASS_PATH = "invoke.chain.dump-inner-class-path";
    public static final String INVOKE_CHAIN_LOG_FILE = "invoke.chain.log-file";
    public static final String INVOKE_CHAIN_LOG_TYPE = "invoke.chain.log-type";
    public static final String INVOKE_CHAIN_LOG_ENABLE = "invoke.chain.log.enable";
    public static final String INVOKE_CHAIN_IGNORE =  "invoke.chain.ignore";
    public static final String INVOKE_APPEND_BOOTSTRAP_PATHS = "invoke.append.bootstrap.paths";

    public static final String DFFAULT_IGNORE = "getter,setter,enums-constructor,tostring,hashcode,equals";
    public static final String DEFAULT_CHAIN_LOG_FILE = "invoke_chain_logfile.log";
    public static final String DEFAULT_DUMP_INNER_CLASS_PATH = "dump-inner-class";
    public static final String DEFAULT_FALSE_BOOLEAN = "false";
    public static final String DEFAULT_TRUE_BOOLEAN = "true";
    public static final String DEFAULT_LOG_TYPE = LogTypeEnum.CONSOLE.name();
    public static final Set<String> DEFAULT_EXCLUDE_PACKAGE = new HashSet<>(
            Arrays.asList("java.","sun.","javax."));
    /**
     * 启用调用链跟踪的包，会查入字节码
     * -Dinvoke.chain.show.package=
     */
    private Set<String> showPackageSet;
    /**
     * 禁用调用链跟踪的包，不会插入字节码
     * -Dinvoke.chain.exclude.package=
     */
    private Set<String> excludePackageSet ;
    /**
     * 是否忽略被默认排除的包
     * -Dinvoke.chain.exclude.ignore-default=
     */
    private boolean isIgnoreDefaultExcludePackage;
    /**
     * 启用log文件方式下，log文件路径
     * -Dinvoke.chain.log-file=
     */
    private String invokeChainLogFile;
    /**
     * 日志记录类型 @see org.demo.java.agent.LogTypeEnum
     * -Dinvoke.chain.log-type=
     */
    private LogTypeEnum logType;
    /**
     * 是否dump内部类，javassist无法注入字节码到内部类
     * -Dinvoke.chain.dump-inner-class=
     */
    private boolean isDumpInnerClass;
    /**
     * dump内部类保存的路径
     * -Dinvoke.chain.dump-inner-class-path=
     */
    private String dumpClassPath ;
    /**
     * 忽略方法集合
     * -Dinvoke.chain.ignore=
     */
    private Set<String> ignoreLogMethodSet;
    /**
     * append到bootstrap类搜索路径的jar包路径
     * -Dinvoke.append.bootstrap.paths
     */
    private String appendToBootstrapClassLoaderPaths;

    private String agentJarFilePath ;

    public void init(String args){
        this.showPackageSet = initShowPackageSet(args, INVOKE_CHAIN_SHOW_PACKAGE);
        this.isIgnoreDefaultExcludePackage =  getBooleanProperty(INVOKE_CHAIN_EXCLUDE_IGNORE_DEFAULT, DEFAULT_FALSE_BOOLEAN);
        this.excludePackageSet = initExcludePackageSet(INVOKE_CHAIN_EXCLUDE_PACKAGE);
        this.isDumpInnerClass = getBooleanProperty(INVOKE_CHAIN_DUMP_INNER_CLASS,DEFAULT_FALSE_BOOLEAN);
        this.dumpClassPath = getProperty(INVOKE_CHAIN_DUMP_INNER_CLASS_PATH, DEFAULT_DUMP_INNER_CLASS_PATH);
        this.invokeChainLogFile = getProperty(INVOKE_CHAIN_LOG_FILE, DEFAULT_CHAIN_LOG_FILE);
        this.ignoreLogMethodSet = initIgnoreLogMethodSet(INVOKE_CHAIN_IGNORE);
        this.appendToBootstrapClassLoaderPaths = getProperty(INVOKE_APPEND_BOOTSTRAP_PATHS, "");

        initLogType(INVOKE_CHAIN_LOG_TYPE);
        initDefaultDumpClassPath();
    }

    private void initLogType(String propertyKey) {
        String logType = getProperty(propertyKey, DEFAULT_LOG_TYPE);
        if(LogTypeEnum.FILE.name().equalsIgnoreCase(logType)){
            this.logType = LogTypeEnum.FILE;
        }else if(LogTypeEnum.LOG.name().equalsIgnoreCase(logType)){
            this.logType = LogTypeEnum.LOG;
        }else {
            this.logType = LogTypeEnum.CONSOLE;
        }
    }

    private void initDefaultDumpClassPath() {
        if(DEFAULT_DUMP_INNER_CLASS_PATH.equals(this.dumpClassPath)){
            this.dumpClassPath = Thread.currentThread().getContextClassLoader().getResource("").getPath()+ this.dumpClassPath;
        }
    }

    public boolean getBooleanProperty(String key, String defaultValue){
        return DEFAULT_TRUE_BOOLEAN.equalsIgnoreCase(getProperty(key, defaultValue));
    }

    public String getProperty(String key, String defaultValue){
        return System.getProperty(key, defaultValue).trim();
    }

    public String getAppendToBootstrapClassLoaderPath() {
        return appendToBootstrapClassLoaderPaths;
    }

    public String getAgentJarFilePath() {
        return agentJarFilePath;
    }

    public void setAgentJarFilePath(String agentJarFilePath) {
        this.agentJarFilePath = agentJarFilePath;
    }

    public List<String> getAppendToBootstrapClassLoaderPathList(){
        if(appendToBootstrapClassLoaderPaths != null &&
                !("".equalsIgnoreCase(appendToBootstrapClassLoaderPaths))){
            String[] paths = appendToBootstrapClassLoaderPaths.split(";");
            List<String> result = new ArrayList();
            if(paths != null) {
                for (int i = 0; i < paths.length; i++) {
                    if(paths [i] != null && !("".equals(paths[i]))){
                        result.add(paths[i]);
                    }
                }
                return result;
            }
        }
        return  new ArrayList();
    }

    public boolean isInvokeChainLogEnable() {
        return getBooleanProperty(INVOKE_CHAIN_LOG_ENABLE, DEFAULT_TRUE_BOOLEAN);
    }

    private Set<String> initIgnoreLogMethodSet(String propertyKey) {
        Set<String> ignoreLogMethodSet = new HashSet<> ();
        String ignoreLogMethods  = getProperty(propertyKey, DFFAULT_IGNORE);
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

    private Set<String> initExcludePackageSet(String propertyKey) {
        Set<String> excludeSet = new HashSet<>();
        excludeSet.addAll(DEFAULT_EXCLUDE_PACKAGE);

        if(this.isIgnoreDefaultExcludePackage){
            excludeSet.clear();
        }
        parsePackageSet(excludeSet, propertyKey);
        return excludeSet;
    }

    private Set<String> initShowPackageSet(String args, String propertyKey) {
        final Set<String> packageSet = new HashSet<String>();
        if(args != null && args.trim().length() > 0 ) {
            String[] packages = args.split(":");
            addToSet(packageSet, packages);
        }
        parsePackageSet(packageSet, propertyKey);
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
