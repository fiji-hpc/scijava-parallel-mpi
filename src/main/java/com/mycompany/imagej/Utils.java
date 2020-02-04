package com.mycompany.imagej;


public class Utils {
    public static void print(Object s) {
        System.out.println(MPIUtils.getRank() + ": " + s.toString());
    }

    public static void rootPrint(Object s) {
        if(MPIUtils.isRoot()) {
            System.out.println(s.toString());
        }
    }
}
