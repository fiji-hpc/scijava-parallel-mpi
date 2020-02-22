package cz.it4i.scijava.mpi.ops.parallel;

import cz.it4i.scijava.mpi.MPIUtils;
import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.mpi.ReduceOp;
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
