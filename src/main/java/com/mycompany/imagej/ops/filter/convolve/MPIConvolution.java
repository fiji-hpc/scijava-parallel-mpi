package com.mycompany.imagej.ops.filter.convolve;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.Utils;
import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.convolution.kernel.SeparableKernelConvolution;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type = Ops.Filter.Convolve.class, priority = Priority.HIGH + 2)
public class MPIConvolution<I extends RealType<I>, K extends RealType<K>, O extends RealType<O>>
        extends
        AbstractUnaryComputerOp<RandomAccessible<I>, RandomAccessibleInterval<O>>
        implements Ops.Filter.Convolve, Contingent
{
    @Parameter
    private RandomAccessibleInterval<K> kernel;

    @Override
    public boolean conforms() {
        return true;
    }

    @Override
    public void compute(RandomAccessible<I> input, RandomAccessibleInterval<O> output) {
        List<RandomAccessibleInterval<O>> blocks = Utils.splitAll(output);

        RandomAccessibleInterval<O> my_block = blocks.get(MPIUtils.getRank());
        SeparableKernelConvolution.convolution(createKernel(kernel))
                .process(input, my_block);

        Utils.gather(output, blocks);
    }

     private static <K extends RealType<K>> Kernel1D createKernel(RandomAccessibleInterval<K> kernel) {
          RandomAccess<K> it = kernel.randomAccess();
          double[] kernel_values = new double[(int) Intervals.numElements(kernel)];
          int i = 0;
          for(long r = kernel.min(1); r <= kernel.max(1); r++) {
              for(long c = kernel.min(0); c <= kernel.max(0); c++) {
                  kernel_values[i++] = it.get().getRealDouble();
                  it.fwd(0);
              }
              it.setPosition(new long[]{0L, kernel.min(0)});
              it.fwd(1);
          }

          return Kernel1D.centralAsymmetric(kernel_values);
      }
}
