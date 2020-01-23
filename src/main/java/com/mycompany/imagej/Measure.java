package com.mycompany.imagej;

import java.io.FileOutputStream;

public class Measure {
    private static int round = 0;

    public interface Runnable {
        void run() throws Exception;
    }

    public interface Supplier<T> {
        T run() throws Exception;
    }

    public static void nextRound() {
        round++;
    }

    public static <T> T measure(String desc, Supplier<T> cb) throws Exception {
        long start = System.nanoTime();
        T ret = cb.run();
        int time_ms = (int) ((System.nanoTime() - start) / 1000000.0);
        System.out.println(desc + ": " + time_ms / 1000.0 + "s");

        try(FileOutputStream out = new FileOutputStream("stats.csv", true)) {
  //          FileLock lock = out.getChannel().lock();
            out.write((desc + "," + MPIUtils.getRank() + "," + MPIUtils.getSize() + "," + round + "," + time_ms + "\n").getBytes());
//            lock.release();
        }

        return ret;
    }

    public static void measure(String desc, Runnable cb) throws Exception {
        measure(desc, ()->{
            cb.run();
            return null;
        });
    }

    public static <T> T measureCatch(String desc, Supplier<T> cb) {
       try {
           return measure(desc, cb);
       } catch(Exception e) {
           e.printStackTrace();
           System.exit(0);
       }
       return null;
    }

    public static void measureCatch(String desc, Runnable cb) {
        try {
            measure(desc, cb);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
