#include <omp.h>
#include "utils.h"
#include "split.h"

#define at(x, y, z) input[(z) * dims[1] * dims[0] + (y) * dims[0] + (x)]

void minfilter(uint8_t *output, uint8_t *input, int64_t *dims, int n_dims, int neighSize) {
  struct split split;
  split_init(&split, output, numElements(dims, n_dims));

  uint64_t node_begin, node_end;
  split_myblock_pos(&split, &node_begin, &node_end);

  #pragma omp parallel
  {
    int num_threads = omp_get_num_threads();
    uint64_t node_elements = node_end - node_begin;
    uint64_t per_thread = (node_elements + num_threads) / num_threads;
    uint64_t begin = node_begin + per_thread * omp_get_thread_num();
    uint64_t end = MIN(begin + per_thread, node_end);

    int64_t Z = begin / (dims[0] * dims[1]);
    int64_t Y = (begin % (dims[0] * dims[1])) / dims[0];
    int64_t X = (begin % (dims[0] * dims[1])) % dims[0];

    for(uint64_t cur = begin; cur < end; cur++) {
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

      X++;
      if(X >= dims[0]) {
        X = 0;
        Y++;
        if(Y >= dims[1]) {
          Y = 0;
          Z++;
        }
      }
    }
  }

  split_sync(&split);
  split_free(&split);
}
