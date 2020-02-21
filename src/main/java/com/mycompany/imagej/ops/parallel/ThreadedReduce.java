package com.mycompany.imagej.ops.parallel;

import com.mycompany.imagej.chunk.Chunk;
import com.mycompany.imagej.mpi.ReduceOp;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

@Plugin(type = Parallel.class, priority = 0.0, attrs = {@Attr(name = "MPI", value="true")})
public class ThreadedReduce<O extends RealType<O>, R> extends AbstractUnaryFunctionOp<IterableInterval<O>, R> implements Reduce {
    @Parameter
    private Function<Chunk<O>, R> action;

    @Parameter
    ReduceOp reduceOp;

    @Parameter
    public ThreadService threadService;

    @Override
    public R calculate(IterableInterval<O> input) {
        final List<Future<R>> futures = new ArrayList<>();
        for(Chunk<O> chunk: new Chunk<>(input).split(Runtime.getRuntime().availableProcessors()).allChunks()) {
            futures.add(
                     threadService.getExecutorService().submit(() -> action.apply(chunk))
            );
        }

        if(reduceOp == ReduceOp.SUM) {
            double sum = 0;
            for (final Future<R> future : futures) {
                try {
                    sum += (Double) future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            return (R) new Double(sum);
        } else if(reduceOp == ReduceOp.MIN) {
            RealType globalMin = (RealType) input.firstElement().createVariable();
            globalMin.setReal(input.firstElement().createVariable().getMaxValue());
            for (final Future<R> future : futures) {
                try {
                    RealType localMin = (RealType) future.get();
                    if (localMin.compareTo(globalMin) < 0) {
                        globalMin.set(localMin);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            return (R) globalMin;
        } else if(reduceOp == ReduceOp.MAX) {
            RealType globalMax = (RealType) input.firstElement().createVariable();
            globalMax.setReal(input.firstElement().createVariable().getMinValue());
            for (final Future<R> future : futures) {
                try {
                    RealType localMax = (RealType) future.get();
                    if (localMax.compareTo(globalMax) > 0) {
                        globalMax.set(localMax);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            return (R) globalMax;
        } else {
            throw new RuntimeException("Unsupported op");
        }
    }
}
