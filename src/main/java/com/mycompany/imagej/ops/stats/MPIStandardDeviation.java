package com.mycompany.imagej.ops.stats;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.chunk.Chunk;
import com.mycompany.imagej.mpi.ReduceOp;
import net.imagej.ops.Ops;
import net.imagej.ops.stats.AbstractStatsOp;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.ValuePair;
import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Plugin(type = Ops.Stats.StdDev.class, label = "Statistics: Standard Deviation",
        priority = Priority.VERY_HIGH + 1, attrs = {@Attr(name = "MPI", value="true")})
public class MPIStandardDeviation<I extends RealType<I>, O extends RealType<O>>
        extends AbstractStatsOp<IterableInterval<I>, O> implements Ops.Stats.StdDev
{
    @Parameter
    protected ThreadService threadService;

    @Override
    public void compute(final IterableInterval<I> input, final O output) {
        Chunk<I> chunks = new Chunk<>(input, MPIUtils.getSize());
        int threads = Runtime.getRuntime().availableProcessors();
        ArrayList<Future<ValuePair<Double, Double>>> futures = new ArrayList<>(Runtime.getRuntime().availableProcessors());

        Chunk<I> nodeChunk = chunks.getChunk(MPIUtils.getRank()).split(threads);
        for(int i = 0; i < threads; i++) {
            final int threadNum = i;
            futures.add(this.threadService.run(() -> {
                double localSum = 0;
                double localSumSq = 0;
                for (final I in : nodeChunk.getChunk(threadNum)) {
                    final double px = in.getRealDouble();
                    localSum += px;
                    localSumSq += px * px;
                }

                return new ValuePair<>(localSum, localSumSq);
            }));
        }

        // thread reduce
        double nodeSum = 0;
        double nodeSumSq = 0;
        for(Future<ValuePair<Double, Double>> future: futures) {
            try {
                ValuePair<Double, Double> result = future.get();
                nodeSum += result.getA();
                nodeSumSq += result.getB();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // mpi reduce
        Double sum = (Double) MPIUtils.Allreduce(nodeSum, ReduceOp.SUM);
        Double sumSq = (Double) MPIUtils.Allreduce(nodeSumSq, ReduceOp.SUM);

        long n = Intervals.numElements(input);
        output.setReal(Math.sqrt((sumSq - (sum * sum / n)) / (n - 1)));
    }

}
