package cz.it4i.scijava.mpi.ops.math;

import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.ops.parallel.Parallel;
import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.NumericType;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

public final class ConstantToIIOutputII {
    @Plugin(type = Ops.Math.Add.class, priority = 100, attrs = {@Attr(name = "MPI", value="true")})
    public static class Add<T extends NumericType<T>> extends
            net.imagej.ops.math.ConstantToIIOutputII.Add<T>
            implements Contingent, Ops.Math.Add
    {
        @Override
        public void compute(final IterableInterval<T> input,
                            final IterableInterval<T> output)
        {
            this.ops().run(Parallel.class, input, (Consumer<Chunk<T>>) chunk -> {
                super.compute(chunk, new Chunk<>(output, chunk.getOffset(), chunk.getLen()));
            });
        }

        @Override
        public void mutate(final IterableInterval<T> arg) {
            this.ops().run(Parallel.class, arg, (Consumer<Chunk<T>>) super::mutate);
        }
    }

    @Plugin(type = Ops.Math.Subtract.class, priority = 100, attrs = {@Attr(name = "MPI", value="true")})
    public static class Subtract<T extends NumericType<T>> extends
            net.imagej.ops.math.ConstantToIIOutputII.Subtract<T>
            implements Contingent, Ops.Math.Subtract
    {
        @Override
        public void compute(final IterableInterval<T> input,
                            final IterableInterval<T> output)
        {
            this.ops().run(Parallel.class, input, (Consumer<Chunk<T>>) chunk -> {
                super.compute(chunk, new Chunk<>(output, chunk.getOffset(), chunk.getLen()));
            });
        }

        @Override
        public void mutate(final IterableInterval<T> arg) {
            this.ops().run(Parallel.class, arg, (Consumer<Chunk<T>>) super::mutate);
        }
    }

    @Plugin(type = Ops.Math.Multiply.class, priority = 100, attrs = {@Attr(name = "MPI", value="true")})
    public static class Multiply<T extends NumericType<T>> extends
            net.imagej.ops.math.ConstantToIIOutputII.Multiply<T>
            implements Contingent, Ops.Math.Multiply
    {
        @Override
        public void compute(final IterableInterval<T> input,
                            final IterableInterval<T> output)
        {
            this.ops().run(Parallel.class, input, (Consumer<Chunk<T>>) chunk -> {
                super.compute(chunk, new Chunk<>(output, chunk.getOffset(), chunk.getLen()));
            });
        }

        @Override
        public void mutate(final IterableInterval<T> arg) {
            this.ops().run(Parallel.class, arg, (Consumer<Chunk<T>>) super::mutate);
        }
    }

    @Plugin(type = Ops.Math.Divide.class, priority = 100, attrs = {@Attr(name = "MPI", value="true")})
    public static class Divide<T extends NumericType<T>> extends
            net.imagej.ops.math.ConstantToIIOutputII.Divide<T>
            implements Contingent, Ops.Math.Divide
    {
        @Override
        public void compute(final IterableInterval<T> input,
                            final IterableInterval<T> output)
        {
            this.ops().run(Parallel.class, input, (Consumer<Chunk<T>>) chunk -> {
                super.compute(chunk, new Chunk<>(output, chunk.getOffset(), chunk.getLen()));
            });
        }

        @Override
        public void mutate(final IterableInterval<T> arg) {
            this.ops().run(Parallel.class, arg, (Consumer<Chunk<T>>) super::mutate);
        }
    }
}
