package cz.it4i.scijava.mpi.ops.math;

import com.sun.jna.Memory;
import cz.it4i.scijava.mpi.Native;
import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.ops.parallel.Parallel;
import net.imagej.DefaultDataset;
import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCFI;
import net.imglib2.IterableInterval;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

import static cz.it4i.scijava.mpi.Measure.measureCatch;

@Plugin(type = Ops.Math.Add.class, priority = 1000)
public class NativeAdd<T extends NumericType<T>> extends
        AbstractUnaryComputerOp<DefaultDataset, DefaultDataset>
        implements Contingent, Ops.Math.Add
{
    @Parameter
    private UnsignedByteType value;

    @Override
    public void compute(final DefaultDataset input,
                        final DefaultDataset output)
    {
        long elements = Intervals.numElements(input);
        Memory outputMem = measureCatch("alloc_outputmem", () -> new Memory(elements));
        Memory inputMem = measureCatch("alloc_inputmem", () -> new Memory(elements));
        measureCatch("copyToNative", () -> {
            Native.copyToNative((PlanarImg) ((DefaultDataset) input).getImgPlus().getImg(), inputMem);
        });
        measureCatch("native_minfilter", () -> {
            Native.NativeLib.INSTANCE.add(outputMem, inputMem, Intervals.numElements(elements), value.getByte());
        });
        measureCatch("copyToNative", () -> {
            Native.copyFromNative(castToPlanar(output), outputMem);
        });
    }

    private PlanarImg castToPlanar(IterableInterval output) {
        if(output instanceof DefaultDataset) {
            return (PlanarImg) ((DefaultDataset) output).getImgPlus().getImg();
        }
        return (PlanarImg) output;
    }

    @Override
    public boolean conforms() {
        return System.getenv("B_USE_NATIVE") != null;
    }
}