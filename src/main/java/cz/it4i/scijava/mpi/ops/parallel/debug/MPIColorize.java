package cz.it4i.scijava.mpi.ops.parallel.debug;

import cz.it4i.scijava.mpi.MPIUtils;
import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.ops.parallel.Parallel;
import net.imagej.ops.Contingent;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.view.IterableRandomAccessibleInterval;
import net.imglib2.view.Views;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

@Plugin(type = MPIRankColor.class, attrs = {@Attr(name = "MPI", value = "true")}, priority = 100)
public class MPIColorize<I extends UnsignedByteType, O extends UnsignedByteType>
            extends
            AbstractUnaryFunctionOp<RandomAccessibleInterval<I>, RandomAccessibleInterval<O>>
            implements MPIRankColor, Contingent {

    @Override
    public RandomAccessibleInterval<O> calculate(RandomAccessibleInterval<I> input) {
        Dimensions channelDim = Views.hyperSlice(input, 2, 0);
        RandomAccessibleInterval<O> R = (RandomAccessibleInterval<O>) ops().create().img(channelDim, new UnsignedByteType());
        RandomAccessibleInterval<O> G = (RandomAccessibleInterval<O>) ops().create().img(channelDim, new UnsignedByteType());
        RandomAccessibleInterval<O> B = (RandomAccessibleInterval<O>) ops().create().img(channelDim, new UnsignedByteType());

        int[] colors = {
                0x300000,
                0x003000,
                0x000030,
                0x303000,
                0x003030,
                0x300030,
        };

        ops().run(Parallel.class, R, (Consumer<Chunk<O>>) chunk -> {
            long[] dim = Intervals.dimensionsAsLongArray(R);
            long[] pos = new long[R.numDimensions()];

            Cursor<O> Rc = chunk.localizingCursor();
            Rc.fwd();
            Rc.localize(pos);

            Cursor<O> Gc = new IterableRandomAccessibleInterval<>(G).cursor();
            Cursor<O> Bc = new IterableRandomAccessibleInterval<>(B).cursor();
            long offset = IntervalIndexer.positionToIndex(pos, dim) + 1;
            Gc.jumpFwd(offset);
            Bc.jumpFwd(offset);

            Cursor<I> Rcin = Views.hyperSlice(input, 2, 0).cursor();
            Cursor<I> Gcin = Views.hyperSlice(input, 2, 1).cursor();
            Cursor<I> Bcin = Views.hyperSlice(input, 2, 2).cursor();
            Rcin.jumpFwd(offset);
            Gcin.jumpFwd(offset);
            Bcin.jumpFwd(offset);

            int color = colors[MPIUtils.getRank()];

            while (Rc.hasNext()) {
                Rc.get().set(Math.min(255, Rcin.get().get() + ((color & 0xFF0000) >> 16)));// + (color & 0xFF0000) >> 16);
                Gc.get().set(Math.min(255, Gcin.get().get() + ((color & 0xFF00) >> 8)));
                Bc.get().set(Math.min(255, Bcin.get().get() + (color & 0xFF)));

                Rcin.fwd();
                Gcin.fwd();
                Bcin.fwd();

                Rc.fwd();
                Gc.fwd();
                Bc.fwd();
            }
        });

        new Chunk<>(new IterableRandomAccessibleInterval<>(G), MPIUtils.getSize()).sync();
        new Chunk<>(new IterableRandomAccessibleInterval<>(B), MPIUtils.getSize()).sync();


        return Views.permute(Views.stack(R, G, B), 2, 3);
    }

    @Override
    public boolean conforms() {
        return in().numDimensions() >= 2 && in().dimension(2) == 3;
    }
}