#!/bin/bash
set -e

MPI_ARGS="--bind-to none --oversubscribe"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
OUT="$DIR/run/benchmark/"

source "$DIR/env.sh"

trap 'exit 1' INT

mkdir -p "$OUT"
cd $OUT || exit 1

benchmark() (
  OP=$1
  METHOD=$2
  INPUT=$3
  SCRIPT=$OP

  if [ -n "$4" ]; then
    suffix="_$4"
  fi

  if [ "$METHOD" = "default" ]; then
    export B_NO_MPI_OPS=1
    export B_NODES=1
  elif [ "$METHOD" = "clij" ]; then
    SCRIPT="${SCRIPT}clij"
    export B_NODES=1
  elif [ "$METHOD" = "mpisingle" ]; then
    export B_THREADS_NUM=1
  elif [ "$METHOD" != "mpi" ]; then
    echo "Unknown method: $METHOD"
    exit 1
  fi

  NAME=${OP}_${INPUT}${suffix}_${METHOD}

  if [ -f "$NAME.csv" ]; then
    echo "[ SKIP] $NAME"
    return
  fi
  echo "[BENCH] $NAME"

  rm -f "$NAME*" stats.csv

  if [ -n "$B_NODES" ]; then
    B_MAX_NODES=$B_NODES
    B_MIN_NODES=$B_NODES
  fi
  env | grep '^B_' | sed 's/^/export /' > "$NAME.env"
  for nodes in $(seq "$B_MAX_NODES" -1 "$B_MIN_NODES"); do
    echo "Running $nodes"
    cmd="mpirun $MPI_ARGS -np $nodes $HOME/Fiji.app/ImageJ-linux64 --ij2 --headless --run $DIR/scripts/$SCRIPT.py input_path=\"$DIR/run/datasets/$INPUT.tif\",output_path=\"$NAME.$nodes.tif\",rounds=\"$B_ROUNDS\""
    date >> "$NAME.out"
    (echo $cmd; $cmd) |& tee -a "$NAME.out"

    if [ "$(tail -n1 "$NAME.out")" != "OK" ]; then
      echo FAIL
      exit 1
    fi
    date >> "$NAME.out"
  done
  mv stats.csv "$NAME.csv"
)


export B_MAX_NODES=8
export B_ROUNDS=1


benchmark minfilter default fused_tp_0_ch_0
benchmark minfilter clij fused_tp_0_ch_0
benchmark minfilter mpi fused_tp_0_ch_0
benchmark minfilter mpisingle fused_tp_0_ch_0


benchmark min default fused_tp_0_ch_0
benchmark min mpi fused_tp_0_ch_0
benchmark min mpisingle fused_tp_0_ch_0


benchmark convolution default fused_tp_0_ch_0
benchmark convolution mpi fused_tp_0_ch_0
benchmark convolution clij fused_tp_0_ch_0
#benchmark minfilter mpisingle fused_tp_0_ch_0
