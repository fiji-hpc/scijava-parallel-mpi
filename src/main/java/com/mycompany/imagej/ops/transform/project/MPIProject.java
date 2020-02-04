package com.mycompany.imagej.ops.transform.project;

import com.mycompany.imagej.Chunk;
import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.RandomAccessibleIntervalGatherer;
import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imagej.ops.Parallel;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Iterator;

import static com.mycompany.imagej.Measure.measureCatch;

@Plugin(type = Ops.Transform.Project.class, //
	priority = 0)
public class MPIProject<T, V> extends
	AbstractUnaryComputerOp<RandomAccessibleInterval<T>, IterableInterval<V>>
	implements Contingent, Parallel, Ops.Transform.Project
{

	@Parameter
	private UnaryComputerOp<Iterable<T>, V> method;

	// dimension which will be projected
	@Parameter
	private int dim;

	@Override
	public void compute(final RandomAccessibleInterval<T> input,
		final IterableInterval<V> output)
	{
		Chunk<V> chunks = new Chunk<>(output, MPIUtils.getSize());

		Cursor<V> cursor = chunks.getChunk(MPIUtils.getRank()).localizingCursor();
		RandomAccess<T> access = input.randomAccess();

		while (cursor.hasNext()) {
			cursor.fwd();
			for (int d = 0; d < input.numDimensions(); d++) {
				if (d != dim) {
					access.setPosition(cursor.getIntPosition(d - (d > dim ? 1 : 0)), d);
				}
			}

			method.compute(new DimensionIterable(input.dimension(dim), access), cursor.get());
		}

		measureCatch("barrier", MPIUtils::barrier);
		RandomAccessibleIntervalGatherer.gather((Chunk) chunks);
	}

	@Override
	public boolean conforms() {
		return in().numDimensions() == out().numDimensions() + 1 && in().numDimensions() > dim;
	}

	final class DimensionIterable implements Iterable<T> {

		private final long size;
		private final RandomAccess<T> access;

		public DimensionIterable(final long size, final RandomAccess<T> access) {
			this.size = size;
			this.access = access;
		}

		@Override
		public Iterator<T> iterator() {
			return new Iterator<T>() {

				int k = -1;

				@Override
				public boolean hasNext() {
					return k < size - 1;
				}

				@Override
				public T next() {
					k++;
					access.setPosition(k, dim);
					return access.get();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException("Not supported");
				}
			};
		}
	}
}