package cz.it4i.scijava.mpi.ops.edgeDetector;


import cz.it4i.scijava.mpi.Measure;
import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.ops.parallel.Parallel;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.IterableRandomAccessibleInterval;
import net.imglib2.view.Views;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.function.Consumer;

import static cz.it4i.scijava.mpi.Measure.measureCatch;

@Plugin(type = EdgeDetector.class, attrs = {@Attr(name = "MPI", value="true")})
public class CannyEdgeDetector<I extends RealType<I>, O extends RealType<O>>
        extends
        AbstractUnaryFunctionOp<RandomAccessibleInterval<I>, RandomAccessibleInterval<O>>
        implements EdgeDetector {

    @Parameter(required = false)
    double lowThreshold = 0.05;

    @Parameter(required = false)
    double highThreshold = 0.15;

    private RandomAccessibleInterval<FloatType> sobelX;
    private RandomAccessibleInterval<FloatType> sobelY;

    private Img<FloatType> visited;

    @Override
    public void initialize() {
        sobelX = ops().create().kernel(new double[][]{
                new double[]{-1, 0, +1},
                new double[]{-2, 0, +2},
                new double[]{-1, 0, +1}
        }, new FloatType());

        sobelY = ops().create().kernel(new double[][]{
                new double[]{1, 2, 1},
                new double[]{0, 0, 0},
                new double[]{-1, -2, -1}
        }, new FloatType());
    }

    private float at(RandomAccess<FloatType> ga, long x, long y) {
        ga.setPosition(x, 0);
        ga.setPosition(y, 1);
        return ga.get().getRealFloat();
    }


    @Override
    public RandomAccessibleInterval<O> calculate(RandomAccessibleInterval<I> withoutNoise) {
        // 4. gradienty
        RandomAccessibleInterval<FloatType> G = ops().create().img(withoutNoise, new FloatType());
        RandomAccessibleInterval<FloatType> fi = ops().create().img(withoutNoise, new FloatType());
        gradients(withoutNoise, G, fi);
        withoutNoise = null;

        // 5. non maximum supression
        Img<FloatType> edges = ops().create().img(G);
        long start = Measure.start();
        nonMaximumSupression(G, fi, edges);
        Measure.end("nonmaximum_supression", start);
        fi = null;

        return measureCatch("edge_tracking", () -> edgeTrackingHysteresis(G, edges));
    }

    private RandomAccessibleInterval edgeTrackingHysteresis(RandomAccessibleInterval<FloatType> g, Img<FloatType> edges) {
        Img<FloatType> edges2 = ops().create().img(g);
        Pair<FloatType, FloatType> minmax = ops().stats().minMax(edges);

        ops().image().normalize(edges2, edges, minmax.getA(), minmax.getB(), new FloatType(0), new FloatType(1.0f));

        visited = ops().create().img(g);
        Cursor<FloatType> cur = edges2.localizingCursor();
        cur.reset();
        cur.next();
        long[] position = new long[g.numDimensions()];
        while(cur.hasNext()) {
            double val = cur.get().get();
            if(val < lowThreshold) {
                cur.get().set(0);
            } else if(val > highThreshold) {
                cur.localize(position);
                walk(position, edges2);
                cur.get().set(1);
            }
            cur.fwd();
        }

        cur.reset();
        cur.next();
        while(cur.hasNext()) {
            if(cur.get().get() < highThreshold) {
                cur.get().set(0);
            }
            cur.fwd();
        }
        return edges2;
    }

    private void nonMaximumSupression(RandomAccessibleInterval<FloatType> g, RandomAccessibleInterval<FloatType> fi, Img<FloatType> edges) {
        ops().run(Parallel.class, edges, (Consumer<Chunk<FloatType>>) chunk -> {
            long[] position = new long[g.numDimensions()];
            Cursor<FloatType> cur = chunk.localizingCursor();
            cur.fwd();
            cur.localize(position);

            RandomAccess<FloatType> ga = Views.extendValue(g, new FloatType(0)).randomAccess();

            long[] dims = Intervals.dimensionsAsLongArray(g);
            long offset = IntervalIndexer.positionToIndex(position, dims) + 1;
            Cursor<FloatType> fia = new IterableRandomAccessibleInterval<FloatType>(fi).cursor();
            fia.jumpFwd(offset);

            while(cur.hasNext()) {
                cur.localize(position);
                long x = position[0];
                long y = position[1];
                ga.setPosition(position);

                float deg = fia.get().get();
                float alfa = (float) Math.atan(deg);
                alfa = Math.max(0.0f, Math.min(1.0f, alfa));

                float Ep, Em;
                float c = ga.get().get();
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

    private void gradients(RandomAccessibleInterval withoutNoise, RandomAccessibleInterval<FloatType> g, RandomAccessibleInterval<FloatType> fi) {
        RandomAccessibleInterval Gx = ops().create().img(withoutNoise);
        RandomAccessibleInterval Gy = ops().create().img(withoutNoise);

        measureCatch("sobel x", () -> ops().filter().convolve(Gx, withoutNoise, sobelX));
        measureCatch("sobel y", () -> ops().filter().convolve(Gy, withoutNoise, sobelY));

        measureCatch("gradient", () -> {
            ops().run(Parallel.class, g, (Consumer<Chunk<FloatType>>) chunk -> {
                long[] position = new long[g.numDimensions()];
                Cursor<FloatType> cursorG = chunk.localizingCursor();
                cursorG.fwd();
                cursorG.localize(position);

                long[] dims = Intervals.dimensionsAsLongArray(g);
                long offset = IntervalIndexer.positionToIndex(position, dims) + 1;

                Cursor<FloatType> Gxa = new IterableRandomAccessibleInterval<FloatType>(Gx).cursor();
                Gxa.jumpFwd(offset);
                Cursor<FloatType> Gya = new IterableRandomAccessibleInterval<FloatType>(Gy).cursor();
                Gya.jumpFwd(offset);
                Cursor<FloatType> fia = new IterableRandomAccessibleInterval<FloatType>(fi).cursor();
                fia.jumpFwd(offset);

                for(FloatType px: chunk) {
                    float localG = (float) Math.sqrt(
                            Gxa.get().get() * Gxa.get().get() +
                                    Gya.get().get() * Gya.get().get()
                    );
                    float localfi = (float) Math.atan2(Gya.get().get(), Gxa.get().get());
                    px.set(localG);
                    fia.get().set(localfi < 0 ? localfi + (float) Math.PI : localfi);

                    Gxa.next();
                    Gya.next();
                    fia.next();
                }
            });
        });
    }

    private void walk(long[] posit, RandomAccessibleInterval<FloatType> img) {
        long[] position = new long[img.numDimensions()];
        position[0] = posit[0];
        position[1] = posit[1];

        RandomAccess<FloatType> r = visited.randomAccess();
        r.setPosition(position);
        if(r.get().get() == 1) {
            return;
        }
        r.get().set(1);

        RandomAccess<FloatType> ra = img.randomAccess();
        ra.setPosition(position);

        double val = ra.get().get();
        if(val >= 1) {
            return;
        }

        if(val > lowThreshold) {
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