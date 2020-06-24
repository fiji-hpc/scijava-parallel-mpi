package cz.it4i.scijava.mpi.ops.edgeDetector;


import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.ops.MPIRankColor;
import cz.it4i.scijava.mpi.ops.parallel.Parallel;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.IterableRandomAccessibleInterval;
import net.imglib2.view.Views;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

import static cz.it4i.scijava.mpi.Measure.measureCatch;

@Plugin(type = MPIRankColor.class, attrs = {@Attr(name = "MPI", value="true")})
public class CannyEdgeDetector<I extends RealType<I>, O extends RealType<O>>
        extends
        AbstractUnaryFunctionOp<RandomAccessibleInterval<I>, RandomAccessibleInterval<O>>
        implements EdgeDetector {

    double low = 0.05;
    double high = 0.15;

    private RandomAccessibleInterval<DoubleType> sobelX;
    private RandomAccessibleInterval<DoubleType> sobelY;

    private Img<DoubleType> visited;

    @Override
    public void initialize() {
        sobelX = ops().create().kernel(new double[][]{
                new double[]{-1, 0, +1},
                new double[]{-2, 0, +2},
                new double[]{-1, 0, +1}
        }, new DoubleType());

        sobelY = ops().create().kernel(new double[][]{
                new double[]{1, 2, 1},
                new double[]{0, 0, 0},
                new double[]{-1, -2, -1}
        }, new DoubleType());
    }

    private double at(RandomAccess<DoubleType> ga, long x, long y) {
        ga.setPosition(x, 0);
        ga.setPosition(y, 1);
        return ga.get().getRealDouble();
    }


    @Override
    public RandomAccessibleInterval<O> calculate(RandomAccessibleInterval<I> withoutNoise) {
        // 4. gradienty
        RandomAccessibleInterval<DoubleType> G = ops().create().img(withoutNoise);
        RandomAccessibleInterval<DoubleType> fi = ops().create().img(withoutNoise);
        gradients(withoutNoise, G, fi);
        withoutNoise = null;

        // 5. non maximum supression
        Img<DoubleType> edges = ops().create().img(G);
        nonMaximumSupression(G, fi, edges);

        return edgeTrackingHysteresis(G, edges);
    }

    private RandomAccessibleInterval edgeTrackingHysteresis(RandomAccessibleInterval<DoubleType> g, Img<DoubleType> edges) {
        Img<DoubleType> edges2 = ops().create().img(g);
        Pair<DoubleType, DoubleType> minmax = ops().stats().minMax(edges);

        ops().image().normalize(edges2, edges, minmax.getA(), minmax.getB(), new DoubleType(0), new DoubleType(1.0));

        visited = ops().create().img(g);
        Cursor<DoubleType> cur = edges2.localizingCursor();
        cur.reset();
        cur.next();
        long[] position = new long[3];
        while(cur.hasNext()) {
            double val = cur.get().get();
            if(val < low) {
                cur.get().set(0);
            } else if(val > high) {
                cur.localize(position);
                walk(position, edges2);
                cur.get().set(1);
            }
            cur.fwd();
        }

        cur.reset();
        cur.next();
        while(cur.hasNext()) {
            if(cur.get().get() < high) {
                cur.get().set(0);
            }
            cur.fwd();
        }
        return edges2;
    }

    private void nonMaximumSupression(RandomAccessibleInterval<DoubleType> g, RandomAccessibleInterval<DoubleType> fi, Img<DoubleType> edges) {
        ops().run(Parallel.class, edges, (Consumer<Chunk<DoubleType>>) chunk -> {
            long[] position = new long[g.numDimensions()];
            Cursor<DoubleType> cur = chunk.localizingCursor();
            cur.fwd();
            cur.localize(position);

            RandomAccess<DoubleType> ga = Views.extendValue(g, new DoubleType(0)).randomAccess();

            long[] dims = Intervals.dimensionsAsLongArray(g);
            long offset = IntervalIndexer.positionToIndex(position, dims) + 1;
            Cursor<DoubleType> fia = new IterableRandomAccessibleInterval<DoubleType>(fi).cursor();
            fia.jumpFwd(offset);

            while(cur.hasNext()) {
                cur.localize(position);
                long x = position[0];
                long y = position[1];
                ga.setPosition(position);

                double deg = fia.get().get();
                double alfa = Math.atan(deg);
                alfa = Math.max(0.0, Math.min(1.0, alfa));

                double Ep, Em;
                double c = ga.get().get();
                if(deg <= Math.PI / 4) {
                    Ep = at(ga, x + 1, y) * (alfa) + at(ga, x + 1, y - 1) * (1-alfa);
                    Em = at(ga, x - 1, y) * (alfa) + at(ga, x - 1, y + 1) * (1-alfa);
                } else if(deg <= 2 * Math.PI / 4) {
                    Ep = at(ga, x + 1, y - 1) * (1-alfa) + at(ga, x, y - 1) * (alfa);
                    Em = at(ga, x - 1, y + 1) * (1-alfa) + at(ga, x, y + 1) * (alfa);
                } else if(deg <= 3 * Math.PI / 4) {
                    Ep = at(ga, x, y - 1) * (1 - alfa) + at(ga, x-1, y - 1) * alfa;
                    Em = at(ga, x + 1, y + 1) * (1 - alfa) + at(ga, x+1, y + 1) * alfa;
                } else {
                    Ep = at(ga, x - 1, y - 1) * (alfa) + at(ga, x - 1, y) * (1 - alfa);
                    Em = at(ga, x + 1, y + 1) * (alfa) + at(ga, x + 1, y) * (1 - alfa);
                }

                if (Ep <= c && c > Em) {
                    cur.get().set(c);
                }
                fia.next();
                cur.next();
            }
        });
    }

    private void gradients(RandomAccessibleInterval withoutNoise, RandomAccessibleInterval<DoubleType> g, RandomAccessibleInterval<DoubleType> fi) {
        RandomAccessibleInterval Gx = ops().create().img(withoutNoise);
        RandomAccessibleInterval Gy = ops().create().img(withoutNoise);

        measureCatch("sobel x", () -> ops().filter().convolve(Gx, withoutNoise, sobelX));
        measureCatch("sobel y", () -> ops().filter().convolve(Gy, withoutNoise, sobelY));

        measureCatch("gradient", () -> {
            ops().run(Parallel.class, g, (Consumer<Chunk<DoubleType>>) chunk -> {
                long[] position = new long[g.numDimensions()];
                Cursor<DoubleType> cursorG = chunk.localizingCursor();
                cursorG.fwd();
                cursorG.localize(position);

                long[] dims = Intervals.dimensionsAsLongArray(g);
                long offset = IntervalIndexer.positionToIndex(position, dims) + 1;

                Cursor<DoubleType> Gxa = new IterableRandomAccessibleInterval<DoubleType>(Gx).cursor();
                Gxa.jumpFwd(offset);
                Cursor<DoubleType> Gya = new IterableRandomAccessibleInterval<DoubleType>(Gy).cursor();
                Gya.jumpFwd(offset);
                Cursor<DoubleType> fia = new IterableRandomAccessibleInterval<DoubleType>(fi).cursor();
                fia.jumpFwd(offset);

                for(DoubleType px: chunk) {
                    double localG = Math.sqrt(
                            Gxa.get().get() * Gxa.get().get() +
                                    Gya.get().get() * Gya.get().get()
                    );
                    double localfi = Math.atan2(Gya.get().get(), Gxa.get().get());
                    px.set(localG);
                    fia.get().set(localfi < 0 ? localfi + Math.PI : localfi);

                    Gxa.next();
                    Gya.next();
                    fia.next();
                }
            });
        });
    }

    private void walk(long[] posit, RandomAccessibleInterval<DoubleType> img) {
        long[] position = new long[img.numDimensions()];
        position[0] = posit[0];
        position[1] = posit[1];

        RandomAccess<DoubleType> r = visited.randomAccess();
        r.setPosition(position);
        if(r.get().get() == 1) {
            return;
        }
        r.get().set(1);

        RandomAccess<DoubleType> ra = img.randomAccess();
        ra.setPosition(position);

        double val = ra.get().get();
        if(val >= 1) {
            return;
        }

        if(val > low) {
            ra.get().set(1);
            for(int i = -1; i <= 1; i++) {
                for(int j = -1; j <= 1; j++) {
                    if(i == 0 && j == 0) {
                        continue;
                    }

                    position[0] = posit[0] + i;
                    position[1] = posit[1] + j;
                    if(position[0] < 0 || position[0] >= img.dimension(0) || position[1] < 0 || position[1] >= img.dimension(1)) {
                        continue;
                    }

                    walk(position, img);
                }
            }
        } else {
            ra.get().set(0);
        }
    }
}