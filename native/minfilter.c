#include "utils.h"
#include "split.h"

#define at(x, y, z) input[(z) * dims[1] * dims[0] + (y) * dims[0] + (x)]

void minfilter(uint8_t *output, uint8_t *input, int64_t *dims, int n_dims, int neighSize) {
    struct split split;
    split_init(&split, output, numElements(dims, n_dims));

    uint64_t begin, end;
    split_myblock_pos(&split, &begin, &end);
    #pragma omp parallel for
    for(uint64_t cur = begin; cur < end; cur++) {
      int64_t Z = cur / (dims[0] * dims[1]);
      int64_t Y = (cur % (dims[0] * dims[1])) / dims[0];
      int64_t X = (cur % (dims[0] * dims[1])) % dims[0];

      uint8_t min = 255;
      for(int64_t x = X - neighSize; x <= X + neighSize; x++) {
        for(int64_t y = Y - neighSize; y <= Y + neighSize; y++) {
          for(int64_t z = Z - neighSize; z <= Z + neighSize; z++) {
            if(x < 0 || y < 0 || z < 0 || x >= dims[0] || y >= dims[1] || z >= dims[2]) {
                continue;
            }

            uint8_t val = at(x, y, z);
            if(val < min) {
              min = val;
            }
          }
        }
      }
      output[cur] = min;
    }

    split_sync(&split);
    split_free(&split);
}


/*
int mpirank(uint8_t *output, uint8_t *input, int64_t *dims, int n_dims) {

    int rankSize = world_size();
    int myRank = world_rank();

    uint64_t totalElements = numElements(dims, n_dims);
    uint64_t perNode = (totalElements + rankSize) / rankSize;

    uint64_t myOffset = perNode * myRank;
    uint64_t myEnd = MIN(myOffset + perNode, totalElements);

    uint8_t *cur = output + myOffset;
    uint8_t *end = output + myEnd - 1;


    uint8_t color_offset = 10;
    uint8_t val = color_offset + (255 - color_offset) * myRank / rankSize;
    printf("%d %d %d\n", myOffset, myEnd, val);
    while(cur != end) {
        *cur = val;
        cur++;
    }

    int recvcounts[rankSize];
    int displs[rankSize];
    for(int i = 0; i < rankSize; i++) {
        recvcounts[i] = perNode;
        displs[i] = i * perNode;
    }
    recvcounts[rankSize - 1] = MIN(displs[rankSize - 1] + perNode, totalElements) - displs[rankSize - 1];

    MPI_CHECK(MPI_Allgatherv(
        output + myOffset,
        myEnd - myOffset,
        MPI_BYTE,
        output,
        recvcounts,
        displs,
        MPI_BYTE,
        MPI_COMM_WORLD
    ));
}

*/
