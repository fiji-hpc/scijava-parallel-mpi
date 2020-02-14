package com.mycompany.imagej.ops.parallel;

import com.mycompany.imagej.chunk.Chunk;
import net.imagej.ops.special.computer.AbstractNullaryComputerOp;
import net.imglib2.IterableInterval;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;
import com.mycompany.imagej.Utils;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Plugin(type = Parallel.class, priority = 1.0)
public class ThreadedParallel<O> extends AbstractNullaryComputerOp<IterableInterval<O>> implements Parallel {
    @Parameter
    private Consumer<Chunk<O>> action;

    @Parameter
    protected ThreadService threadService;

    @Override
    public void compute(IterableInterval<O> block) {
        int threads = Utils.numThreads();

        Chunk<O> chunks = new Chunk<>(block, threads);
        ArrayList<Future<?>> futures = new ArrayList<>(threads);

        for(int i = 0; i < threads; i++) {
            final int threadNum = i;
            futures.add(this.threadService.run(new Runnable() {
                public void run() {
                    action.accept(chunks.getChunk(threadNum));
                }
            }));
        }

        for(Future<?> future: futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
