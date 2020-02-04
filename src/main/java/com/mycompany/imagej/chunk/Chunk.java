package com.mycompany.imagej.chunk;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.Measure;
import com.mycompany.imagej.gatherer.RandomAccessibleIntervalGatherer;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.util.Intervals;

import java.util.Iterator;

import static com.mycompany.imagej.Measure.measureCatch;

public class Chunk<T> implements Iterable<Chunk<T>> {
    private IterableInterval<T> data;
    private long offset;
    private long len;
    private int chunks;

    public Chunk(IterableInterval<T> data, int chunks) {
        this.data = data;
        this.chunks = chunks;
        this.offset = 0;
        this.len = Intervals.numElements(data);
    }

    public Chunk(IterableInterval<T> data, long offset, long len) {
        this(data, offset, len, 1);
    }

    public Chunk(IterableInterval<T> data, long offset, long len, int chunks) {
        this.data = data;
        this.offset = offset;
        this.len = len;
        this.chunks = chunks;
    }

    public Chunk(IterableInterval<T> data) {
        this(data, 1);
    }

    public Cursor<T> localizingCursor() {
        Cursor<T> cursor = data.localizingCursor();
        cursor.jumpFwd(offset);

        return new ChunkCursor<>(
                cursor,
                len
        );
    }

    public Cursor<T> cursor() {
        Cursor<T> cursor = data.cursor();
        cursor.jumpFwd(offset);

        return new ChunkCursor<>(
                cursor,
                len
        );
    }

    public Chunk<T> getChunk(int n) {
        if(n < 0 || n >= chunks) {
            throw new IllegalArgumentException("Invalid chunk: " + n);
        }

        long elementsPerNode = (long) Math.ceil((double) len / chunks);
        long startIndex = offset + elementsPerNode * n;
        long end = Math.min(startIndex + elementsPerNode, offset + len);

        return new Chunk<T>(data, startIndex, end  - startIndex);
    }

    public Chunk<T> split(int chunks) {
        return new Chunk<>(data, offset, len, chunks);
    }

    public long getOffset() {
        return offset;
    }

    public long getLen() {
        return len;
    }

    public IterableInterval<T> getData() {
        return data;
    }

    public void sync() {
        Measure.measureCatch("barrier", MPIUtils::barrier);
        measureCatch("gather", () -> RandomAccessibleIntervalGatherer.gather(this));
    }

    @Override
    public Iterator<Chunk<T>> iterator() {
        return new Iterator<Chunk<T>>() {
            private int chunk;

            @Override
            public boolean hasNext() {
                return chunk < chunks;
            }

            @Override
            public Chunk<T> next() {
                return getChunk(chunk++);
            }
        };
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "data=" + data +
                ", offset=" + offset +
                ", len=" + len +
                ", chunks=" + chunks +
                '}';
    }
}
