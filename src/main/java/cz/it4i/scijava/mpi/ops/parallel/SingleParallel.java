package cz.it4i.scijava.mpi.ops.parallel;

import cz.it4i.scijava.mpi.chunk.Chunk;
import net.imagej.ops.special.computer.AbstractNullaryComputerOp;
import net.imglib2.IterableInterval;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

@Plugin(type = Parallel.class, priority = 0.0)
public class SingleParallel<O> extends AbstractNullaryComputerOp<IterableInterval<O>> implements Parallel {
    @Parameter
    private Consumer<Chunk<O>> action;

    @Override
    public void compute(IterableInterval<O> block) {
        action.accept(new Chunk<>(block));
    }
}
