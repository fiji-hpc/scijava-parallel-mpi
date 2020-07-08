#!/usr/bin/env python3
import random
import sys
import os

class Op:
    def __init__(self, name=''):
        self.name = name
        self.childs = {}
        self.time_ms = 0
        self.parent = None
 
    def __str__(self):
        return str(self.name)

if len(sys.argv) != 2:
    print("Usage: ops_time_graph.py path_to.csv.0")
    exit(1)

input_path = sys.argv[1]
output_path = f"{os.path.basename(input_path)}.html"

root = Op()
with open(input_path) as f:
    for line in f:
        parts = line.split(',')
        s = parts[0]
        ms = int(parts[-1].strip())

        p = s.split(';')
        parent = root
        while p:
            cur = p.pop(0)
            if cur not in parent.childs:
                parent.childs[cur] = Op(cur)
                parent.childs[cur].parent = parent

            parent = parent.childs[cur]

        parent.time_ms = ms

root.time_ms = sum([n.time_ms for n in root.childs.values()])

def color():
    r = int(205 + 50 * random.uniform(0.0, 1.0))
    g = int(230 * random.uniform(0.0, 1.0))
    b = int(55 * random.uniform(0.0, 1.0))

    return f"#{r:02x}{g:02x}{b:02x}"

def walk(node, f):
    if node.parent:
        percent = round(node.time_ms / node.parent.time_ms*100, 2)
        f.write(f"<div style='width: {percent}%;' title='{node.name}, {node.time_ms} ms, {percent}%' class='{'' if node.childs else 'leaf'}'>")

        #f.write(f"<div class='name' style='background: {color()}'>{node.name.split('.')[-1]}</div>")
        f.write(f"<div class='name' style='background: {color()}'>{node.name}</div>")
        #f.write(" " + str(node.time_ms))
    else:
        f.write(f"<div title='{node.name}'>")
        f.write(node.name)

    if node.childs:
        f.write("<div class='rows'>")
        for child in node.childs.values():
            walk(child, f)
        f.write('</div>')
    f.write("</div>")

with open(output_path, "w") as f:
    f.write("""
    <script>
    document.addEventListener('click', (evt) => {
        if(!evt.target.classList.contains('name')) {
            return;
        }

        if(evt.target.classList.contains('up')) {
            document.querySelector('.full').hidden = false;
            document.querySelector('.zoomed').hidden = true;
            return;
        }

        let clone = evt.target.parentNode.cloneNode(true);
        clone.style.width = '100%';

        document.querySelector('.full').hidden = true;

        let zoomed = document.querySelector('.zoomed')
        zoomed.innerHTML = '<div class="name up">up</div.';
        zoomed.appendChild(clone);
        zoomed.hidden = false;
    });
    </script>
    <style>
    div {
      overflow: hidden;
      white-space: nowrap;
      margin: 0;
      padding: 0;
    }
    .name {
        border: 1px solid white;
        border-right: 0px;
        border-top: 0px;
        border-left: 1px solid white;
        border-radius: 4px;
        height: fit-content;
        cursor: pointer;
        padding: 1px;
        direction: rtl;
        text-overflow: ellipsis;
        text-align: left;
    }
    
    .name:hover {
        font-weight: bold;
    }
    
    .up {
        text-align: center;
    }
    
    div.rows {
      display: flex;
    }
    
    .rows:first-of-type {
        border-top: 1px solid white;
    }
    
    .rows div:last-of-type .name {
        border-right: 1px solid white;
    }
  </style>    
    """)
    f.write("<div class='full'>")
    walk(root, f)
    f.write("</div>")
    f.write("<div class='zoomed'></div>")
