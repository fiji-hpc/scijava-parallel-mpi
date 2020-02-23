CFLAGS=-fPIC -Wall -O2
CC=mpicc

all: libscijava_parallel_mpi_native.so

libscijava_parallel_mpi_native.so: split.o utils.o mpirank.o minfilter.o
	$(CC) $(CFLAGS) -shared $^ -o $@

clean:
	rm *.so *.o
