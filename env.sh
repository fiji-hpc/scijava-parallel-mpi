export B_MAX_NODES=${B_MAX_NODES:-2}
export B_MIN_NODES=${B_MIN_NODES:-1}
export B_NODES=${B_NODES}
export B_ROUNDS=${B_ROUNDS:-1}
export B_IMG_MODE=${B_IMG_MODE:-PLANAR}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export CLASSPATH=$(< "$DIR"/classpath)
export D="-agentlib:jdwp=transport=dt_socket,server=n,address=127.0.0.1:5005,suspend=y,onuncaught=n"

benchenv() {
    env | grep ^B_
}

bench_report_dirs() {
  path="${1:-$DIR/run/outputs}"
  find "$path" -name "stats.csv" | while read -r stats_path; do
    dirname "$stats_path"
  done | sort
}

benchrun() {
  op=$1
	dataset=$2

  if [ -z "$dataset" ] || [ -z "$op" ]; then
    echo "Usage: run operation dataset"
    return 1
  fi

	(
    if [ -z "$B_OUTPUT_DIR" ]; then
      date=$(date +%Y_%m_%d_%H_%M_%S)
      B_OUTPUT_DIR=$DIR/run/outputs/$date
    fi

		mkdir -p "$B_OUTPUT_DIR"
    cd "$B_OUTPUT_DIR"
    echo "$op $dataset" > info
    echo "git commit: $(GIT_DIR=$DIR/.git git rev-parse HEAD)" >> info
    benchenv | sed 's/^/export /' > env

    if [ ! -z "$date" ];then
  		(cd ..; rm -f latest; ln -s "$date" latest)
    fi

    if [ -n "$B_NODES" ]; then
      B_MAX_NODES=$B_NODES
      B_MIN_NODES=$B_NODES
    fi
		for nodes in $(seq "$B_MAX_NODES" -1 "$B_MIN_NODES"); do
			output_dir="$nodes"
			mkdir -p "$output_dir"

			P="-agentpath:$HOME/profiler/libasyncProfiler.so=start,alluser,file=$output_dir/profile.%p.svg,interval=1ms"
			cmd="mpirun --bind-to none --oversubscribe -np $nodes java $P com.mycompany.imagej.Main $op  $DIR/run/datasets/$dataset $nodes/result.tiff"
			(
        echo "## $nodes nodes"
        echo $cmd;
        $cmd < /dev/null
      ) |& tee -a out
		done
	)
}

benchpostprocess() (
  for report_dir in $(bench_report_dirs "$1"); do
    echo "$report_dir"

    if [ ! -f "$report_dir/checksums" ]; then
      find "$report_dir" -name "result.tiff" -exec md5sum {} \; > "$report_dir/checksums"
    fi

    find "$report_dir" -name "result.tiff" | while read -r result_image; do
      thumbnail_path="${result_image%/*}/result.thumbnail.png"
      if [ ! -f "$thumbnail_path" ]; then
        taskset -c 1 convert "$result_image" -resize 500x "$thumbnail_path"
      fi
    done
  done
)

benchreports() {
  for report_dir in $(bench_report_dirs "$1"); do
    echo $report_dir
    B_RESULT_DIR="$report_dir" jupyter nbconvert --ExecutePreprocessor.timeout=600 --execute --to html --stdout "$DIR/report.ipynb" > "$report_dir/report.html"
  done
}

benchlist() {
  for report in $(bench_report_dirs "$1"); do
    basename "$report"
    cat "$report/info"
    cat "$report/env"
    echo
  done
}
