package cz.it4i.scijava.mpi;


public class Utils {
    public static void print(Object s) {
        System.out.println(MPIUtils.getRank() + ": " + s.toString());
    }

    public static void rootPrint(Object s) {
        if(MPIUtils.isRoot()) {
            System.out.println(s.toString());
        }
    }

    public static int numThreads() {
        if(System.getenv("B_THREADS_NUM") != null) {
            return Integer.parseInt(System.getenv("B_THREADS_NUM"));
        }

        return Runtime.getRuntime().availableProcessors();
    }
}
