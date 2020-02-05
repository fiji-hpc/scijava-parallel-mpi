package com.mycompany.imagej.chunk;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.Measure;
import com.mycompany.imagej.gatherer.RandomAccessibleIntervalGatherer;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Positionable;
import net.imglib2.RealPositionable;
import net.imglib2.util.Intervals;

import java.util.Iterator;

import static com.mycompany.imagej.Measure.measureCatch;

public class Chunk<T> implements IterableInterval<T> {
    private IterableInterval<T> data;
    private long offset;
    private long len;
    private int chunks;

    public Chunk(IterableInterval<T> data, int chunks) {
        this(data, 0, Intervals.numElements(data), chunks);
    }

    public Chunk(IterableInterval<T> data, long offset, long len) {
        this(data, offset, len, 1);
    }

    public Chunk(IterableInterval<T> data, long offset, long len, int chunks) {
        if(data instanceof Chunk) {
            Chunk<T> self = (Chunk<T>) data;
            this.data = self.data;
            this.offset = self.offset + offset;
        } else {
            this.data = data;
            this.offset = offset;
        }
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

    @Override
    public long size() {
        return len;
    }

    @Override
    public T firstElement() {
        return cursor().get();
    }

    @Override
    public Object iterationOrder() {
        return null;
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

    public Iterable<Chunk<T>> allChunks() {
        return new Iterable<Chunk<T>>() {
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

    @Override
    public long min(int d) {
        if(d != 0) {
            throw new IllegalArgumentException();
        }

        return offset;
    }

    @Override
    public void min(long[] min) {
        min[0] = min(0);
    }

    @Override
    public void min(Positionable min) {
        min.setPosition(min(0), 0);
    }

    @Override
    public long max(int d) {
        if(d != 0) {
            throw new IllegalArgumentException();
        }

        return offset + len;
    }

    @Override
    public void max(long[] max) {
        max[0] = max(1);
    }

    @Override
    public void max(Positionable max) {
        max.setPosition(max(1), 0);
    }

    @Override
    public void dimensions(long[] dimensions) {
        dimensions[0] = dimension(0);
    }

    @Override
    public long dimension(int d) {
        if(d != 0) {
            throw new IllegalArgumentException();
        }

        return len;
    }

    @Override
    public double realMin(int d) {
        return min(d);
    }

    @Override
    public void realMin(double[] min) {
        min[0] = min(0);
    }

    @Override
    public void realMin(RealPositionable min) {
        min.setPosition(min(0), 0);
    }

    @Override
    public double realMax(int d) {
        return max(d);
    }

    @Override
    public void realMax(double[] max) {
        max[0] = max(0);
    }

    @Override
    public void realMax(RealPositionable max) {
        max.setPosition(max(0), 0);
    }

    @Override
    public int numDimensions() {
        return 1;
    }

    @Override
    public Iterator<T> iterator() {
        return cursor();
    }
}
