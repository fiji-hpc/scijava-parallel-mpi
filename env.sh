export B_MAX_NODES=${B_MAX_NODES:-2}
export B_ROUNDS=${B_ROUNDS:-1}
export B_IMG_MODE=${B_IMG_MODE:-PLANAR}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export CLASSPATH=$(< "$DIR"/classpath)
export D="-agentlib:jdwp=transport=dt_socket,server=n,address=127.0.0.1:5005,suspend=y,onuncaught=n"

benchenv() {
    env | grep ^B_
}

benchrun() {
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
    echo "git commit: $(GIT_DIR=$DIR/.git git rev-parse HEAD)" >> info
    benchenv > env

		(cd ..; rm -f latest; ln -s "$date" latest)
		for nodes in $(seq "$B_MAX_NODES" -1 1); do
			output_dir="$nodes"
			mkdir -p "$output_dir"

			P="-agentpath:$HOME/profiler/libasyncProfiler.so=start,alluser,file=$output_dir/profile.%p.svg,interval=1ms"
			cmd="mpirun --bind-to none --oversubscribe -np $nodes java $P com.mycompany.imagej.Main $op  ../../datasets/$dataset $nodes/result.tiff"
			(echo $cmd; $cmd < /dev/null) |& tee -a out
		done
	)
}

benchpostprocess() (
  cd "$DIR/run/outputs"
  for result in */; do
    if [ ! -f "$result/checksums" ]; then
      find "$result" -name "result.tiff" -exec md5sum {} \; > "$result/checksums"
    fi

    find "$result" -name "result.tiff" | while read -r result_image; do
      thumbnail_path="${result_image%/*}/result.thumbnail.png"
      if [ ! -f "$thumbnail_path" ]; then
        taskset -c 1 convert "$result_image" -resize 500x "$thumbnail_path"
      fi
    done
  done
)

benchreports() {
  path="$DIR/run/outputs/"
  find "$path" -maxdepth 1 ! -path "$path" -type d | while read -r report_dir; do
    B_RESULT_DIR="$report_dir" jupyter nbconvert --execute --to html --stdout "$DIR/report.ipynb" > "$report_dir/report.html"
  done
}
