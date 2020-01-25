package com.mycompany.imagej;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.util.ArrayList;
import java.util.List;

public class NonBlockingBroadcast {
    private class BlockTransfer {
        private Pointer request;
        private byte[] data;
        private Memory memory;
        private int offset;
        private int len;
        private int root;

        public BlockTransfer(int root, byte[] data, int offset, int len) {
            this.root = root;
            this.data = data;
            this.offset = offset;
            this.len = len;

            this.memory = new Memory(len);
            this.requestTransfer();
        }

        private void requestTransfer() {
            // copy data only from root rank
            if(MPIUtils.getRank() == root) {
                memory.write(0, data, offset, len);
            }

            // request a nonblocking transfer
            PointerByReference ptr = new PointerByReference();
            int ret = MPIUtils.MPILibrary.INSTANCE.MPI_Ibcast(
                    memory,
                    len, MPIUtils.MPI_BYTE, root, MPIUtils.MPI_COMM_WORLD, ptr);
            if(ret != 0) {
                throw new RuntimeException("mpi failed");
            }
            request = ptr.getValue();
        }

        public void finishTransfer() {
            if(MPIUtils.getRank() != root) {
                memory.read(0, data, offset, len);
            }
        }
    };

    private List<BlockTransfer> transfers = new ArrayList<>();

    public void requestTransfer(int root, byte[] data, int offset, int len) {
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
