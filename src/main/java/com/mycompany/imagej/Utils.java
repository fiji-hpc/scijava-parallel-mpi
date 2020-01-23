package com.mycompany.imagej;

import io.scif.img.cell.SCIFIOCellImg;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileByteArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    public static <O extends RealType<O>> void gather(RandomAccessibleInterval<O> output, List<RandomAccessibleInterval<O>> blocks) {
        if(output instanceof Dataset) {
            output = (RandomAccessibleInterval<O>) ((Dataset) output).getImgPlus();
        }

        if(output instanceof ImgPlus) {
            output = ((ImgPlus<O>) output).getImg();
        }

        if(output instanceof ArrayImg) {
            ArrayImg img = (ArrayImg) output;
            int off = 0;
            for(int i = 0; i < blocks.size(); i++) {
                RandomAccessibleInterval<O> block = blocks.get(i);
                byte[] data = ((ByteArray) img.update(null)).getCurrentStorageArray();
                int length = (int) Intervals.numElements(block);

                int ret = MPIUtils.MPILibrary.INSTANCE.MPI_Bcast(
                        ByteBuffer.wrap(data, off, length).slice(),
                        length, MPIUtils.MPI_BYTE, i, MPIUtils.MPI_COMM_WORLD);
                if(ret != 0) {
                    throw new RuntimeException("mpi failed");
                }
                off += length;
            }
        } else if(output instanceof SCIFIOCellImg) {
            SCIFIOCellImg img = (SCIFIOCellImg) output;
            rootPrint(img.getCellGrid());

            int cols = (int) img.dimension(0);

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

                    byte[] data = ((DirtyVolatileByteArray) c.getData()).getCurrentStorageArray();

                    int ret = MPIUtils.MPILibrary.INSTANCE.MPI_Bcast(
                            ByteBuffer.wrap(data, cell_off * cols, cols * length).slice(),
                            cols * length, MPIUtils.MPI_BYTE, i, MPIUtils.MPI_COMM_WORLD);
                    if(ret != 0) {
                        throw new RuntimeException("mpi failed");
                    }
                    ((DirtyVolatileByteArray) c.getData()).setDirty();

                    cell_off += length;
                    if(cell_off > cell_rows) {
                        System.out.println("UNEXPECTED");
//                        throw new RuntimeException("UNEXPECTED");
                    }
                    if(cell_off == cell_rows) {
                        lastCell++;
                        cell_off = 0;

                        c = get(img, lastCell);
                    }

                    sent += length;
                }
            }
        } else if(output instanceof PlanarImg) {
            PlanarImg img = (PlanarImg) output;
            int cols = (int) img.dimension(0);

            int cell_off = 0;
            ByteArray c = (ByteArray) img.getPlane(0);
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

                    byte[] data = c.getCurrentStorageArray();

                    int ret = MPIUtils.MPILibrary.INSTANCE.MPI_Bcast(
                            ByteBuffer.wrap(data, cell_off * cols, cols * length).slice(),
                            cols * length, MPIUtils.MPI_BYTE, i, MPIUtils.MPI_COMM_WORLD);
                    if(ret != 0) {
                        throw new RuntimeException("mpi failed");
                    }

                    cell_off += length;
                    if(cell_off > cell_rows) {
                        System.out.println("UNEXPECTED");
//                        throw new RuntimeException("UNEXPECTED");
                    }
                    if(cell_off == cell_rows) {
                        lastCell++;
                        cell_off = 0;

                        if(lastCell < img.numSlices()) {
                            c = (ByteArray) img.getPlane((int) lastCell);
                        }
                    }

                    sent += length;
                }
            }
            if(lastCell != img.numSlices()) {
               print("nope");
            }
        } else {
            throw new RuntimeException("Unsupported!" + output.getClass());
        }
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
