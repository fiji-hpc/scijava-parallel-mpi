export MAX_NODES=${MAX_NODES:-2}
export ROUNDS=${ROUNDS:-1}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export CLASSPATH=$(< "$DIR"/classpath)

run() {
  op=$1
	dataset=$2

  if [ -z "$dataset" ] || [ -z "$op" ]; then
    echo "Usage: run operation dataset"
    return 1
  fi

	date=$(date +%Y_%m_%d_%H_%M_%S)
	(
		mkdir -p "$DIR/run/outputs/$date"
		cd "$DIR/run/outputs/$date"
    echo "$op $dataset" > info

		(cd ..; rm -f latest; ln -s "$date" latest)
		for nodes in $(seq "$MAX_NODES" -1 1); do
			output="$nodes/$dataset"
			output_dir="$(dirname "$output")"
			mkdir -p "$output_dir"

			P="-agentpath:$HOME/profiler/libasyncProfiler.so=start,alluser,file=$output_dir/profile.%p.svg"
			cmd="mpirun --oversubscribe -np $nodes java $P com.mycompany.imagej.Main $op  ../../datasets/$dataset $nodes/result.tiff"
			(echo $cmd; $cmd < /dev/null) |& tee -a out
		done
	)
}
