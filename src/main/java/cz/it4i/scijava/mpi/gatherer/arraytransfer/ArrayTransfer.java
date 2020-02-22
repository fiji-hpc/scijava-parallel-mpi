package cz.it4i.scijava.mpi.gatherer.arraytransfer;

import net.imglib2.Cursor;

public abstract class ArrayTransfer<T> {

   private int limit;
   private int capacity;

   public void allocate(int len) {
       if(capacity < len) {
           capacity = len;
           allocateArray(len);
       }

       limit = len;
   }

   protected int limit() {
       return limit;
   }

   abstract public void allocateArray(int len);
   abstract public void write(Cursor<T> cursor);
   abstract public void transfer(int root);
   abstract public void read(Cursor<T> cursor);
}
