package org.demo.java.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.regex.Pattern;

public class TargetClassFileTransformer implements ClassFileTransformer {
    private static final BootLogger log = BootLogger.getLogger(TargetClassFileTransformer.class.getName());
    private InvokeChainConfig config;

    public TargetClassFileTransformer(InvokeChainConfig config) {
        this.config = config;
    }

    public void setConfig(InvokeChainConfig config){
        this.config = config;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(className == null){
            return classfileBuffer;
        }
        String packageClass = className.replaceAll("/", ".");
        /*log.info("className: " + className + " showSetSize："+ config.getShowPackageSet().size()
                + " excludeSetSize: " + config.getExcludePackageSet().size()
                + " classLoader: " + (loader != null ? loader.getClass().getName() : "null") );*/
        if(!packageClass.startsWith("org.demo.java.agent")){
            String excludePackage = matchPackage(packageClass, config.getExcludePackageSet());
            String includePackage = matchPackage(packageClass, config.getShowPackageSet());
            boolean isShow = false;
            if(config.getShowPackageSet().size() <= 0 && excludePackage == null ){
                isShow = true;
            }else if(includePackage != null && (excludePackage == null
                || (includePackage.startsWith(excludePackage) && includePackage.length() > excludePackage.length()))){
                isShow = true;
            }
            if(isShow ) {
                InjectByteCode injectByteCode = new InjectByteCode(config, packageClass
                        , classfileBuffer, packageClass, loader).invoke();
                if (injectByteCode.is()) {
                    return injectByteCode.getBytes();
                }
            }
        }
        return classfileBuffer;
    }

    private static String matchPackage(String packageClass, Set<String> set){
        for (String packageName : set) {
            if(packageClass.startsWith(packageName)){
                return packageName;
            }
        }
        return null;
    }
}
