package cz.it4i.scijava.mpi.ops.stats;


import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.mpi.ReduceOp;
import cz.it4i.scijava.mpi.ops.parallel.Reduce;
import net.imagej.ops.Ops;
import net.imagej.ops.stats.AbstractStatsOp;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Function;


@Plugin(type = Ops.Stats.Sum.class, label = "Statistics: Sum", priority = 1, attrs = {@Attr(name = "MPI", value="true")})
public class MPISum<I extends RealType<I>, O extends RealType<O>> extends
        AbstractStatsOp<IterableInterval<I>, O> implements Ops.Stats.Sum
{
    @Override
    public void compute(IterableInterval<I> input, O output) {
        Double result = (Double) ops().run(Reduce.class, input, (Function<Chunk<I>, Double>) chunk -> {
            double res = 0.0;
            for (final I in : chunk) {
                res += in.getRealDouble();
            }
            return res;
        }, ReduceOp.SUM);
        output.setReal(result);
    }
}
