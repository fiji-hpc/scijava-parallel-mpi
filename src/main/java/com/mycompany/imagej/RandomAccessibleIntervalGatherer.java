package com.mycompany.imagej;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.view.IntervalView;

import java.util.ArrayList;
import java.util.List;

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
         if(System.getenv("B_GATHER_GENERIC") != null) {
            Utils.rootPrint("Gather: gatherGeneric (overridden)");
            gatherGeneric(chunk);
         } else if(finalOutput instanceof PlanarImg) {
            Utils.rootPrint("Gather: gatherPlanar");
            gatherPlanar(chunk, (PlanarImg) finalOutput);
         } else {
            Utils.rootPrint("Gather: gatherGeneric");
            gatherGeneric(chunk);
         }
      });
   }

   private static <O extends RealType<O>> void gatherPlanar(Chunk<O> chunks, PlanarImg img) {
      NonBlockingBroadcast transfer = new NonBlockingBroadcast();
      int currentRoot = 0;

      ArrayDataAccess<?> c = img.getPlane(0);
      int planeOffset = 0;
      int lastPlane = 0;

      for(Chunk<O> chunk: chunks) {
            List<NonBlockingBroadcast.Block> transferBlocks = new ArrayList<>();

            long sent = 0;
            long totalLen = chunk.getLen();
            while(sent < totalLen) {
               int length = (int) Math.min(totalLen - sent, c.getArrayLength() - planeOffset);
               transferBlocks.add(new NonBlockingBroadcast.Block(c.getCurrentStorageArray(), planeOffset, length));

               planeOffset += length;
               if(planeOffset > c.getArrayLength()) {
                  throw new RuntimeException("UNEXPECTED");
               }
               if(planeOffset == c.getArrayLength()) {
                  lastPlane++;
                  planeOffset = 0;

                  if(lastPlane < img.numSlices()) {
                     c = img.getPlane((int) lastPlane);
                  }
               }

               sent += length;
            }

            transfer.requestTransfer(currentRoot++, transferBlocks);
         }
         if(lastPlane != img.numSlices()) {
            throw new RuntimeException("Unexpected");
         }

         transfer.waitForTransfer();
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
