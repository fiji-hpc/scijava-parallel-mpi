package com.mycompany.imagej.ops.stats;

import net.imagej.ops.Ops;
import net.imagej.ops.special.chain.RTs;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imagej.ops.stats.AbstractStatsOp;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

@Plugin(type = Ops.Stats.Mean.class, label = "Statistics: Mean",
        priority = Priority.VERY_HIGH + 1)
public class MPIMean<I extends RealType<I>, O extends RealType<O>> extends
        AbstractStatsOp<IterableInterval<I>, O> implements Ops.Stats.Mean
{
    private UnaryFunctionOp<Iterable<I>, O> sumFunc;

    @Override
    public void initialize() {
        sumFunc = RTs.function(ops(), Ops.Stats.Sum.class, in());
    }

    @Override
    public void compute(final IterableInterval<I> input, final O output) {
        double sum = sumFunc.calculate(input).getRealDouble();
        output.setReal(sum / Intervals.numElements(input));
    }
}
