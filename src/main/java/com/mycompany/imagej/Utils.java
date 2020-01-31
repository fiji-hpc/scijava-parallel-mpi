package com.mycompany.imagej;

import io.scif.img.cell.SCIFIOCellImg;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileByteArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.IterableRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.mycompany.imagej.Measure.measureCatch;

public class Utils {
    public static <I> List<RandomAccessibleInterval<I>> splitAll(RandomAccessibleInterval<I> input) {
        return splitAll(input, MPIUtils.getSize());
    }

    public static <I> List<RandomAccessibleInterval<I>> splitAll(RandomAccessibleInterval<I> input, int n) {
        List<RandomAccessibleInterval<I>> splits = new ArrayList<>();
        for(int i = 0; i < n; i++) {
            splits.add(splitFor(input, i, n));
        }
        return splits;
    }

    private static <I> IntervalView<I> splitFor(RandomAccessibleInterval<I> img, int rank, int nodes) {
        long[] from = new long[img.numDimensions()];
        long[] to = new long[img.numDimensions()];
        for (int d = 0; d < img.numDimensions(); d++) {
            from[d] = img.min(d);
            to[d] = img.max(d);
        }
        long per_node = rowsPerNode(img, nodes);
        to[img.numDimensions() - 1] = from[img.numDimensions() -1] + Math.min(
                 (rank + 1) * per_node - 1,
                img.dimension(img.numDimensions() - 1) - 1
        );
        from[img.numDimensions() - 1] += rank * per_node;

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

    public static <O extends RealType<O>> void gather(RandomAccessibleInterval<O> output, List<RandomAccessibleInterval<O>> blocks) {
        if(output instanceof IntervalView) {
            output = (RandomAccessibleInterval<O>) (((IntervalView<O>) output).getSource());
        }

        if(output instanceof Dataset) {
            output = (RandomAccessibleInterval<O>) ((Dataset) output).getImgPlus();
        }

        if(output instanceof ImgPlus) {
            output = ((ImgPlus<O>) output).getImg();
        }

        RandomAccessibleInterval<O> finalOutput = output;
        measureCatch("gather", () -> {
            if(System.getenv("B_GATHER_GENERIC") != null) {
                rootPrint("Gather: gatherGeneric (overridden)");
                gatherGeneric(blocks, finalOutput);
            } else if(finalOutput instanceof ArrayImg) {
                rootPrint("Gather: gatherImg");
                gatherImg(blocks, (ArrayImg) finalOutput);
            } else if(finalOutput instanceof SCIFIOCellImg) {
                rootPrint("Gather: gatherCells");
                gatherCells(blocks, (SCIFIOCellImg) finalOutput);
            } else if(finalOutput instanceof PlanarImg) {
                rootPrint("Gather: gatherPlanar");
                gatherPlanar(blocks, (PlanarImg) finalOutput);
            } else {
                rootPrint("Gather: gatherGeneric");
                gatherGeneric(blocks, finalOutput);
            }
        });
    }

    private static <O extends RealType<O>> void gatherGeneric(List<RandomAccessibleInterval<O>> blocks, RandomAccessibleInterval<O> img) {
        NonBlockingBroadcast broadcast = new NonBlockingBroadcast();
        List<float[]> results = new ArrayList<>();

        for (int node = 0; node < blocks.size(); node++) {
            RandomAccessibleInterval<O> block = blocks.get(node);

            long[] dims = new long[block.numDimensions()];
            block.dimensions(dims);
            int count = (int) Intervals.numElements(dims);
            if (count > 0) {
                float[] storage = new float[count];
                results.add(storage);
                if (node == MPIUtils.getRank()) {
                    int j = 0;
                    for (O item : new IterableRandomAccessibleInterval<>(block)) {
                        FloatType b = (FloatType) item;
                        storage[j++] = b.getRealFloat();
                    }
                }

                broadcast.requestTransfer(node, storage, 0, count);
            }
        }

        broadcast.waitForTransfer();

        for (int node = 0; node < blocks.size(); node++) {
            RandomAccessibleInterval<O> block = blocks.get(node);
            float[] storage = results.get(node);
            if (node != MPIUtils.getRank()) {
                int j = 0;
                for (O item : new IterableRandomAccessibleInterval<>(block)) {
                    FloatType b = (FloatType) item;
                    b.setReal(storage[j++]);
                }
            }
        }
    }

    private static <O extends RealType<O>> void gatherPlanar(List<RandomAccessibleInterval<O>> blocks, PlanarImg img) {
        int cols = (int) img.dimension(0);

        NonBlockingBroadcast transfer = new NonBlockingBroadcast();
        int cell_off = 0;
        ArrayDataAccess<?> c = img.getPlane(0);
        long lastCell = 0;
        for(int i = 0; i < blocks.size(); i++) {
            RandomAccessibleInterval<O> block = blocks.get(i);
            int block_rows = (int) block.dimension(1);
            int total_rows = block_rows;
            if(block.numDimensions() > 2) {
                total_rows *= (int) block.dimension(2);
            }

            int sent = 0;
            while(sent < total_rows) {
                int cell_rows = (int) img.dimension(1);
                int length = Math.min(total_rows - sent, cell_rows - cell_off);
                print(block + " cell=" + lastCell + " cell_ofset=" + cell_off + " length=" + length + "; node=" + i);

                transfer.requestTransfer(i, c, cell_off * cols, cols * length);

                cell_off += length;
                if(cell_off > cell_rows) {
                    throw new RuntimeException("UNEXPECTED");
                }
                if(cell_off == cell_rows) {
                    lastCell++;
                    cell_off = 0;

                    if(lastCell < img.numSlices()) {
                        c = img.getPlane((int) lastCell);
                    }
                }

                sent += length;
            }
        }
        if(lastCell != img.numSlices()) {
            throw new RuntimeException("Unexpected");
        }

        transfer.waitForTransfer();
    }

    private static <O extends RealType<O>> void gatherCells(List<RandomAccessibleInterval<O>> blocks, SCIFIOCellImg img) {
        rootPrint(img.getCellGrid());

        int cols = (int) img.dimension(0);

        NonBlockingBroadcast transfer = new NonBlockingBroadcast();
        int cell_off = 0;
        Cell c = get(img, 0);
        long lastCell = 0;
        for(int i = 0; i < blocks.size(); i++) {
            RandomAccessibleInterval<O> block = blocks.get(i);
            int block_rows = (int) block.dimension(1);

            int sent = 0;
            while(sent < block_rows) {
                int cell_rows = img.getCellGrid().getCellDimension(1, lastCell);
                int length = Math.min(block_rows - sent, cell_rows - cell_off);
                print(block + " cell=" + lastCell + " cell_ofset=" + cell_off + " length=" + length + "; node=" + i);

                transfer.requestTransfer(i, (ArrayDataAccess) c.getData(), cell_off * cols, cols * length);
                ((DirtyVolatileByteArray) c.getData()).setDirty();

                cell_off += length;
                if(cell_off > cell_rows) {
                    throw new RuntimeException("UNEXPECTED");
                }
                if(cell_off == cell_rows) {
                    lastCell++;
                    cell_off = 0;

                    c = get(img, lastCell);
                }

                sent += length;
            }
        }

        transfer.waitForTransfer();

    }

    private static <O extends RealType<O>> void gatherImg(List<RandomAccessibleInterval<O>> blocks, ArrayImg img) {
        NonBlockingBroadcast transfer = new NonBlockingBroadcast();
        int off = 0;
        for(int i = 0; i < blocks.size(); i++) {
            RandomAccessibleInterval<O> block = blocks.get(i);
            int length = (int) Intervals.numElements(block);

            transfer.requestTransfer(i, (ArrayDataAccess) img.update(null), off, length);
            off += length;
        }

        transfer.waitForTransfer();
    }

    public static Cell get(SCIFIOCellImg img, long index) {
        try {
            return (Cell) img.getCache().get(index);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
