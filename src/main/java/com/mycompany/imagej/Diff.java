package com.mycompany.imagej;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;


public class Diff {
    public static void main(String[] args) {
        try {
            ImageJ ij = new ImageJ();

            Dataset leftDataset = ij.scifio().datasetIO().open(args[0]);
            Dataset rightDataset = ij.scifio().datasetIO().open(args[1]);

            boolean print = false;
            if(args.length >= 3) {
                print = args[2].equals("--print");
            }
            boolean match = diff(leftDataset, rightDataset, print);
            System.exit(match ? 0 : 1);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String pad(Object o) {
        return String.format("%4s", o);
    }

    private static <T> boolean diff(RandomAccessibleInterval<T> leftDataset, RandomAccessibleInterval<T> rightDataset, boolean print) {
        Cursor<T> left = Views.flatIterable(leftDataset).cursor();
        Cursor<T> right = Views.flatIterable(rightDataset).cursor();

        int i = 0;
        StringBuilder buf = new StringBuilder();
        boolean lineEquals = true;
        boolean haveLeft = true;
        boolean haveRight = true;
        boolean globalMatch = true;

        while(haveLeft || haveRight) {
            left.fwd();
            right.fwd();

            if(haveLeft && haveRight) {
                if (!left.get().equals(right.get())) {
                    lineEquals = false;
                    globalMatch = false;
                }
            } else {
                lineEquals = false;
                globalMatch = false;
            }

            if(print) {
                System.out.print((pad(haveLeft ? left.get() : "")) + " ");
                buf.append(pad(haveRight ? right.get() : " ")).append(" ");

                i++;
                if (i % 8 == 0) {
                    System.out.print(lineEquals ? "       " : "  WRONG  ");
                    System.out.print(buf.toString());
                    buf.setLength(0);
                    System.out.print('\n');
                    lineEquals = true;
                }
            }

            haveLeft = left.hasNext();
            haveRight = right.hasNext();
        }

        System.out.println();

        System.out.println(globalMatch ? "OK" : "NOT EQUALS");
        return globalMatch;
    }
}
