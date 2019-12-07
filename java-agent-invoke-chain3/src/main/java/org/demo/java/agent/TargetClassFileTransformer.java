package org.demo.java.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Set;

public class TargetClassFileTransformer implements ClassFileTransformer {
    private final InvokeChainConfig config;

    public TargetClassFileTransformer(InvokeChainConfig config) {
        this.config = config;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        String packageClass = className.replaceAll("/", ".");
        if (config.getShowPackageSet().size() <= 0) {
            if (!packageClass.startsWith("org.demo.java.agent")  && !config.getExcludePackageSet().contains(packageClass)) {
                boolean isExclude = isExclude(packageClass, config.getExcludePackageSet());
                if (!isExclude) {
                    InjectByteCode injectByteCode = new InjectByteCode(config, classfileBuffer, packageClass,loader).invoke();
                    if (injectByteCode.is()) {
                        return injectByteCode.getBytes();
                    }
                }
            }
        } else {
            for (String packageName : config.getShowPackageSet()) {
                if (packageClass.startsWith(packageName) &&
                        !packageClass.startsWith("org.demo.java.agent")
                        && !config.getExcludePackageSet().contains(packageClass)) {
                    boolean isExclude = isExclude(packageClass, config.getExcludePackageSet());
                    if (!isExclude) {
                        InjectByteCode injectByteCode = new InjectByteCode(config, classfileBuffer, packageClass,loader).invoke();
                        if (injectByteCode.is()) {
                            return injectByteCode.getBytes();
                        }
                    }
                    break;
                }
            }
        }
        return classfileBuffer;
    }


    private static boolean isExclude(String packageClass, Set<String> excludeSet) {
        boolean isExclude = false;
        for (String excludePackageName : excludeSet) {
            if (packageClass.startsWith(excludePackageName)) {
                isExclude = true;
            }
        }
        return isExclude;
    }
}
