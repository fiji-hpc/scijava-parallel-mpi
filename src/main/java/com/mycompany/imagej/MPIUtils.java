package com.mycompany.imagej;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.nio.ByteBuffer;

public class MPIUtils {
    public static Pointer MPI_BYTE;
    public static Pointer MPI_FLOAT;
    public static Pointer MPI_MIN;
    public static Pointer MPI_MAX;
    public static Pointer MPI_COMM_WORLD;

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
        checkMpiResult(MPILibrary.INSTANCE.MPI_Init(null, null));

        MPI_BYTE = getSymbolPtr("ompi_mpi_byte");
        MPI_COMM_WORLD = getSymbolPtr("ompi_mpi_comm_world");
        MPI_FLOAT = getSymbolPtr("ompi_mpi_float");
        MPI_MIN = getSymbolPtr("ompi_mpi_op_min");
        MPI_MAX = getSymbolPtr("ompi_mpi_op_max");
    }

    public static void Finalize() {
        MPILibrary.INSTANCE.MPI_Finalize();
    }

    public static int getSize() {
        int[] rank = new int[1];
        checkMpiResult(MPILibrary.INSTANCE.MPI_Comm_size(MPI_COMM_WORLD, rank));
        return rank[0];
    }

    public static int getRank() {
        int[] rank = new int[1];
        checkMpiResult(MPILibrary.INSTANCE.MPI_Comm_rank(MPI_COMM_WORLD, rank));
        return rank[0];
    }

    public static void barrier() {
        checkMpiResult(MPILibrary.INSTANCE.MPI_Barrier(MPI_COMM_WORLD));
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

    public static void Allreduce(float[] sendbuf, float[] recvbuf, int count, Pointer datatype, Pointer op, Pointer comm) {
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
        int MPI_Init(Pointer argv, Pointer argc);
        int MPI_Finalize();
        int MPI_Comm_rank(Pointer comm, int[] rank);
        int MPI_Comm_size(Pointer comm, int[] size);
        int MPI_Barrier(Pointer comm);
        int MPI_Type_contiguous(int count, Pointer old, PointerByReference newType);
        int MPI_Type_commit(PointerByReference type);
        int MPI_Get_processor_name(byte[] name, int []resultlen);
        int MPI_Allreduce(float[] sendbuf, float[] recvbuf, int count, Pointer datatype, Pointer op, Pointer comm);
        int MPI_Send(long buf, int count, Pointer datatype, int dest, int tag, Pointer comm);
        int MPI_Recv(long buf, int count, Pointer datatype, int dest, int tag, Pointer comm, Pointer status);
        int MPI_Bcast(ByteBuffer buffer, int count, Pointer datatype, int root, Pointer comm);
    }

}