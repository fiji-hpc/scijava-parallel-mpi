package com.mycompany.imagej.ops;

import com.mycompany.imagej.Chunk;
import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.RandomAccessibleIntervalGatherer;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.plugin.Plugin;

import static com.mycompany.imagej.Measure.measureCatch;

@Plugin(type = MPIRankColor.class)
public class DefaultMPIRankColor<I extends RealType<I>, O extends RealType<O>>
        extends
        AbstractUnaryComputerOp<RandomAccessible<I>, RandomAccessibleInterval<O>>
        implements MPIRankColor
{
    @Override
    public void compute(RandomAccessible<I> input, RandomAccessibleInterval<O> output) {
        Chunk<O> chunks = new Chunk<>(Views.flatIterable(output), MPIUtils.getSize());

        measureCatch("coloring", () -> {
            double max = output.randomAccess().get().getMaxValue();
            double offset = max * 0.1;
            double value = offset + (max - offset) / MPIUtils.getSize() * MPIUtils.getRank();
            for (Cursor<O> it = chunks.getChunk(MPIUtils.getRank()).cursor(); it.hasNext(); ) {
                O b = it.next();
                b.setReal(value);
            }
        });

        RandomAccessibleIntervalGatherer.gather(chunks);

        /*
        List<RandomAccessibleInterval<O>> blocks = Utils.splitAll(output);
        RandomAccessibleInterval<O> my_block = blocks.get(MPIUtils.getRank());
        Utils.print(my_block);

        measureCatch("coloring", () -> {
            double max = output.randomAccess().get().getMaxValue();
            double offset = max * 0.1;
            double value = offset + (max - offset) / MPIUtils.getSize() * MPIUtils.getRank();
            for (O b : new IterableRandomAccessibleInterval<O>(my_block)) {
                b.setReal(value);
            }
        });

        Utils.gather(output, blocks);*/
    }
}
