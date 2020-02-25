#include <omp.h>
#include "utils.h"
#include "split.h"

#define at(x, y, z) input[(z) * dims[1] * dims[0] + (y) * dims[0] + (x)]
#define unlikely(x)     __builtin_expect(!!(x), 0)



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

    int64_t pos[n_dims];
    index_to_pos(begin, pos, dims, n_dims);
    
    if(n_dims == 3) {
      for(uint64_t cur = begin; cur < end; cur++) {
        uint8_t min = 255;
        for(int64_t z = pos[2] - neighSize; z <= pos[2] + neighSize; z++) {
          if(z < 0 || z >= dims[2]) continue;
          for(int64_t y = pos[1] - neighSize; y <= pos[1] + neighSize; y++) {
            if(y < 0 || y >= dims[1]) continue;
            for(int64_t x = pos[0] - neighSize; x <= pos[0] + neighSize; x++) {
              if(x < 0 || x >= dims[0]) continue;

              uint8_t val = at(x, y, z);
              if(val < min) {
                min = val;
              }
            }
          }
        }
        output[cur] = min;
        fwd(pos, dims, n_dims);
      }
    } else if(n_dims == 5) {
      for(uint64_t cur = begin; cur < end; cur++) {
        uint8_t min = 255;
        for(int64_t t = pos[4] - neighSize; t <= pos[4] + neighSize; t++) {
          if(unlikely(t < 0 || t >= dims[4])) continue;
          for(int64_t c = pos[3] - neighSize; c <= pos[3] + neighSize; c++) {
            if(unlikely(c < 0 || c >= dims[3])) continue;
            for(int64_t z = pos[2] - neighSize; z <= pos[2] + neighSize; z++) {
              if(unlikely(z < 0 || z >= dims[2])) continue;
              for(int64_t y = pos[1] - neighSize; y <= pos[1] + neighSize; y++) {
                if(unlikely(y < 0 || y >= dims[1])) continue;
                for(int64_t x = pos[0] - neighSize; x <= pos[0] + neighSize; x++) {
                  if(unlikely(x < 0 || x >= dims[0])) continue;

                  uint64_t idx = t * dims[3] * dims[2] * dims[1] * dims[0]
                               + c * dims[2] * dims[1] * dims[0]
                               + z * dims[1] * dims[0]
                               + y * dims[0]
                               + x;

                  uint8_t val = input[idx];
                  if(unlikely(val < min)) {
                    min = val;
                  }
                }
              }
            }
          }
        }
        output[cur] = min;
        fwd(pos, dims, n_dims);
      }
    }
  }

  split_sync(&split);
  split_free(&split);
}
