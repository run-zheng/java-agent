package org.demo;

import javassist.*;

import java.io.IOException;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws CannotCompileException, NotFoundException, IOException {
        System.out.println("==========ClassPool.getDefault=========");
        ClassPool pool = ClassPool.getDefault();

        System.out.println("==========ClassPool.makeClass=========");
        CtClass user = pool.makeClass("org.demo.model.User");
        System.out.println("==========CtField.make name=========");
        CtField name = CtField.make("private String name;", user);
        System.out.println("==========CtField.make age=========");
        CtField age = CtField.make("private int age;", user);
        System.out.println("==========CtClass.addField name=========");
        user.addField(name);
        System.out.println("==========CtClass.addField age=========");
        user.addField(age);
        System.out.println("==========CtMethod.make getName=========");
        CtMethod getName = CtMethod.make("public String getName() { return name; }", user);
        System.out.println("==========CtMethod.make setName=========");
        CtMethod setName = CtMethod.make("public void setName(String name) { return this.name = name ; }", user);
        System.out.println("==========CtClass.addMethod getName=========");
        user.addMethod(getName);
        System.out.println("==========CtClass.addMethod setName=========");
        user.addMethod(setName);
        System.out.println("==========New CtConstructor=========");
        CtConstructor constructor = new CtConstructor(new CtClass [] {pool.get("java.lang.String"), CtClass.intType}, user);
        System.out.println("==========CtConstructor setBody=========");
        constructor.setBody("{this.name = $1; this.age = $2;}");
        System.out.println("==========CtClass addConstructor=========");
        user.addConstructor(constructor);
        System.out.println("==========CtClass writeFile=========");
        user.writeFile();
        System.out.println("==========END=========");

    }
}
