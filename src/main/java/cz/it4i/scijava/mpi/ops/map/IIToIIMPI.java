package cz.it4i.scijava.mpi.ops.map;

import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.ops.parallel.Parallel;
import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imagej.ops.map.AbstractMapComputer;
import net.imagej.ops.map.Maps;
import net.imglib2.IterableInterval;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

@Plugin(type = Ops.Map.class, priority = 40, attrs = {@Attr(name = "MPI", value="true")})
public class IIToIIMPI<EI, EO> extends
    AbstractMapComputer<EI, EO, IterableInterval<EI>, IterableInterval<EO>>
        implements Contingent {
    @Override
    public boolean conforms() {
        return out() == null || Maps.compatible(in(), out());
    }

    @Override
    public void compute(final IterableInterval<EI> input,
                        final IterableInterval<EO> output)
    {
        this.ops().run(Parallel.class, output, (Consumer<Chunk<EO>>) chunk -> {
            Chunk<EI> in = new Chunk<>(input, chunk.getOffset(), chunk.getLen());
            Maps.map(
                in,
                chunk,
                getOp()
            );
        });
    }
}
