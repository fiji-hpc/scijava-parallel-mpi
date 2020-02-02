package com.mycompany.imagej;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.view.IntervalView;

import static com.mycompany.imagej.Measure.measureCatch;

public class RandomAccessibleIntervalGatherer {
   public static <O extends RealType<O>> void gather(Chunk<O> chunk) {
      RandomAccessibleInterval<O> output = chunk.getData();
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
         if(System.getenv("B_GATHER_GENERIC") != null || true) {
            Utils.rootPrint("Gather: gatherGeneric (overridden)");
            gatherGeneric(chunk);
         } else if(finalOutput instanceof PlanarImg) {
            Utils.rootPrint("Gather: gatherPlanar");
            throw new RuntimeException("not implemented!");
//            gatherPlanar(blocks, (PlanarImg) finalOutput);
         } else {
            Utils.rootPrint("Gather: gatherGeneric");
            gatherGeneric(chunk);
         }
      });
   }

   private static <O extends RealType<O>> void gatherGeneric(Chunk<O> chunks) {
      NonBlockingBroadcast b = new NonBlockingBroadcast();
      byte[][] storages = new byte[MPIUtils.getSize()][];
      int c = 0;
      for(Chunk<O> chunk: chunks) {
         byte[] storage = new byte[(int) chunks.getLen()];
         int i = 0;
         for (Cursor<O> it = chunk.cursor(); it.hasNext(); ) {
            O t = it.next();
            storage[i++] = ((ByteType) t).getByte();
         }

         b.requestTransfer(c, storage, 0, storage.length);
         storages[c++] = storage;
      }

      b.waitForTransfer();

      for(int i = 0; i < storages.length; i++) {
         byte[] storage = storages[i];
         int j = 0;
         for (Cursor<O> it = chunks.getChunk(i).cursor(); it.hasNext();) {
            O t = it.next();
            ((ByteType) t).setByte(storage[j++]);
         }
      }
   }
}
