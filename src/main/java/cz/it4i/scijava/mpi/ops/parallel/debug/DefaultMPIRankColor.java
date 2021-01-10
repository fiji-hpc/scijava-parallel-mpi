package cz.it4i.scijava.mpi.ops.parallel.debug;

import cz.it4i.scijava.mpi.MPIUtils;
import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.ops.parallel.Parallel;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.view.IterableRandomAccessibleInterval;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

@Plugin(type = MPIRankColor.class, attrs = {@Attr(name = "MPI", value="true")})
public class DefaultMPIRankColor<I extends RealType<I>, O extends RealType<O>>
        extends
        AbstractUnaryComputerOp<RandomAccessibleInterval<I>, RandomAccessibleInterval<O>>
        implements MPIRankColor
{
    @Override
    public void compute(RandomAccessibleInterval<I> input, RandomAccessibleInterval<O> output) {
        this.ops().run(Parallel.class, output, (Consumer<Chunk<O>>) chunk -> {
            Cursor<O> cursor = chunk.cursor();
            cursor.fwd();

            long[] pos = new long[input.numDimensions()];
            cursor.localize(pos);

            Cursor<I> inCursor = new IterableRandomAccessibleInterval<I>(input).cursor();
            long[] dims = Intervals.dimensionsAsLongArray(input);
            inCursor.jumpFwd(IntervalIndexer.positionToIndex(pos, dims) + 1);

            double x = 0.1 + 0.9 * MPIUtils.getRank() / MPIUtils.getSize();

            while(cursor.hasNext()) {
                cursor.get().setReal(inCursor.get().getRealFloat() * x);
                inCursor.fwd();
                cursor.fwd();
            }
        });
    }
}
