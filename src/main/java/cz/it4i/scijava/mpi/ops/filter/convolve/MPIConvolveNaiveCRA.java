package cz.it4i.scijava.mpi.ops.filter.convolve;


import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.AbstractBinaryComputerOp;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

@Plugin(type = Ops.Filter.Convolve.class, priority = 1000.0D, attrs = {@Attr(name = "MPI", value="true")})
public class MPIConvolveNaiveCRA<I extends RealType<I>, O extends RealType<O> & NativeType<O>, K extends RealType<K>, C extends ComplexType<C> & NativeType<C>> extends AbstractBinaryComputerOp<RandomAccessible<I>, RandomAccessibleInterval<K>, RandomAccessibleInterval<O>>
        implements Ops.Filter.Convolve {
    public void compute(RandomAccessible<I> input, RandomAccessibleInterval<K> kernel, RandomAccessibleInterval<O> output) {
        this.ops().run(
                Ops.Filter.Convolve.class,
                output,
                Views.interval(input, output),
                kernel
        );
    }
}
