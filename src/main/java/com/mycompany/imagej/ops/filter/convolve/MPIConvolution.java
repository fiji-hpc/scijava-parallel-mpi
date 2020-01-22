package com.mycompany.imagej.ops.filter.convolve;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.Utils;
import io.scif.img.cell.SCIFIOCellImg;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.ops.Contingent;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.AbstractUnaryComputerOp;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.DirtyVolatileByteArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IterableRandomAccessibleInterval;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Plugin(type = Ops.Filter.Convolve.class, priority = Priority.HIGH + 2)
public class MPIConvolution<I extends RealType<I>, K extends RealType<K>, O extends RealType<O>>
            extends
            AbstractUnaryComputerOp<RandomAccessible<I>, RandomAccessibleInterval<O>>
            implements Ops.Filter.Convolve, Contingent
{
    @Parameter
    private RandomAccessibleInterval<K> kernel;

    @Override
    public boolean conforms() {
        return true;
    }

    @Override
    public void compute(RandomAccessible<I> input, RandomAccessibleInterval<O> output) {
        List<RandomAccessibleInterval<O>> blocks = Utils.splitAll(output);

        Utils.rootPrint("MPI, input: " + input + "; output: " + output);

        for (RandomAccessibleInterval<O> rai : blocks) {
            Utils.rootPrint(rai);
        }

        RandomAccessibleInterval<O> my_block = blocks.get(MPIUtils.getRank());
        Utils.print(my_block);

        int offset = 30;
        int value = offset + (255 - offset) / MPIUtils.getSize() * MPIUtils.getRank();
        for(O b: new IterableRandomAccessibleInterval<O>(my_block)) {
            b.setReal(value);
        }

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
            Utils.rootPrint(img.getCellGrid());

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
                    Utils.print(block + " cell=" + lastCell + " cell_ofset=" + cell_off + " length=" + length + "; node=" + i);

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
                int total_rows = block_rows * (int) block.dimension(2);

                int sent = 0;
                while(sent < total_rows) {
                    int cell_rows = (int) img.dimension(1);
                    int length = Math.min(total_rows - sent, cell_rows - cell_off);
                    Utils.print(block + " cell=" + lastCell + " cell_ofset=" + cell_off + " length=" + length + "; node=" + i);

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
               Utils.print("nope");
            }
        } else {
            throw new RuntimeException("Unsupported!" + output.getClass());
        }
    }

    public Cell get(SCIFIOCellImg img, long index) {
        try {
            return (Cell) img.getCache().get(index);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
