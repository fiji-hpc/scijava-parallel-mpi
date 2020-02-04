package com.mycompany.imagej.chunk;

import net.imglib2.Cursor;
import net.imglib2.Sampler;

public class ChunkCursor<T> implements Cursor<T> {
    private Cursor<T> cursor;
    private long count;
    private long remaining;

    public ChunkCursor(Cursor<T> cursor, long count) {
        this.cursor = cursor;
        this.count = count;
        this.remaining = count;
    }

    @Override
    public Cursor<T> copyCursor() {
        return cursor.copyCursor();
    }

    @Override
    public T next() {
        remaining--;
        return cursor.next();
    }

    @Override
    public void jumpFwd(long steps) {
        remaining -= steps;
        cursor.jumpFwd(steps);
    }

    @Override
    public void fwd() {
        remaining--;
        cursor.fwd();;
    }

    @Override
    public void reset() {
        remaining = count;
    }

    @Override
    public boolean hasNext() {
        return remaining > 0 && cursor.hasNext();
    }

    @Override
    public void localize(int[] position) {
        cursor.localize(position);
    }

    @Override
    public void localize(long[] position) {
        cursor.localize(position);
    }

    @Override
    public int getIntPosition(int d) {
        return cursor.getIntPosition(d);
    }

    @Override
    public long getLongPosition(int d) {
        return cursor.getIntPosition(d);
    }

    @Override
    public void localize(float[] position) {
        cursor.localize(position);
    }

    @Override
    public void localize(double[] position) {
        cursor.localize(position);
    }

    @Override
    public float getFloatPosition(int d) {
        return cursor.getIntPosition(d);
    }

    @Override
    public double getDoublePosition(int d) {
        return cursor.getIntPosition(d);
    }

    @Override
    public int numDimensions() {
        return cursor.numDimensions();
    }

    @Override
    public T get() {
        return cursor.get();
    }

    @Override
    public Sampler<T> copy() {
        return cursor.copy();
    }
}
