package com.mycompany.imagej.ops;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.Utils;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IterableRandomAccessibleInterval;
import org.scijava.plugin.Plugin;

import java.util.List;

import static com.mycompany.imagej.Measure.measureCatch;

@Plugin(type = MPIRankColor.class)
public class DefaultMPIRankColor<I extends RealType<I>, O extends RealType<O>>
        extends
        AbstractUnaryComputerOp<RandomAccessible<I>, RandomAccessibleInterval<O>>
        implements MPIRankColor
{
    @Override
    public void compute(RandomAccessible<I> input, RandomAccessibleInterval<O> output) {
        List<RandomAccessibleInterval<O>> blocks = Utils.splitAll(output);
        RandomAccessibleInterval<O> my_block = blocks.get(MPIUtils.getRank());
        Utils.print(my_block);

        measureCatch("coloring", () -> {
            int offset = 30;
            int value = offset + (255 - offset) / MPIUtils.getSize() * MPIUtils.getRank();
            for (O b : new IterableRandomAccessibleInterval<O>(my_block)) {
                b.setReal(value);
            }
        });

        Utils.gather(output, blocks);
    }
}
