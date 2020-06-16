package cz.it4i.scijava.mpi;

import cz.it4i.scijava.mpi.chunk.Chunk;
import cz.it4i.scijava.mpi.ops.convert.RGB2Grayscale;
import cz.it4i.scijava.mpi.ops.parallel.Parallel;
import io.scif.SCIFIO;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.IterableRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.function.Consumer;

public class CannyDetector {
    static double low = 0.05;
    static double high = 0.15;

    static Img<DoubleType> visited;

    public static RandomAccessibleInterval<DoubleType> createKernel(OpService ops, double... values) {
        int n = (int) Math.sqrt(values.length);
        RandomAccessibleInterval<DoubleType> img = ops.create().img(new FinalDimensions(n, n), new DoubleType());
        RandomAccess<DoubleType> access = img.randomAccess();
        for(double value: values) {
            access.get().set(value);
            access.fwd(0);
        }
        return img;
    }

    public static double at(RandomAccess<DoubleType> ga, long x, long y) {
        ga.setPosition(y, 1);
        ga.setPosition(x, 0);
        return ga.get().get();
    }

    public static void main(String[] args) throws Exception {
        ImageJ ij = new ImageJ();

        OpService ops = ij.op();
        SCIFIO scifio = ij.scifio();
/*
        RandomAccessibleInterval imgRGB = scifio.datasetIO().open("/home/daniel/out.tif");
        Img fi = ops.create().img(imgRGB);
        ops.run(MPIRankColor.class, fi, imgRGB);
        ij.ui().show(fi);
        //ij.scifio().datasetIO().save(ij.dataset().create(imgRGB), "/tmp/a.tif");
 */

        // 1. load
        RandomAccessibleInterval imgRGB = scifio.datasetIO().open(args[0]);

        // 2. convert RGB to grayscale
        RandomAccessibleInterval img = (RandomAccessibleInterval) ops.run(RGB2Grayscale.class, imgRGB);
        imgRGB = null;
        img = ops.convert().float64((IterableInterval) img);

        // 3. gauss filter
        RandomAccessibleInterval withoutNoise = ops.create().img(img);
        ops.filter().convolve(withoutNoise, img, createKernel(ops,
                /*
                1.0/9, 1.0/9, 1.0/9,
                1.0/9, 1.0/9, 1.0/9,
                1.0/9, 1.0/9, 1.0/9
                 */

                1.0/16, 2.0/16, 1.0/16,
                2.0/16, 4.0/16, 2.0/16,
                1.0/16, 2.0/16, 1.0/16


                /*1/256.0, 4/256.0, 6/256.0, 4/256.0, 1/256.0,
                4/256.0, 14/256.0, 24/256.0, 14/256.0, 4/256.0,
                6/256.0, 24/256.0, 36/256.0, 24/256.0, 6/256.0,
                4/256.0, 14/256.0, 24/256.0, 14/256.0, 4/256.0,
                1/256.0, 4/256.0, 6/256.0, 4/256.0, 1/256.0

                 */
        ));
        img = null;

        // 4. gradienty
        RandomAccessibleInterval<DoubleType> G = ops.create().img(withoutNoise);
        RandomAccessibleInterval<DoubleType> fi = ops.create().img(withoutNoise);
        gradients(ij, withoutNoise, G, fi);
        withoutNoise = null;

        // 5. non maximum supression
        Img<DoubleType> edges = ops.create().img(G);
        //TODO: zesynchronizovat!!
        ops.run(Parallel.class, edges, (Consumer<Chunk<DoubleType>>) chunk -> {
            long[] position = new long[G.numDimensions()];
            Cursor<DoubleType> cur = chunk.localizingCursor();
            cur.fwd();
            cur.localize(position);

            RandomAccess<DoubleType> ga = Views.extendValue(G, new DoubleType(0)).randomAccess();

            long[] dims = Intervals.dimensionsAsLongArray(G);
            long offset = IntervalIndexer.positionToIndex(position, dims) + 1;
            Cursor<DoubleType> fia = new IterableRandomAccessibleInterval<DoubleType>(fi).cursor();
            fia.jumpFwd(offset);

            while(cur.hasNext()) {
                cur.localize(position);
                ga.setPosition(position);
                long x = position[0];
                long y = position[1];

                double deg = fia.get().get();
                if(deg < 0) {
                    deg += Math.PI;
                }

                double alfa = Math.tan(deg);
                alfa = Math.max(0, Math.min(1, alfa));

                double Ep = 0, Em = 0;
                double c = at(ga, x, y);
                Ep = Em = c + 1;

                int stupne = (int) (deg * 180 / Math.PI);
                if(stupne >= 0 && stupne <= 45) {
                    Ep = at(ga, x + 1, y) * (alfa) + at(ga, x + 1, y - 1) * (1-alfa);
                    Em = at(ga, x - 1, y) * (alfa) + at(ga, x - 1, y + 1) * (1-alfa);
                } else if(stupne <= 90) {
                    Ep = at(ga, x + 1, y - 1) * (1-alfa) + at(ga, x, y - 1) * (alfa);
                    Em = at(ga, x - 1, y + 1) * (1-alfa) + at(ga, x, y + 1) * (alfa);
                } else if(stupne <= 135) {
                    Ep = at(ga, x, y - 1) * (1 - alfa) + at(ga, x-1, y - 1) * alfa;
                    Em = at(ga, x + 1, y + 1) * (1 - alfa) + at(ga, x+1, y + 1) * alfa;
                } else if(stupne <= 180) {
                    Ep = at(ga, x - 1, y - 1) * (alfa) + at(ga, x - 1, y) * (1 - alfa);
                    Em = at(ga, x + 1, y + 1) * (alfa) + at(ga, x + 1, y) * (1 - alfa);
                } else {
                    throw new RuntimeException("xxd" + stupne);
                }

                if (Ep < c && c > Em) {
                    cur.get().set(c);
                }
                fia.next();
                cur.next();
            }
        });

        ij.ui().show(edges);
        //ij.ui().show(G);
        //ij.scifio().datasetIO().save(ij.dataset().create(ij.op().convert().uint8(edges)), "/tmp/a.tif");
        //Thread.sleep(10000000);

        Img<DoubleType> edges2 = ops.create().img(G);
        Pair<DoubleType, DoubleType> minmax = ops.stats().minMax(edges);

        ops.image().normalize(edges2, edges, minmax.getA(), minmax.getB(), new DoubleType(0), new DoubleType(1.0));

        System.out.println(minmax.getA()+" "+ minmax.getB());


        visited = ops.create().img(G);
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


        //ij.ui().show(edges2);
        ij.scifio().datasetIO().save(ij.dataset().create(ij.op().convert().uint8(ops.math().multiply(edges2, new DoubleType(255)))), "/tmp/a.jpg");

      //  ij.io().save(G, "/tmp/a.jpg");
        /*



        RandomAccessibleInterval G = ops.create().img(img);
        IterableInterval x = ops.math().add(new IterableRandomAccessibleInterval<DoubleType>(Gx), (IterableInterval<DoubleType>) new IterableRandomAccessibleInterval<DoubleType>(Gy));

        RAIs.binaryComputer(ops, Ops.Math.Add.class, Gx, Gy).compute(Gx, Gy);

        Ops.Math.Sqrt squareOp = ops.op(Ops.Math.Sqrt.class, DoubleType.class, DoubleType.class);
        RAIs.computer(ops, Ops.Math.Sqrt.class).compute(Gy, Gy);

        ij.ui().show(Gy);

        //RandomAccessibleInterval out = ops.create().img(img);


/*
        for(DoubleType o: (IterableInterval<DoubleType>) out) {


            o.set(0);
        }
        ij.ui().show(out);*/



       // img = ops.filter().gauss(img, 1);

    }

    private static void gradients(ImageJ ij, RandomAccessibleInterval withoutNoise, RandomAccessibleInterval<DoubleType> g, RandomAccessibleInterval<DoubleType> fi) {
        OpService ops = ij.op();
        RandomAccessibleInterval Gx = ops.create().img(withoutNoise);
        ops.filter().convolve(Gx, withoutNoise, createKernel(ops,
                -1, 0, +1,
                -2, 0, +2,
                -1, 0, +1
        ));
        RandomAccessibleInterval Gy = ops.create().img(withoutNoise);
        ops.filter().convolve(Gy, withoutNoise, createKernel(ops,
                1, 2, 1,
                0, 0, 0,
                -1, -2, -1
        ));

        ops.run(Parallel.class, g, (Consumer<Chunk<DoubleType>>) chunk -> {
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
                fia.get().set(localfi);

                Gxa.next();
                Gya.next();
                fia.next();
            }
        });

        //ij.ui().show(Gx);
    }

    private static void walk(long[] posit, RandomAccessibleInterval<DoubleType> img) {
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

                    position[0] += i;
                    position[1] += j;
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
