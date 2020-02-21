package com.mycompany.imagej.ops.stats;

import com.mycompany.imagej.chunk.Chunk;
import com.mycompany.imagej.mpi.ReduceOp;
import com.mycompany.imagej.ops.parallel.Reduce;
import net.imagej.ops.Ops;
import net.imagej.ops.special.chain.RTs;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imagej.ops.stats.AbstractStatsOp;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Function;


@Plugin(type = Ops.Stats.Moment1AboutMean.class, label = "Statistics: Moment1AboutMean", attrs = {@Attr(name = "MPI", value="true")})
public class MPIMoment1AboutMean<I extends RealType<I>, O extends RealType<O>>
        extends AbstractStatsOp<Iterable<I>, O> implements Ops.Stats.Moment1AboutMean
{

    private UnaryFunctionOp<Iterable<I>, O> meanFunc;
    private UnaryFunctionOp<Iterable<I>, O> sizeFunc;

    @Override
    public void initialize() {
        meanFunc = RTs.function(ops(), Ops.Stats.Mean.class, in());
        sizeFunc = RTs.function(ops(), Ops.Stats.Size.class, in());
    }

    @Override
    public void compute(final Iterable<I> input, final O output) {
        final double mean = meanFunc.calculate(input).getRealDouble();
        final double size = sizeFunc.calculate(input).getRealDouble();
        Double result = (Double) ops().run(Reduce.class, input, (Function<Chunk<I>, Double>) chunk -> {
            double res = 0.0;
            for (final I in : chunk) {
                res += in.getRealDouble() - mean;
            }
            return res;
        }, ReduceOp.SUM);

        output.setReal(result / size);
    }
}
