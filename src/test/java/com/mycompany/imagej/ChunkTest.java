package com.mycompany.imagej;

import com.mycompany.imagej.chunk.Chunk;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkTest {

    @org.junit.jupiter.api.Test
    void testSplitting() {
        long[] dims = new long[]{300, 200};
        byte[] data = new byte[(int) Intervals.numElements(dims)];

        ArrayImg img = new ArrayImg(data, dims, new Fraction(8, 1));

        Chunk chunk = new Chunk<>(img);
        assertEquals(300 * 200, chunk.getLen());
        assertEquals(0, chunk.getOffset());

        Chunk split = chunk.split(3);
        assertEquals(300 * 200, split.getLen());
        assertEquals(0, split.getOffset());

        assertEquals(300 * 200 / 3, split.getChunk(0).getLen());
        assertEquals(0, split.getChunk(0).getOffset());

        assertEquals(300 * 200 / 3, split.getChunk(1).getLen());
        assertEquals(300 * 200 / 3, split.getChunk(1).getOffset());

        assertEquals(300 * 200 / 3, split.getChunk(2).getLen());
        assertEquals(2 * 300 * 200 / 3, split.getChunk(2).getOffset());

        split = split.getChunk(1).split(2);
        assertEquals(300 * 200 / 3, split.getLen());
        assertEquals(300 * 200 / 3, split.getOffset());

        assertEquals(300 * 200 / 3 / 2, split.getChunk(0).getLen());
        assertEquals(300 * 200 / 3, split.getChunk(0).getOffset());

        assertEquals(300 * 200 / 3 / 2, split.getChunk(1).getLen());
        assertEquals(300 * 200 / 3 + 300 * 200 / 3 / 2, split.getChunk(1).getOffset());
    }
}