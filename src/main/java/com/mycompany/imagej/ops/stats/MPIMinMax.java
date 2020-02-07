package com.mycompany.imagej.ops.stats;


import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.chunk.Chunk;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.stats.DefaultMinMax;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Plugin(type = Ops.Stats.MinMax.class, priority = 2, attrs = {@Attr(name = "MPI", value="true")})
public class MPIMinMax<I extends RealType<I>> extends AbstractUnaryFunctionOp<IterableInterval<I>, Pair<I, I>> implements Ops.Stats.MinMax {
    @Parameter
    public ThreadService threadService;

    @Override
    public Pair<I, I> calculate(final IterableInterval<I> input) {
        Chunk<I> chunks = new Chunk<>(input, MPIUtils.getSize());

        Pair<Double, Double> nodeExtremes = threadedMinMax(chunks.getChunk(MPIUtils.getRank()));

        double[] globalMin = {0.0f};
        double[] globalMax = {0.0f};
        double[] mins = {nodeExtremes.getA()};
        double[] maxs = {nodeExtremes.getB()};

        MPIUtils.Allreduce(
                mins,
                globalMin,
                1,
                MPIUtils.MPI_DOUBLE,
                MPIUtils.MPI_OP_MIN,
                MPIUtils.MPI_COMM_WORLD
        );

        MPIUtils.Allreduce(
                maxs,
                globalMax,
                1,
                MPIUtils.MPI_DOUBLE,
                MPIUtils.MPI_OP_MAX,
                MPIUtils.MPI_COMM_WORLD
        );

        final I min = input.cursor().get().createVariable();
        min.setReal(globalMin[0]);

        final I max = input.cursor().get().createVariable();
        max.setReal(globalMax[0]);

        return new ValuePair<>(min, max);
    }

    private Pair<Double, Double> threadedMinMax(Chunk<I> chunks) {
        final List<Future< Pair<I, I> >> futures = new ArrayList<>();

        for(final Chunk<I> chunk: chunks.allChunks()) {
            final Callable<Pair<I, I>> call = new Callable<Pair<I, I>>() {
                @Override
                public Pair<I, I> call() throws Exception {
                    return new DefaultMinMax<I>().calculate(chunk);
                }
            };

            futures.add(threadService.getExecutorService().submit(call));
        }

        double tmpMin = Double.POSITIVE_INFINITY;
        double tmpMax = Double.NEGATIVE_INFINITY;
        for (final Future<Pair<I, I>> future : futures) {
            try {
                Pair<I, I> result = future.get();

                if(result.getA().getRealDouble() < tmpMin) {
                    tmpMin = result.getA().getRealDouble();
                }

                if(result.getB().getRealDouble() > tmpMax) {
                    tmpMax = result.getB().getRealDouble();
                }
            }
            catch (final InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return new ValuePair<>(tmpMin, tmpMax);
    }
}

