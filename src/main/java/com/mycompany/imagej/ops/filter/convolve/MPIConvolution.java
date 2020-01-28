package com.mycompany.imagej.ops.filter.convolve;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.Utils;
import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.AbstractBinaryComputerOp;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.convolution.Convolution;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.convolution.kernel.SeparableKernelConvolution;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IterableRandomAccessibleInterval;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;
import java.util.concurrent.Executors;

import static com.mycompany.imagej.Measure.measureCatch;

@Plugin(type = Ops.Filter.Convolve.class, priority = Priority.HIGH + 2)
public class MPIConvolution<I extends RealType<I>, K extends RealType<K>, O extends RealType<O>>
        extends
        AbstractBinaryComputerOp<RandomAccessibleInterval<I>, RandomAccessibleInterval<K>, RandomAccessibleInterval<O>>
        implements Ops.Filter.Convolve
{
    @Override
    public void compute(RandomAccessibleInterval<I> in, RandomAccessibleInterval<K> kernel, RandomAccessibleInterval<O> out) {
        List<RandomAccessibleInterval<O>> blocks = Utils.splitAll(out);

        RandomAccessibleInterval<O> my_block = blocks.get(MPIUtils.getRank());
        Utils.print(my_block);
        measureCatch("convolution", () -> {
            Convolution<NumericType<?>> convolution = SeparableKernelConvolution.convolution(createKernel(kernel));
            if(System.getenv("B_SINGLE_THREAD") != null) {
                convolution.setExecutor(Executors.newSingleThreadExecutor());
            }
            convolution.process(in, my_block);
        });

    //    Utils.gather(output, blocks);
    }

     private static <K extends RealType<K>> Kernel1D createKernel(RandomAccessibleInterval<K> kernel) {
         double[] kernel_values = new double[(int) Intervals.numElements(kernel)];

         int i = 0;
         for(K value: new IterableRandomAccessibleInterval<>(kernel)) {
             kernel_values[i++] = value.getRealDouble();
         }

          return Kernel1D.centralAsymmetric(kernel_values);
      }
}
