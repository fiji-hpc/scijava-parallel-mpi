package com.mycompany.imagej.gatherer;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.chunk.Chunk;
import com.mycompany.imagej.gatherer.arraytransfer.*;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

public class GenericGatherer<O> implements Gatherer<O> {
    @Override
    public boolean gather(Chunk<O> chunks) {
        ArrayTransfer access = createArrayAccess(chunks);
        int root = 0;

        for (Chunk<O> chunk : chunks.allChunks()) {
            access.allocate((int) chunk.getLen());

            if (root == MPIUtils.getRank()) {
                Cursor<O> cursor = chunk.cursor();
                access.write(cursor);
            }

            access.transfer(root);

            if (root != MPIUtils.getRank()) {
                Cursor<O> cursor = chunk.cursor();
                access.read(cursor);
            }

            root++;
        }

        return true;
    }

    private ArrayTransfer createArrayAccess(Chunk<O> chunk) {
        Cursor c = chunk.cursor();
        c.fwd();
        Object type = c.get();

        if(type instanceof GenericByteType) {
            return new ByteArrayTransfer();
        } else if(type instanceof GenericShortType) {
            return new ShortArrayTransfer();
        } else if(type instanceof GenericIntType) {
            return new IntArrayTransfer();
        } else if(type instanceof GenericLongType) {
            return new LongArrayTransfer();
        } else if(type instanceof FloatType) {
            return new FloatArrayTransfer();
        } else if(type instanceof DoubleType) {
            return new DoubleArrayTransfer();
        } else {
            throw new RuntimeException("Unsupported type: " + type.getClass());
        }
    }
}
