package cz.it4i.scijava.mpi.ops.stats;


import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.mpi.ReduceOp;
import cz.it4i.scijava.mpi.ops.parallel.Reduce;
import net.imagej.ops.Ops;
import net.imagej.ops.special.chain.RTs;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imagej.ops.stats.AbstractStatsOp;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Function;

@Plugin(type = Ops.Stats.Variance.class, label = "Statistics: Variance", priority = Priority.VERY_HIGH + 1, attrs = {@Attr(name = "MPI", value="true")})
public class MPIVariance<I extends RealType<I>, O extends RealType<O>>
	extends AbstractStatsOp<IterableInterval<I>, O> implements Ops.Stats.Variance
{

	private UnaryFunctionOp<IterableInterval<I>, O> meanOp;

	@Override
	public void initialize() {
		meanOp = RTs.function(ops(), Ops.Stats.Mean.class, in());
	}

	@Override
	public void compute(final IterableInterval<I> input, final O output) {
		double mean = meanOp.calculate(input).getRealDouble();

		Double totalSum = (Double) ops().run(Reduce.class, input, (Function<Chunk<I>, Double>) chunk -> {
			double sum = 0;
			for (I in : chunk) {
				double x = in.getRealDouble();
				sum += (x - mean) * (x - mean);
			}
			return sum;
		}, ReduceOp.SUM);

		long n = Intervals.numElements(input);
		output.setReal(totalSum / (n - 1));
	}
}