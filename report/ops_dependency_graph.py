import os
import sys
import logging
import pickle
import javalang
import networkx as nx

logging.basicConfig(level=logging.DEBUG)

if len(sys.argv) != 2:
    print(f"Usage: {sys.argv[0]} path/to/built/imagej-ops")
    exit(1)

ops = {}
classess = {}
node_opts = {'shape': 'rect', 'style': 'filled'}

class C:
    def __init__(self, node, tree):
        self.node = node
        self.tree = tree

def p(path):
    logging.debug("processing %s", path)
    tree = javalang.parse.parse(open(path).read())
    for path, node in tree:
        if isinstance(node, javalang.tree.ClassDeclaration) or isinstance(node, javalang.tree.InterfaceDeclaration):
            p = [tree.package.name]
            for yy in path:
                if isinstance(yy, javalang.tree.ClassDeclaration):
                    p.append(yy.name)
            p.append(node.name)
            classess['.'.join(p)] = C(node, tree)
            logging.debug("Found: %s", '.'.join(p))

def parse_all():
    cache_path = '/tmp/ops_dependency_graph.cache'
    try:
        with open(cache_path, "rb") as f:
            return pickle.load(f)
    except FileNotFoundError:
        pass

    for d in ['target/generated-sources/', 'src/main']:
        for root, dirs, files in os.walk(os.path.join(sys.argv[1], d)):
            for f in files:
                if f.endswith('.java'):
                    p(os.path.join(root, f))
    
    with open(cache_path, "wb") as f:
        pickle.dump(classess, f)

    return classess

def resolve(name, tree):
    parts = []
    if isinstance(name, javalang.tree.ClassReference):
        if name.qualifier:
            parts += name.qualifier.split('.')
        name = name.type
    parts.append(name.name)

    if not name:
        return ""

    t = name
    while t:
        if not isinstance(t, javalang.tree.TypeArgument) and not isinstance(t, javalang.tree.ClassDeclaration) and not isinstance(t, javalang.tree.InterfaceDeclaration):
            if isinstance(name, javalang.tree.BasicType):
                return name.name
            t = t.sub_type
        else:
            break

    prefix = tree.package.name
    for imp in tree.imports:
        if parts[0] == imp.path.split('.')[-1]:
            prefix = '.'.join(imp.path.split('.')[:-1])

    return f"{prefix}.{'.'.join(parts)}"

classess = parse_all()
processed = set()
def process(fqname):
    if fqname in processed:
        return
    processed.add(fqname)
   
    if not fqname.startswith('net.imagej.ops'):
        return

    if fqname not in classess:
        logging.error("Missing %s", fqname)
        return
    node = classess[fqname]
    if isinstance(node.node, javalang.tree.InterfaceDeclaration):
        if node.node.extends:
            for interface in node.node.extends:
                fullname = resolve(interface, node.tree)
                if fullname == 'net.imagej.ops.Op':
                    ops[fqname] = [fqname]
                process(fullname)
                if fullname in ops:
                    if fqname not in ops:
                        ops[fqname] = []
                    ops[fqname].append(fullname)

    else:
        if node.node.implements:
            for impl in node.node.implements:
                fullname = resolve(impl, node.tree)
                process(fullname)
                if fullname in ops:
                    if fqname not in ops:
                        ops[fqname] = []
                    ops[fqname].append(fullname)

        if node.node.extends:
            for impl in node.node.extends:
                if impl[0] != ():
                    continue
                fullname = resolve(impl[1], node.tree)
                process(fullname)
                if fullname in ops:
                    if fqname not in ops:
                        ops[fqname] = []
                    ops[fqname].append(fullname)


for name, node in classess.items():
    process(name)
#process("net.imagej.ops.filter.min.MinFilterOp")

