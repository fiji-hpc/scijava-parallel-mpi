package cz.it4i.scijava.mpi.ops;

import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.ops.parallel.Parallel;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

@Plugin(type = BenchmarkTransferOp.class, attrs = {@Attr(name = "MPI", value="true")})
public class DefaultBenchmarkTransfer<I extends RealType<I>, O extends RealType<O>>
        extends
        AbstractUnaryFunctionOp<RandomAccessibleInterval<I>, RandomAccessibleInterval<O>>
        implements BenchmarkTransferOp
{
    @Override
    public RandomAccessibleInterval<O> calculate(RandomAccessibleInterval<I> input) {
        return (RandomAccessibleInterval<O>) this.ops().run(Parallel.class, input, (Consumer<Chunk<O>>) chunk -> {
            // NOOP, just benchmark transfer time
        });
    }
}
