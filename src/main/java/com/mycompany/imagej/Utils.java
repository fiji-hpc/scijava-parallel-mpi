package com.mycompany.imagej;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.List;

public class Utils {
      public static <I> List<RandomAccessibleInterval<I>> splitAll(RandomAccessibleInterval<I> input) {
        int n = MPIUtils.getSize();

        List<RandomAccessibleInterval<I>> splits = new ArrayList<>();
        for(int i = 0; i < n; i++) {
            splits.add(splitFor(input, i, n));
        }
        return splits;
    }

    private static <I> IntervalView<I> splitFor(RandomAccessibleInterval<I> img, int rank, int nodes) {
        long[] from = new long[img.numDimensions()];
        long[] to = new long[img.numDimensions()];
        for (int d = 0; d < img.numDimensions() - 1; d++) {
            from[d] = img.min(d);
            to[d] = img.max(d);
        }
        long per_node = rowsPerNode(img, nodes);
        from[img.numDimensions() - 1] = rank * per_node;
        to[img.numDimensions() - 1] = Math.min(
                (rank + 1) * per_node - 1,
                img.dimension(img.numDimensions() - 1) - 1
        );

        return Views.interval(img, from, to);
    }

    private static <I> int rowsPerNode(RandomAccessibleInterval<I> img, int nodes) {
        return (int) ((img.dimension(img.numDimensions() - 1) + nodes) / nodes);
    }

    public static void print(Object s) {
        System.out.println(MPIUtils.getRank() + ": " + s.toString());
    }

    public static void rootPrint(Object s) {
         if(MPIUtils.isRoot()) {
            System.out.println(s.toString());
         }
    }
}
