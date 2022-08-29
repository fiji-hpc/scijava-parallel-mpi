package cz.it4i.scijava.mpi;

import cz.it4i.scijava.mpi.mpi.ReduceOp;
import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.Native;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import java.lang.reflect.Field;

public class MPIUtils {
    public static Pointer MPI_BYTE;
    public static Pointer MPI_UNSIGNED_SHORT;
    public static Pointer MPI_UNSIGNED;
    public static Pointer MPI_UNSIGNED_LONG;
    public static Pointer MPI_FLOAT;
    public static Pointer MPI_DOUBLE;
    public static Pointer MPI_OP_MIN;
    public static Pointer MPI_OP_MAX;
    public static Pointer MPI_OP_SUM;

    public static Pointer MPI_COMM_WORLD;
    public static Pointer currentComm;
    
  	private static Pointer MPI_THREAD_MULTIPLE;

    private static NativeLibrary mpilib;
    static {
        mpilib = NativeLibrary.getInstance("mpi");
        Init();
        Runtime.getRuntime().addShutdownHook(new Thread(MPIUtils::Finalize));
    }

    private static void checkMpiResult(int ret) {
        if(ret != 0) {
            throw new RuntimeException("MPI failed with: " + ret);
        }
    }

    private static void Init() {
        int[] isInitialized = new int[1];
        checkMpiResult(MPILibrary.INSTANCE.MPI_Initialized(isInitialized));
        if(isInitialized[0] == 0){
        	int[] provided = new int[1];
    			checkMpiResult(MPILibrary.INSTANCE.MPI_Init_thread(null, null,
    				MPI_THREAD_MULTIPLE, provided));
        }
        for(Field f: MPIUtils.class.getDeclaredFields()) {
            if(!f.getName().startsWith("MPI_")) {
                continue;
            }

            try {
                f.set(null, getSymbolPtr("ompi_" + f.getName().toLowerCase()));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        currentComm = MPI_COMM_WORLD;
    }

    public static void Finalize() {
        int[] isFinalized = new int[1];
        checkMpiResult(MPILibrary.INSTANCE.MPI_Finalized(isFinalized));
        if(isFinalized[0] == 0) {
            MPILibrary.INSTANCE.MPI_Finalize();
        }
    }

    public static void split(int color, int key) {
        Pointer newcomm = new Memory(8 /* TODO: Pointer.SIZE */);
        PointerByReference ptr = new PointerByReference(newcomm);
        checkMpiResult(MPILibrary.INSTANCE.MPI_Comm_split(MPI_COMM_WORLD, color, key, ptr));
        currentComm = ptr.getValue();
    }

    public static void setCommWorld() {
        currentComm = MPI_COMM_WORLD;
    }

    public static int getSize() {
        int[] rank = new int[1];
        checkMpiResult(MPILibrary.INSTANCE.MPI_Comm_size(currentComm, rank));
        return rank[0];
    }

    public static int getRank() {
        int[] rank = new int[1];
        checkMpiResult(MPILibrary.INSTANCE.MPI_Comm_rank(currentComm, rank));
        return rank[0];
    }

    public static void barrier() {
        checkMpiResult(MPILibrary.INSTANCE.MPI_Barrier(currentComm));
    }

    public static String getProcessorName() {
        byte[] name = new byte[1024]; //MPI_MAX_PROCESSOR_NAME
        int [] len = new int[1];
        checkMpiResult(MPILibrary.INSTANCE.MPI_Get_processor_name(name, len));
        return new String(name, 0, len[0]);
    }

    public static boolean isRoot() {
        return getRank() == 0;
    }

    public static void Send(long buf, int count, Pointer datatype, int dest, int tag, Pointer comm) {
        checkMpiResult(MPILibrary.INSTANCE.MPI_Send(buf, count, datatype, dest, tag, comm));
    }

    public static void Recv(long buf, int count, Pointer datatype, int dest, int tag, Pointer comm, Pointer status) {
        checkMpiResult(MPILibrary.INSTANCE.MPI_Recv(buf, count, datatype, dest, tag, comm, status));
    }

    public static Pointer toOP(ReduceOp op) {
        switch(op) {
            case SUM:
                return MPI_OP_SUM;
            case MIN:
                return MPI_OP_MIN;
            case MAX:
                return MPI_OP_MAX;
        }
        throw new RuntimeException("unsupported");
    }

    public static Object Allreduce(Object send, ReduceOp op) {
        Pointer mpiType = MPIType.toMPI(send);
        Memory local = new Memory(MPIType.size(mpiType));
        Memory global = new Memory(MPIType.size(mpiType));

        if(mpiType == MPI_BYTE) {
            if(send instanceof GenericByteType) {
                local.setByte(0, ((GenericByteType) send).getByte());
            } else {
                local.setByte(0, (byte) send);
            }
        } else if(mpiType == MPI_DOUBLE) {
            local.setDouble(0, (double) send);
        } else {
            throw new RuntimeException("Unsupported");
        }

        int ret = MPILibrary.INSTANCE.MPI_Allreduce(
                local,
                global,
                1,
                mpiType,
                toOP(op),
                MPIUtils.currentComm
        );
        checkMpiResult(ret);

        if(mpiType == MPI_BYTE) {
            if(send instanceof GenericByteType) {
                return new UnsignedByteType(global.getByte(0));
            } else {
                return global.getByte(0);
            }
        } else if(mpiType == MPI_DOUBLE) {
            return global.getDouble(0);
        }
        throw new RuntimeException("Unsupported");
    }

    public static void Allreduce(double[] sendbuf, double[] recvbuf, int count, Pointer datatype, Pointer op, Pointer comm) {
        checkMpiResult(MPILibrary.INSTANCE.MPI_Allreduce(sendbuf, recvbuf, count, datatype, op, comm));
    }

    private static Pointer getSymbolPtr(String name) {
        return mpilib.getGlobalVariableAddress(name);
    }

    public interface MPILibrary extends Library {
        MPILibrary INSTANCE = Native.load("mpi", MPILibrary.class);
        int MPI_Allgather(long sendbuf, int sendcount, Pointer sendtype,
                          long recvbuf, int recvcount, Pointer recvtype,
                          Pointer comm);
        int MPI_Allgatherv(long sendbuf, int sendcount, Pointer sendtype,
                           long recvbuf, int[] recvcount, int[] displs, Pointer recvtype,
                           Pointer comm);
        int MPI_Initialized(int[] flag);
        int MPI_Finalized(int[] flag);
        int MPI_Init_thread(Pointer argv, Pointer argc, Pointer required,
    			int[] provided);
        int MPI_Finalize();
        int MPI_Comm_rank(Pointer comm, int[] rank);
        int MPI_Comm_size(Pointer comm, int[] size);
        int MPI_Barrier(Pointer comm);
        int MPI_Type_contiguous(int count, Pointer old, PointerByReference newType);
        int MPI_Type_commit(PointerByReference type);
        int MPI_Get_processor_name(byte[] name, int []resultlen);
        int MPI_Allreduce(double[] sendbuf, double[] recvbuf, int count, Pointer datatype, Pointer op, Pointer comm);
        int MPI_Allreduce(Memory sendbuf, Memory recvbuf, int count, Pointer datatype, Pointer op, Pointer comm);
        int MPI_Send(long buf, int count, Pointer datatype, int dest, int tag, Pointer comm);
        int MPI_Recv(long buf, int count, Pointer datatype, int dest, int tag, Pointer comm, Pointer status);
        int MPI_Ibcast(Memory buffer, int count, Pointer datatype, int root, Pointer comm, PointerByReference request);
        int MPI_Waitall(int count, Pointer[] requests, Pointer[] statuses);
        int MPI_Comm_split(Pointer comm, int color, int key, PointerByReference newcomm);

        int MPI_Bcast(float[] buffer, int count, Pointer datatype, int root, Pointer comm);
        int MPI_Bcast(double[] buffer, int count, Pointer datatype, int root, Pointer comm);
        int MPI_Bcast(byte[] buffer, int count, Pointer datatype, int root, Pointer comm);
        int MPI_Bcast(short[] buffer, int count, Pointer datatype, int root, Pointer comm);
        int MPI_Bcast(int[] buffer, int count, Pointer datatype, int root, Pointer comm);
        int MPI_Bcast(long[] buffer, int count, Pointer datatype, int root, Pointer comm);
    }

}
