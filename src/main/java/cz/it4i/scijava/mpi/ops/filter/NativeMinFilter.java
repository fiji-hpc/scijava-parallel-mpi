package cz.it4i.scijava.mpi.ops.filter;

import com.sun.jna.Memory;
import cz.it4i.scijava.mpi.Native;
import net.imagej.DefaultDataset;
import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.util.Intervals;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Ops.Filter.Min.class, priority = Priority.EXTREMELY_HIGH)
public class NativeMinFilter<I, O> extends
        AbstractUnaryComputerOp<DefaultDataset, IterableInterval<O>>
    implements Ops.Filter.Min, Contingent {

    @Parameter
    private RectangleShape shape;

    @Parameter(required = false)
    private OutOfBoundsFactory<I, RandomAccessibleInterval<I>> outOfBoundsFactory =
            new OutOfBoundsBorderFactory<>();

    @Override
    public void compute(DefaultDataset input, IterableInterval<O> output) {
        long elements = Intervals.numElements(input);
        Memory outputMem = new Memory(elements);
        Memory inputMem = new Memory(elements);

        Native.copyToNative((PlanarImg) input.getImgPlus().getImg(), inputMem);
        Native.NativeLib.INSTANCE.minfilter(outputMem, inputMem, Intervals.dimensionsAsLongArray(input), input.numDimensions(), shape.getSpan());
        Native.copyFromNative((PlanarImg) output, outputMem);
    }

    @Override
    public boolean conforms() {
        return System.getenv("B_USE_NATIVE") != null;
    }
}
