import pandas
import os
import shlex
import subprocess
import sys
import tempfile
import time
import argparse

ROUNDS = 10
MPI_ARGS = shlex.split("--bind-to none --tag-output --timestamp-output --merge-stderr-to-stdout")
DIR = os.path.dirname(os.path.realpath(__file__))
OUTPUT_DIR = os.path.join(DIR, "run", "benchmark")
INPUT_DIR = os.path.join(DIR, "run", "datasets")

class Benchmark:
    def __init__(self, op, method, dataset, rank_size, suffix='', env=None):
        self.op = op
        self.method = method
        self.dataset = dataset
        self.suffix = suffix
        self.rank_size = rank_size
        self.env = env
        if not self.env:
            self.env = {}
        self.columns = ['stat', 'rank', 'size', 'round', 'time_ms']

    @property
    def name(self):
        return "_".join([
            self.op,
            self.dataset,
            self.suffix + self.method,
        ])

    @property
    def csv_path(self):
        return os.path.join(OUTPUT_DIR, self.name + '.csv')

    def to_pandas(self):
        data = pandas.read_csv(self.csv_path, names=self.columns)
        return data[data['size'] == self.rank_size]

    def check(self):
        missing_runs = set(range(0, ROUNDS))
        corrupted_runs = set()

        try:
            d = self.to_pandas()
        except FileNotFoundError:
            return missing_runs, set()

        x = d[d['stat'] == 'total_op'].groupby(['round'])['stat'].count()
        for run, measurements in x.iteritems():
            if measurements != self.rank_size:
                corrupted_runs.add(run)
            else:
                missing_runs.remove(run)


        return missing_runs, corrupted_runs

    def status(self):
        print(self.name)
        print(f"\tworld_size={self.rank_size}")
        missing, corrupted = self.check()
        if missing:
            print(f"\t{missing=}")
        if corrupted:
            print(f"\t{corrupted=}")
        print()
        return missing, corrupted

    def benchmark_remaining(self, dry_run=False, fail_on_error=False, single=False):
        missing_runs = self.check()[0]
        for run in missing_runs:
            success = self.make_benchmark(run, dry_run)
            if not success and fail_on_error:
                raise Exception("Failed")
            if single:
              exit(0)

    def make_benchmark(self, run, dry_run=False):
        print(f"Benchmarking {self.name} nodes={self.rank_size} #{run}")
        if dry_run:
            return True

        fire_event("benchmark_started", self, run)
        start = time.time()
        with tempfile.NamedTemporaryFile(dir=OUTPUT_DIR) as f:
            stats_path = f.name
            errors, output = self.run_fiji(stats_path)

            data = pandas.read_csv(stats_path, names=self.columns)
            op_data = data[data['stat'] == 'total_op']
            if len(op_data) != self.rank_size:
                errors.append(f'wrong num of measurements: {len(op_data)} != {self.rank_size}')

            if sorted(list(op_data['rank'])) != list(range(0, self.rank_size)):
                errors.append(f'wrong num of measurements: {list(op_data["rank"])}')

            if not errors:
                data['round'] = list(self.check()[0])[0]
                data.to_csv(self.csv_path, mode='a', header=False, index=False)

            print(errors)
            print(data)

            fire_event("benchmark_finished", self, errors, output, time.time() - start)
            return False if errors else True


    def run_fiji(self, stats_path):
        env = {**os.environ.copy(), **self.env}
        env['B_STATS_PATH'] = stats_path

        script = self.op
        if self.method == 'default':
            env['B_NO_MPI_OPS'] = 1
        elif self.method == 'clij':
            self.op += 'clij'
        elif self.method == 'mpisingle':
            env['B_THREADS_NUM'] = 1
            env['OMP_NUM_THREADS'] = 1
        elif self.method != 'mpi':
            raise Exception(f"Unknown method: {self.method}")

        cmd = list(map(str, [
            "mpirun",
            *MPI_ARGS,
            "-np", self.rank_size,
            os.path.expanduser("~/Fiji.app/ImageJ-linux64"),
            "--ij2", "--headless",
            "--run", f"{DIR}/scripts/{script}.py",
            f"input_path=\"{INPUT_DIR}/{self.dataset}.tif\",output_path=\"{OUTPUT_DIR}/{self.name}.{self.rank_size}.tif\",rounds=\"1\""
        ]))

        with open(os.path.join(OUTPUT_DIR, f"{self.name}.out"), "a") as f:
            f.write("\n\n\n\n")
            f.write(shlex.join(cmd))
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, env={k: str(v) for k, v in env.items()})
            output = ""
            while True:
                data = p.stdout.read(1024).decode('utf-8')
                if not data:
                    break
                sys.stdout.write(data)
                f.write(data)
                output += data
            p.wait()

            errors = []
            def add_error(name, fn):
                if not fn():
                    errors.append(name)

            add_error('missing OK', lambda: output.strip().endswith('OK'))
            add_error('exit code', lambda: p.returncode == 0)

            for word in ['error', 'exception', 'failed']:
                add_error(word, lambda: word not in output.strip())

            print(f"exit code = {p.returncode}")
            return errors, output

class All:
    def __init__(self):
        self.benchmarks = []

    def add(self, op, methods, datasets, ranks, suffix='', env=None):
        for method in methods:
            for dataset in datasets:
                for rank in ranks:
                    self.benchmarks.append(Benchmark(
                        op=op,
                        method=method,
                        dataset=dataset,
                        rank_size=rank
                    ))

    def benchmark_remaining(self, nodes, dry_run=False, fail_on_error=False, single=False):
        for b in self.benchmarks:
            if b.rank_size == nodes:
                b.benchmark_remaining(dry_run, fail_on_error, single)

    def status(self):
        remaining = {}
        for b in self.benchmarks:
            rem, corrupted = b.status()

            if b.rank_size not in remaining:
              remaining[b.rank_size] = 0

            remaining[b.rank_size] += len(rem) + len(corrupted)

        print(remaining)


events = {
    'benchmark_started': [],
    'benchmark_finished': [],
}
def Event(name):
    def decorator(fn):
        events[name].append(fn)
        return fn
    return decorator

def fire_event(name, *args, **kwargs):
    for fn in events[name]:
        fn(*args, **kwargs)

with open("notify.py") as f:
    exec(f.read())


parser = argparse.ArgumentParser()
parser.add_argument('action')
parser.add_argument('--dry-run', action='store_true')
parser.add_argument('--nodes', required=True, type=int)
parser.add_argument('--fail-on-error', action='store_true')
parser.add_argument('--single', action='store_true')
args = parser.parse_args()


all_methods = ['default', 'clij', 'mpi', 'mpisingle']
def even_nodes(max_nodes):
  return [1] + list(range(2, max_nodes + 1, 2))

def datasets(fmt, nums):
  return [fmt.format(i=i) for i in nums]

b = All()
b.add(
    op='convolution',
    methods=['mpi'],
    ranks=even_nodes(16),
    datasets=datasets('test_2048x2048x{i}x{i}x{i}', [1] + list(range(2, 19, 2))),
)

if args.action == 'benchmark':
    b.benchmark_remaining(
        args.nodes,
        dry_run=args.dry_run,
        fail_on_error=args.fail_on_error,
        single=args.single,
    )
elif args.action == 'status':
    b.status()
else:
  print("unknown action")

