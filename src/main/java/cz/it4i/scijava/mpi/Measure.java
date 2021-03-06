package cz.it4i.scijava.mpi;

import org.apache.commons.lang3.StringUtils;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.List;

public class Measure {
    public interface Runnable {
        void run() throws Exception;
    }

    public interface Supplier<T> {
        T run() throws Exception;
    }

    private static int round;
    private static List<String> path = new ArrayList<>();

    public static void nextRound() {
        round++;
    }

    public static long start(String desc) {
        path.add(desc);
        return System.nanoTime();
    }

    public static void end(long start) {
        int time_ms = (int) ((System.nanoTime() - start) / 1000000.0);

        String desc = StringUtils.join(path, ";");
        path.remove(path.size() - 1);

        //System.out.println(desc + ": " + time_ms / 1000.0 + "s");

        int round = Measure.round;
        if(System.getenv("B_CURRENT_ROUND") != null) {
            round = Integer.parseInt(System.getenv("B_CURRENT_ROUND"));
        }

        String csvPath = System.getenv("B_STATS_PATH");
        if(csvPath != null) {
            try (FileOutputStream out = new FileOutputStream(csvPath + "." + MPIUtils.getRank(), true)) {
                out.write((desc + "," + MPIUtils.getRank() + "," + MPIUtils.getSize() + "," + round + "," + time_ms + "\n").getBytes());
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <T> T measure(String desc, Supplier<T> cb) throws Exception {
        long start = start(desc);
        T ret = cb.run();
        end(start);
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
           throw new RuntimeException(e);
       }
    }

    public static void measureCatch(String desc, Runnable cb) {
        try {
            measure(desc, cb);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T benchmark(Supplier<T> cb, int rounds) {
        for(int round = 0; round < rounds; round++) {
            MPIUtils.barrier();
            //Utils.print("Round " + round + " started at " + new Date());
            if(round + 1 == rounds) {
              return measureCatch("total_op", cb);
            }

            measureCatch("total_op", cb);

            //Utils.print("Round " + round + " finished at " + new Date());
            Measure.nextRound();
            System.gc();
        }
        return null;
    }
}
