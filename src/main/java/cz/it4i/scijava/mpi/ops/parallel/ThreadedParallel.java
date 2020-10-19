package cz.it4i.scijava.mpi.ops.parallel;

import cz.it4i.scijava.mpi.Utils;
import cz.it4i.scijava.mpi.chunk.Chunk;
import net.imagej.ops.special.computer.AbstractNullaryComputerOp;
import net.imglib2.IterableInterval;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Plugin(type = Parallel.class, priority = 1.0)
public class ThreadedParallel<O> extends AbstractNullaryComputerOp<IterableInterval<O>> implements Parallel {
    @Parameter
    private Consumer<Chunk<O>> action;

    @Override
    public void compute(IterableInterval<O> block) {
        ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads());

        int jobs = 2 * Utils.numThreads();
        Chunk<O> chunks = new Chunk<>(block, jobs);
        ArrayList<Future<?>> futures = new ArrayList<>(jobs);

        for(int i = 0; i < jobs; i++) {
            final int threadNum = i;
            futures.add(executor.submit(() -> action.accept(chunks.getChunk(threadNum))));
        }

        for(Future<?> future: futures) {
            try {
                future.get();
                //Utils.print("Job finished at " + new Date());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
    }
}
