package com.mycompany.imagej.ops.parallel;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.chunk.Chunk;
import net.imagej.ops.special.computer.AbstractNullaryComputerOp;
import net.imglib2.IterableInterval;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

@Plugin(type = Parallel.class, priority = 2.0, attrs = {@Attr(name = "MPI", value="true")})
public class MPIParallel<O> extends AbstractNullaryComputerOp<IterableInterval<O>> implements Parallel {
    @Parameter
    private Consumer<Chunk<O>> action;

    @Override
    public void compute(IterableInterval<O> block) {
        Chunk<O> chunks = new Chunk<>(block, MPIUtils.getSize());

        this.ops().run(
                ThreadedParallel.class,
                chunks.getChunk(MPIUtils.getRank()),
                action
        );

        chunks.sync();
    }
}
