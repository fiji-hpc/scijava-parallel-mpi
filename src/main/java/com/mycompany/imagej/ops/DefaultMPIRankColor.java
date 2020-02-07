package com.mycompany.imagej.ops;

import com.mycompany.imagej.chunk.Chunk;
import com.mycompany.imagej.ops.parallel.Parallel;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

@Plugin(type = MPIRankColor.class, attrs = {@Attr(name = "MPI", value="true")})
public class DefaultMPIRankColor<I extends RealType<I>, O extends RealType<O>>
        extends
        AbstractUnaryComputerOp<RandomAccessible<I>, RandomAccessibleInterval<O>>
        implements MPIRankColor
{
    @Override
    public void compute(RandomAccessible<I> input, RandomAccessibleInterval<O> output) {
        double max = output.randomAccess().get().getMaxValue();
        double offset = max * 0.1;
        long totalSize = Intervals.numElements(output);

        this.ops().run(Parallel.class, output, (Consumer<Chunk<O>>) chunk -> {
            System.out.println(chunk);
            double value = offset + (max - offset) / totalSize * chunk.getOffset();

            Cursor<O> cursor = chunk.cursor();
            while(cursor.hasNext()) {
                cursor.fwd();
                cursor.get().setReal(value);
            }
        });
    }
}
