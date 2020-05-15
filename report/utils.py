import pandas as pd
import matplotlib.pyplot as plt
import os
import re
from IPython.display import display, Markdown

def load_dataset(path, regexp):
    columns = ['stat', 'rank', 'size', 'round', 'time_ms']
    data = pd.DataFrame(columns=columns + ['method'])
    for file in os.listdir(path):
        if not file.endswith('.csv') or file == 'stats.csv' or not re.match(regexp, file):
            continue

        d = pd.read_csv(os.path.join(path, file), names=columns)
        d['method'] = file.replace('.csv', '').split('_')[-1]
        d['image_size'] = int(file.replace('.csv', '').split('_')[-2].split('x')[-1])

        data = pd.concat([data, d])
        
    data.reset_index()

    for t in ['rank', 'size', 'round', 'time_ms']:
        data[t] = data[t].astype('int')
        
    return data

def aggregate(data, method, stat, aggfunc, print=False, count_check=10):
    disp = display if print else lambda x: ()    
    
    disp(Markdown("## All measured data"))
    disp(data[(data['stat'] == stat) & (data['method'] == method)].pivot_table('time_ms', columns=['size'], index=['image_size', 'round', 'rank']))
    
    disp(Markdown("## Max node time for each run/round"))
    maxes = data[(data['stat'] == stat) & (data['method'] == method)].pivot_table('time_ms', columns=['size'], index=['image_size', 'round'], aggfunc='max')
    disp(maxes)
    
    counts = maxes.reset_index().melt(id_vars=['image_size', 'round'], value_name='time_ms').pivot_table('time_ms', columns=['size'], index=['image_size'], aggfunc='count')
    disp(counts)
    if count_check is not None and (counts != count_check).any().any():
        display(counts)
        raise Exception("Missing measurements")
    
    return maxes.reset_index().melt(id_vars=['image_size', 'round'], value_name='time_ms').pivot_table('time_ms', columns=['size'], index=['image_size'], aggfunc=aggfunc)

def plot_quantiles(data, method, stat, legend=None, **kwargs):
    p2, p5, p8 = [(aggregate(data, method=method, stat=stat, aggfunc=lambda x: x.quantile(q), **kwargs)/1000).T for q in [0.2, 0.5, 0.8]]
    plt.plot(p5, marker='.')

    for i, s in enumerate(p8.columns):
        plt.fill_between(p8.index, p8[s], p2[s], color=plt.rcParams['axes.prop_cycle'].by_key()['color'][i], alpha=0.5)
    plt.legend([legend.format(n=int(n)) for n in p8.columns], loc='upper left', bbox_to_anchor=(1,1))
    plt.ylabel('time [s]')
    plt.xlabel('number of nodes')
    
def plot_surface(d, legend):
    fig = plt.figure()
    ax = fig.gca(projection='3d')
    ax.plot_trisurf(d['size'], d['image_size'], d['time_ms'] / 1000)
    ax.set_xlabel('nodes')
    ax.set_ylabel(legend.format(n='N'))
    ax.set_zlabel('time [s]')
    
def plot_common(data, algorithm, method, legend):
    display(Markdown("## Speedup"))
    medians = aggregate(data, method=method, stat='total_op', aggfunc='median', print=False)
    (1 / medians.div(medians[1], axis=0)).T.plot(marker='.')
    plt.plot(medians.columns, medians.columns, label='ideal', marker='.')
    plt.xlabel('number of nodes')
    plt.ylabel('speedup')
    plt.legend([legend.format(n=int(float(i))) if str.isdigit(i[0]) else i for i in map(lambda x: x.get_text(), plt.legend().get_texts())], bbox_to_anchor=(1,1))
    plt.savefig(f'figures/{algorithm}_{method}_speedup.pdf', bbox_inches='tight')
    plt.show()
    
    
    display(Markdown("## Total time with 0.2quantile, 0.8quantile"))
    plot_quantiles(data, method, 'total_op', legend=legend)
    plt.savefig(f'figures/{algorithm}_{method}_time.pdf', bbox_inches='tight')
    plt.show()
    
    
    display(Markdown("## Time as a function of image size and number of nodes"))
    plot_surface(medians.reset_index().melt(id_vars=['image_size'], value_name='time_ms'), legend)
    plt.savefig(f'figures/{algorithm}_{method}_3d.pdf')
    plt.show()
    
    gather_time = aggregate(data, method=method, stat='gather', aggfunc='median', print=False).reset_index().melt(id_vars=['image_size'], value_name='time_ms')
    if not gather_time.empty:
        display(Markdown("## MPI Gahering time"))
        plot_surface(gather_time, legend)
        plt.savefig(f'figures/{algorithm}_{method}_gathertime.pdf')
        plt.show()
    
        plot_quantiles(data, method, 'gather', legend=legend)
        plt.savefig(f'figures/{algorithm}_{method}_time.pdf', bbox_inches='tight')
        plt.show()
    
    p5 = aggregate(data, method=method, stat='barrier', aggfunc='median')
    if not p5.empty:
        display(Markdown("## barrier - waiting for all nodes before result transfer"))
        plot_quantiles(data, method, 'barrier', legend=legend)
        plt.savefig(f'figures/{algorithm}_{method}_time.pdf', bbox_inches='tight')
        plt.show()
    
    
    display(Markdown("## Dataset read time"))
    plot_quantiles(data, method, 'read', legend=legend)
    plt.show()
    
    
    display(Markdown("## Dataset write time"))
    plot_quantiles(data, method, 'write', legend=legend, count_check=None)
    plt.show()