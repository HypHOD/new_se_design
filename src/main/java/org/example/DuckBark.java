package org.example;
import java.io.IOException;

public class DuckBark {
    public interface BehaveStrategy { void makeBehave(); }
    public interface SoundStrategy { void makeSound(); }

    // 行为组接口
    public interface DuckAction{
        BehaveStrategy getBehaveStrategy();
        SoundStrategy getSoundStrategy();
    }

    // -------------------得分行为----------------------
    public static class GetPointAction implements DuckAction {
        @Override
        public BehaveStrategy getBehaveStrategy() {
            return new Behave_GetPoint();
        }
        @Override
        public SoundStrategy getSoundStrategy() {
            return new Sound_GetPoint();
        }
    }

    // Behave不同的实现
    static class Behave_GetPoint implements BehaveStrategy {
        @Override public void makeBehave() {System.out.println("Behave_GetPoint");}
    }

    // Sound不同的实现
    static class Sound_GetPoint implements SoundStrategy {
        @Override public void makeSound() {
            String txt="又要到饭啦";
            TTSUtil.speak(txt);
        }
    }

    // 异步执行
    public static class AsyncActionUtil{
        // 创建线程池
        private static final java.util.concurrent.ExecutorService executor =
                java.util.concurrent.Executors.newCachedThreadPool(
                        r ->{
                            Thread t = new Thread(r);
                            t.setName("AsyncActionUtil");
                            t.setDaemon(true);
                            return t;
                        }
                );
        // 异步执行任务
        public static void execute(Runnable r){
            if(r==null)return;
            try {
                executor.submit(()->{
                    try{
                        r.run();
                    }catch (Exception e){
                        System.out.println(e.getMessage());
                    }
                });
            }catch (Exception e){
                r.run();
            }
        }
        //退出程序 关闭线程池 防止资源泄漏
        public static void shutdown(){
            executor.shutdown();
            try{
                if(!executor.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS))
                    executor.shutdownNow();
            }catch (InterruptedException e){
                executor.shutdownNow();
            }
        }
    }

    /* 封装 TTS */
    static class TTSUtil {
        public static void speak(String text) {
            try {
                String cmd;
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    // PowerShell 调用 SAPI
                    cmd = "powershell -ExecutionPolicy Bypass -Command \"Add-Type –AssemblyName System.Speech; " +
                            "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('" + text + "');\"";
                } else if (os.contains("mac")) {
                    cmd = "say " + text;
                } else {               // Linux
                    cmd = "espeak \"" + text + "\"";
                }
                new ProcessBuilder(cmd.split(" ")).inheritIO().start().waitFor();
            } catch (IOException | InterruptedException e) {
                System.err.println("TTS 失败: " + e.getMessage());
            }
        }
    }

    static class Duck {
        private BehaveStrategy behaveStrategy;
        private SoundStrategy soundStrategy;

        // 构造方法：默认使用得点行为组
        public Duck() {
            setAction(new GetPointAction());
        }

        // 切换行为组
        public void setAction(DuckAction action) {
            this.behaveStrategy = action.getBehaveStrategy();
            this.soundStrategy = action.getSoundStrategy();
        }

        // 兼容原有单独切换策略的方法（可选保留）
        public void setBehaveStrategy(BehaveStrategy behaveStrategy) {
            this.behaveStrategy = behaveStrategy;
        }

        public void setSoundStrategy(SoundStrategy soundStrategy) {
            this.soundStrategy = soundStrategy;
        }

        // act()方法不变：自动执行当前绑定的策略
        public void act() {
            if (behaveStrategy != null) behaveStrategy.makeBehave();
            if (soundStrategy != null) soundStrategy.makeSound();
        }
    }
}
