package org.demo.java.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileAsyncWriter {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvokeChainLogger.class);

    private LinkedBlockingQueue<String> queue;
    private ExecutorService singleThreadPool;
    private PrintWriter writer;
    private AtomicInteger retryTimes = new AtomicInteger(5);
    private Lock lock = new ReentrantLock();
    private volatile boolean isExit = false;
    private FileAsyncWriter() {
        queue = new LinkedBlockingQueue<String>();
        singleThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        SecurityManager s = System.getSecurityManager();
                        ThreadGroup group = (s != null) ? s.getThreadGroup() :
                                Thread.currentThread().getThreadGroup();
                        Thread t = new Thread(group, r,"FileAsyncWriter-thread",
                                0);
                        t.setDaemon(true);
                        return t ;
                    }
                });
        Runnable runnable =  new Runnable() {
            @Override
            public void run() {
                while (!isExit) {
                    try {
                        String formatLog = queue.take();
                        writer.write(formatLog);
                    } catch (InterruptedException e) {
                        log.error("写入日志异常:" + e.getMessage(), e);
                    }
                }
            }
        };
        singleThreadPool.submit(runnable);
        initFileWriter();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                isExit = true ;
            }
        }));
    }

    private void initFileWriter() {
        if (writer == null) {
            lock.lock();
            try {
                if (writer == null) {
                    int tryTimes = retryTimes.decrementAndGet();
                    if (tryTimes > 0) {
                        String invokeChainLogFile = System.getProperty("invoke.chain.logfile");
                        if (invokeChainLogFile == null || "".equalsIgnoreCase(invokeChainLogFile.trim())) {
                            invokeChainLogFile = getClass().getClassLoader().getResource("").getPath() + "/invoke_chain_logfile.log";
                        }
                        try {
                            File file = new File(invokeChainLogFile);
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                            writer = new PrintWriter(new FileWriter(file, true), true);
                        } catch (IOException e) {
                            log.error("创建/读取invoke.chain.logfile失败: " + invokeChainLogFile + " errrorMsg: " + e.getMessage(), e);
                        }
                    } else {
                        log.error("文件初始化超过重试次数");
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void log(String formatLog) {
        try {
            if ((writer == null || writer.checkError()) && retryTimes.get() > 0) {
                initFileWriter();
            }
            if (writer != null) {
                queue.put(formatLog);
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    private enum FileAsyncWriterHolder {
        INSTANCE;
        private FileAsyncWriter fileAsyncWriter;

        FileAsyncWriterHolder() {
            fileAsyncWriter = new FileAsyncWriter();
        }

        public FileAsyncWriter getInstance() {
            return this.fileAsyncWriter;
        }
    }

    public static FileAsyncWriter getInstance() {
        FileAsyncWriterHolder holder = FileAsyncWriterHolder.INSTANCE;
        if(holder != null){
            return holder.getInstance();
        }else {
            return null;
        }

    }
}
