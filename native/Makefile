CFLAGS=-fPIC -Wall -fopenmp -g -O3 -march=native -mtune=native
CC=mpicc

all: libscijava_parallel_mpi_native.so

libscijava_parallel_mpi_native.so: split.o utils.o mpirank.o minfilter.o add.o
	$(CC) $(CFLAGS) -shared $^ -o $@

clean:
	rm *.so *.o
