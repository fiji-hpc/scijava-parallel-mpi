package cz.it4i.scijava.mpi.ops.stats;

import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.mpi.ReduceOp;
import cz.it4i.scijava.mpi.ops.parallel.Reduce;
import net.imagej.ops.Ops;
import net.imagej.ops.stats.AbstractStatsOp;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Function;

@Plugin(type = Ops.Stats.Max.class, label = "Statistics: Max",
        priority = Priority.VERY_HIGH + 1, attrs = {@Attr(name = "MPI", value="true")})
public class MPIMax<T extends RealType<T>> extends
        AbstractStatsOp<IterableInterval<T>, T> implements Ops.Stats.Max
{

    @Override
    public void compute(final IterableInterval<T> input, final T output) {
        T min = (T) ops().run(Reduce.class, input, (Function<Chunk<T>, T>) chunk -> {
            T localMin = createOutput(input);
            localMin.setReal(output.getMinValue());
            for (final T in : chunk) {
                if (localMin.compareTo(in) < 0) {
                    localMin.set(in);
                }
            }
            return localMin;
        }, ReduceOp.MAX);

        output.set(min);
    }

    @Override
    public T createOutput(IterableInterval<T> input) {
        return input.iterator().next().createVariable();
    }
}
