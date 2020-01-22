package com.mycompany.imagej.ops.filter.convolve;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.Utils;
import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IterableRandomAccessibleInterval;
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
        Utils.print(my_block);

        int offset = 30;
        int value = offset + (255 - offset) / MPIUtils.getSize() * MPIUtils.getRank();
        for(O b: new IterableRandomAccessibleInterval<O>(my_block)) {
            b.setReal(value);
        }

        Utils.gather(output, blocks);
    }
}
