package com.mycompany.imagej;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;

import java.util.ArrayList;
import java.util.List;

public class NonBlockingBroadcast {
    private class BlockTransfer {
        private Pointer request;
        private Object data;
        private Memory memory;
        private int offset;
        private int len;
        private int root;

        public BlockTransfer(int root, Object data, int offset, int len) {
            this.root = root;
            this.data = data;
            this.offset = offset;
            this.len = len;

            this.memory = new Memory(len * getTypeSize());
            this.requestTransfer();
        }

        private int getTypeSize() {
            if(data instanceof byte[]) {
                return 1;
            } else if(data instanceof short[]) {
                return 2;
            } else if(data instanceof int[]) {
                return 4;
            } else if(data instanceof long[]) {
                return 8;
            } else if(data instanceof float[]) {
                return 4;
            } else if(data instanceof double[]) {
                return 8;
            } else {
                throw new RuntimeException("Unsupported type: " + data);
            }
        }

        private Pointer getMPIType() {
            if(data instanceof byte[]) {
                return MPIUtils.MPI_BYTE;
            } else if(data instanceof short[]) {
                return MPIUtils.MPI_UNSIGNED_SHORT;
            } else if(data instanceof int[]) {
                return MPIUtils.MPI_UNSIGNED;
            } else if(data instanceof long[]) {
                return MPIUtils.MPI_UNSIGNED_LONG;
            } else if(data instanceof float[]) {
                return MPIUtils.MPI_FLOAT;
            } else if(data instanceof double[]) {
                return MPIUtils.MPI_DOUBLE;
            } else {
                throw new RuntimeException("Unsupported type: " + data);
            }
        }

        private void requestTransfer() {
            // copy data only from root rank
            if(MPIUtils.getRank() == root) {
                if(data instanceof byte[]) {
                    memory.write(0, (byte[]) data, offset, len);
                } else if(data instanceof short[]) {
                    memory.write(0, (short[]) data, offset, len);
                } else if(data instanceof int[]) {
                    memory.write(0, (int[]) data, offset, len);
                } else if(data instanceof long[]) {
                    memory.write(0, (long[]) data, offset, len);
                } else if(data instanceof float[]) {
                    memory.write(0, (float[]) data, offset, len);
                } else if(data instanceof double[]) {
                    memory.write(0, (double[]) data, offset, len);
                } else {
                    throw new RuntimeException("Unsupported type: " + data);
                }
            }

            // request a nonblocking transfer
            PointerByReference ptr = new PointerByReference();
            int ret = MPIUtils.MPILibrary.INSTANCE.MPI_Ibcast(
                    memory,
                    len, getMPIType(), root, MPIUtils.MPI_COMM_WORLD, ptr);
            if(ret != 0) {
                throw new RuntimeException("mpi failed");
            }
            request = ptr.getValue();
        }

        public void finishTransfer() {
            if(MPIUtils.getRank() != root) {
                if(data instanceof byte[]) {
                    memory.read(0, (byte[]) data, offset, len);
                } else if(data instanceof short[]) {
                    memory.read(0, (short[]) data, offset, len);
                } else if(data instanceof int[]) {
                    memory.read(0, (int[]) data, offset, len);
                } else if(data instanceof long[]) {
                    memory.read(0, (long[]) data, offset, len);
                } else if(data instanceof float[]) {
                    memory.read(0, (float[]) data, offset, len);
                } else if(data instanceof double[]) {
                    memory.read(0, (double[]) data, offset, len);
                } else {
                    throw new RuntimeException("Unsupported type: " + data);
                }
            }
        }
    };

    private List<BlockTransfer> transfers = new ArrayList<>();

    public <A> void requestTransfer(int root, ArrayDataAccess<A> data, int offset, int len) {
        this.transfers.add(new BlockTransfer(root, data.getCurrentStorageArray(), offset, len));
    }

    public <A> void requestTransfer(int root, float[] data, int offset, int len) {
        this.transfers.add(new BlockTransfer(root, data, offset, len));
    }

    public void waitForTransfer() {
        Pointer[] ptrs = new Pointer[transfers.size()];
        for(int i = 0; i < transfers.size(); i++) {
            ptrs[i] = transfers.get(i).request;
        }

        MPIUtils.MPILibrary.INSTANCE.MPI_Waitall(ptrs.length, ptrs, null);
        for(BlockTransfer transfer: transfers) {
            transfer.finishTransfer();
        }
        transfers.clear();
    }
}
