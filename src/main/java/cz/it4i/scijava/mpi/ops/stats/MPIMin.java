package cz.it4i.scijava.mpi.ops.stats;


import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.mpi.ReduceOp;
import cz.it4i.scijava.mpi.ops.parallel.Reduce;
import net.imagej.ops.Ops;
import net.imagej.ops.stats.AbstractStatsOp;
import net.imglib2.type.numeric.RealType;
import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Function;

@Plugin(type = Ops.Stats.Min.class, label = "Statistics: Min",
	priority = Priority.VERY_HIGH + 1, attrs = {@Attr(name = "MPI", value="true")})
public class MPIMin<T extends RealType<T>> extends
		AbstractStatsOp<Iterable<T>, T> implements Ops.Stats.Min
{

	@Override
	public void compute(final Iterable<T> input, final T output) {
		T min = (T) ops().run(Reduce.class, input, (Function<Chunk<T>, T>) chunk -> {
			T localMin = createOutput(input);
			localMin.setReal(output.getMaxValue());
			for (final T in : chunk) {
				if (localMin.compareTo(in) > 0) {
					localMin.set(in);
				}
			}
			return localMin;
		}, ReduceOp.MIN);

		output.set(min);
	}

	@Override
	public T createOutput(Iterable<T> input) {
		return input.iterator().next().createVariable();
	}
}