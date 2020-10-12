package cz.it4i.scijava.mpi;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;

import java.util.ArrayList;
import java.util.List;

public class NonBlockingBroadcast {
    public static class Block {
        private Object array;
        private int offset;
        private int len;

        public Block(Object array, int offset, int len) {
            this.array = array;
            this.offset = offset;
            this.len = len;
        }
    }

    private class BlockTransfer {
        private Pointer request;
        private Memory memory;
        private int root;
        private List<Block> blocks;
        private long totalElements = 0;

        public BlockTransfer(int root, List<Block> blocks) {
            this.root = root;
            this.blocks = blocks;

            for(Block b: blocks) {
                totalElements += b.len;
            }

            if(totalElements >= Integer.MAX_VALUE) {
                throw new RuntimeException("Total elements too large: " + totalElements);
            }

            this.memory = new Memory(totalElements * getTypeSize());
            this.requestTransfer();
        }

        private int getTypeSize() {
            Object data = blocks.get(0).array;
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
            Object data = blocks.get(0).array;
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
                long offset = 0;
                for(Block block: blocks) {
                    int sent = 0;
                    while(sent < block.len) {
                        int chunk = Math.min((int) (2147483640L / getTypeSize()), block.len - sent);
                        if (block.array instanceof byte[]) {
                            memory.write(offset, (byte[]) block.array, block.offset + sent, chunk);
                        } else if (block.array instanceof short[]) {
                            memory.write(offset, (short[]) block.array, block.offset + sent, chunk);
                        } else if (block.array instanceof int[]) {
                            memory.write(offset, (int[]) block.array, block.offset + sent, chunk);
                        } else if (block.array instanceof long[]) {
                            memory.write(offset, (long[]) block.array, block.offset + sent, chunk);
                        } else if (block.array instanceof float[]) {
                            memory.write(offset, (float[]) block.array, block.offset + sent, chunk);
                        } else if (block.array instanceof double[]) {
                            memory.write(offset, (double[]) block.array, block.offset + sent, chunk);
                        } else {
                            throw new RuntimeException("Unsupported type: " + block.array);
                        }
                        sent += chunk;
                        offset += chunk * getTypeSize();
                    }
                }
            }

            // request a nonblocking transfer
            PointerByReference ptr = new PointerByReference();
            int ret = MPIUtils.MPILibrary.INSTANCE.MPI_Ibcast(
                    memory,
                    (int) totalElements, getMPIType(), root, MPIUtils.currentComm, ptr);

            if(ret != 0) {
                throw new RuntimeException("mpi failed");
            }
            request = ptr.getValue();
        }

        public void finishTransfer() {
            if(MPIUtils.getRank() != root) {
                long offset = 0;
                for(Block block: blocks) {
                    if(block.array instanceof byte[]) {
                        memory.read(offset, (byte[]) block.array, block.offset, block.len);
                    } else if(block.array instanceof short[]) {
                        memory.read(offset, (short[]) block.array, block.offset, block.len);
                    } else if(block.array instanceof int[]) {
                        memory.read(offset, (int[]) block.array, block.offset, block.len);
                    } else if(block.array instanceof long[]) {
                        memory.read(offset, (long[]) block.array, block.offset, block.len);
                    } else if(block.array instanceof float[]) {
                        memory.read(offset, (float[]) block.array, block.offset, block.len);
                    } else if(block.array instanceof double[]) {
                        memory.read(offset, (double[]) block.array, block.offset, block.len);
                    } else {
                        throw new RuntimeException("Unsupported type: " + block.array);
                    }
                    offset += block.len * getTypeSize();
                }
            }
        }
    };

    private List<BlockTransfer> transfers = new ArrayList<>();

    public <A> void requestTransfer(int root, ArrayDataAccess<A> data, int offset, int len) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block(data.getCurrentStorageArray(), offset, len));
        requestTransfer(root, blocks);
    }

    public <A> void requestTransfer(int root, float[] data, int offset, int len) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block(data, offset, len));

        requestTransfer(root, blocks);
    }
    public <A> void requestTransfer(int root, byte[] data, int offset, int len) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block(data, offset, len));

        requestTransfer(root, blocks);
    }

    public void requestTransfer(int root, List<Block> blocks) {
        if(blocks.size() > 0) {
            this.transfers.add(new BlockTransfer(root, blocks));
        }
    }

    public void waitForTransfer() {
        Pointer[] ptrs = new Pointer[transfers.size()];
        for(int i = 0; i < transfers.size(); i++) {
            ptrs[i] = transfers.get(i).request;
        }

        int ret = MPIUtils.MPILibrary.INSTANCE.MPI_Waitall(ptrs.length, ptrs, null);
        if(ret != 0) {
            throw new RuntimeException("MPI_Waitall failed");
        }
        for(BlockTransfer transfer: transfers) {
            transfer.finishTransfer();
        }
        transfers.clear();
    }
}
