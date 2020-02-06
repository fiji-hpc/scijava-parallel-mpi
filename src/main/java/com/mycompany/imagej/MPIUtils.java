package com.mycompany.imagej;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

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
        int MPI_Init(Pointer argv, Pointer argc);
        int MPI_Finalize();
        int MPI_Comm_rank(Pointer comm, int[] rank);
        int MPI_Comm_size(Pointer comm, int[] size);
        int MPI_Barrier(Pointer comm);
        int MPI_Type_contiguous(int count, Pointer old, PointerByReference newType);
        int MPI_Type_commit(PointerByReference type);
        int MPI_Get_processor_name(byte[] name, int []resultlen);
        int MPI_Allreduce(double[] sendbuf, double[] recvbuf, int count, Pointer datatype, Pointer op, Pointer comm);
        int MPI_Send(long buf, int count, Pointer datatype, int dest, int tag, Pointer comm);
        int MPI_Recv(long buf, int count, Pointer datatype, int dest, int tag, Pointer comm, Pointer status);
        int MPI_Ibcast(Memory buffer, int count, Pointer datatype, int root, Pointer comm, PointerByReference request);
        int MPI_Waitall(int count, Pointer[] requests, Pointer[] statuses);

        int MPI_Bcast(float[] buffer, int count, Pointer datatype, int root, Pointer comm);
        int MPI_Bcast(double[] buffer, int count, Pointer datatype, int root, Pointer comm);
        int MPI_Bcast(byte[] buffer, int count, Pointer datatype, int root, Pointer comm);
        int MPI_Bcast(short[] buffer, int count, Pointer datatype, int root, Pointer comm);
        int MPI_Bcast(int[] buffer, int count, Pointer datatype, int root, Pointer comm);
        int MPI_Bcast(long[] buffer, int count, Pointer datatype, int root, Pointer comm);
    }

}
