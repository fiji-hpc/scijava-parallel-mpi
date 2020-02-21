package com.mycompany.imagej.ops.parallel;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.chunk.Chunk;
import com.mycompany.imagej.mpi.ReduceOp;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.IterableInterval;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.function.Function;

@Plugin(type = Parallel.class, priority = 1.0, attrs = {@Attr(name = "MPI", value="true")})
public class MPIReduce<O, R> extends AbstractUnaryFunctionOp<IterableInterval<O>, R> implements Reduce {
    @Parameter
    private Function<Chunk<O>, R> action;

    @Parameter
    ReduceOp reduceOp;

    @Override
    public R calculate(IterableInterval<O> input) {
        Chunk<O> chunks = new Chunk<>(input, MPIUtils.getSize());
        R result = (R) ops().run(ThreadedReduce.class, chunks.getChunk(MPIUtils.getRank()), action, reduceOp);

        return (R) MPIUtils.Allreduce(result, reduceOp);

    }
}
