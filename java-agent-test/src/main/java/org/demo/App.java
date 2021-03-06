package org.demo;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class App {
    //-javaagent:H:/programmer/works/works-learning/java-agent/java-agent-demo/java-agent-invoke-chain3/target/java-agent-invoke-chain3.jar -Dexclude.package=org.slf4j:ch.qos:com.alibaba.fastjson.asm:org.groovy.debug:com.intellij -Dinvoke.chain.logtype=log
    //-javaagent:H:/programmer/works/works-learning/java-agent/java-agent-demo/java-agent-invoke-chain3/target/java-agent-invoke-chain3.jar -Dexclude.package=org.slf4j:ch.qos:com.alibaba.fastjson.asm:org.groovy.debug:com.intellij
    public static void main( String[] args ) throws ClassNotFoundException, InterruptedException {
        System.out.println( "Hello World!" );

        //log.info("Start long operation...");
        longOperation();

        /*logback slf4j log 调用链分析*/
        //log.info("End of long operation...");

        System.out.println("----at com.alibaba.fastjson.parser.Feature.Feature(com.alibaba.fastjson.parser.Feature:146)()");
        System.out.println("----at com.alibaba.fastjson.parser.DefaultJSONParser.setContext(com.alibaba.fastjson.parser.DefaultJSONParser:1331)(java.lang.Object,java.lang.Object)");
        System.out.println("----at com.alibaba.fastjson.parser.SymbolTable.addSymbol(com.alibaba.fastjson.parser.SymbolTable:86)(int,int,int,boolean)");
        log.info("Hello World!");


        /*  JSON 调用链分析*/
        List list = new ArrayList();
        list.add("test");
        System.out.println(JSON.toJSONString(list));

        System.out.println(JSON.toJSONString(new App()));

        System.out.println(JSON.toJSONString(new User("jacky", 18)));
        System.out.println(JSON.toJSONString(new User("Joe", 5)));

        String joe = "{\"age\":5,\"name\":\"Joe\"}";
        String jacky = "{\"age\":18,\"name\":\"jacky\"}";
        System.out.println("==========================parseObject jacky========================");
        JSON.parseObject(jacky, User.class);
        System.out.println("==========================parseObject joe========================");
        JSON.parseObject(joe, User.class);

        System.out.println("==========================parse jacky========================");
        JSON.parse(jacky);
        System.out.println("==========================parse joe========================");
        JSON.parse(joe);
    }

    public static class User {
        private String name;
        private int age;

        public User() {
        }

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }



    public static void longOperation() throws InterruptedException {
        Thread.sleep(1000);
    }
}