def is_op(cl):
    if resolve(cl.node, cl.tree) == 'net.imagej.ops.Op':
        return True

    if cl.node.extends:
        for c in cl.node.extends:
            if isinstance(c, tuple):
                if c[0] == ():
                    continue
                c = c[0][0]
            name = resolve(c, cl.tree)
            if name in classess and is_op(classess[name]):
                return True

    if isinstance(cl.node, javalang.tree.ClassDeclaration) and cl.node.implements:
        for c in cl.node.implements:
            if isinstance(c, tuple):
                if c[0] == ():
                    c = None
                c = c[0][0]
            if c:
                name = resolve(c, cl.tree)
                if name in classess and is_op(classess[name]):
                    return True

    return False

ignore = [
    'net.imagej.ops.math.',
    'net.imagej.ops.logic.',
    'net.imagej.ops.map.',
    'net.imagej.ops.create',
    'net.imagej.ops.Ops.Create.Im',
    'net.imagej.ops.lookup.',
    'net.imagej.ops.run.'
    'net.imagej.ops.eval.'
    'net.imagej.ops.special.'
]

G = nx.DiGraph()
G.graph['graph'] = {
    "ratio": "fill",
    "size": "8.3,10.7!",
    "margin": "0",
    'rankdir': 'LR',
    "ranksep": "5.5",
}
for cl in classess:
    if not is_op(classess[cl]):
        continue

    def is_allowed(cl):
        return True
        for i in ignore:
            if cl.startswith(i):
                return False
        return True

    print(cl)
    found_ops = set()
    for path, node in classess[cl].node:
        if isinstance(node, javalang.tree.ClassReference):
            op = resolve(node, classess[cl].tree)
            if op.startswith("net.imagej.ops") and not isinstance(path[-3], javalang.tree.Annotation):
                found_ops.add(op)
        elif isinstance(node, javalang.tree.ClassDeclaration) and node.implements:
            for cls in node.implements:
                op = resolve(cls, classess[cl].tree)
                if op in found_ops:
                    found_ops.remove(op)
        elif isinstance(node, javalang.tree.FieldDeclaration):
            if isinstance(node.type, javalang.tree.ReferenceType):
                name = resolve(node.type, classess[cl].tree)
                if name.startswith('net.imagej.ops.special'):
                    if node.annotations and node.annotations[0].name == 'Parameter':
                        found_ops.add(name)

    for op in found_ops:
        if is_allowed(cl) and is_allowed(op):
            G.add_node(cl, **node_opts)
            G.add_node(op, **node_opts)
            G.add_edge(cl, op)

print("===");

paralelized = [
    'Filter.Convolve',
    'Filter.Min',
    'Map',
    'Math.Add',
    'Math.Divide',
    'Math.Multiply',
    'Math.Subtract',
    'Stats.GeometricMean',
    'Stats.HarmonicMean',
    'Stats.Max',
    'Stats.Mean',
    'Stats.Min',
    'Stats.MinMax',
    'Stats.Moment1AboutMean',
    'Stats.Moment2AboutMean',
    'Stats.Moment3AboutMean',
    'Stats.Moment4AboutMean',
    'Stats.Size',
    'Stats.StdDev',
    'Stats.Sum',
    'Stats.SumOfInverses',
    'Stats.SumOfLogs',
    'Stats.SumOfSquares',
    'Stats.Variance',
    'Transform.Project',
]

def walk(G, node):
    for succ in G.predecessors(node):
        if 'fillcolor' not in G.nodes[succ]:
            G.nodes[succ]['fillcolor'] = 'yellow'
        walk(G, succ)

for op in paralelized:
    full_op = f'net.imagej.ops.Ops.{op}'
    if full_op not in G.nodes:
        G.add_node(full_op, **node_opts)
    G.nodes[full_op]['fillcolor'] = 'green'
    walk(G, full_op)

for node in list(G):
    if 'fillcolor' not in G.nodes[node]:
        G.remove_node(node)

for node in list(G):
    print(node)

short_labels = {fqn: fqn.split('.')[-1] for fqn in G} 
G = nx.relabel_nodes(G, short_labels)
nx.drawing.nx_agraph.write_dot(G, "ops_dependency_graph.dot")
