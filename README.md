# OpenMPI Ops
## Introduction
This is a project that brings [MPI](https://en.wikipedia.org/wiki/Message_Passing_Interface)-parallelized
versions of many [Scijava Ops](https://imagej.net/libs/imagej-ops/) into [Fiji](https://fiji.sc/). Fiji is a distribution of ImageJ.
The *Ops* are here single- or multi-threaded image processing *operations*. This project extends many of them to become distributed, which allows to process very large images per partes with each part being computed separately and ideally in the same time (in parallel).

It is a sibbling project to the [Parallel Macro](https://github.com/fiji-hpc/parallel-macro).

This project is expected be used along with HPC Workflow Manager that provides a GUI. The links to it are given below.
<br/>
**Fasttrack**:
Follow [these instructions](https://github.com/fiji-hpc/parallel-macro/wiki/How-to-install-HPC-Workflow-Manager-client) to install it from a Fiji update site.

## Important links
- [__The Short Guide on how to install and use the project HPC Workflow Manager__](https://github.com/fiji-hpc/parallel-macro/wiki/Short-Guide)
- [The official web page about this whole project](https://fiji-hpc.github.io/hpc-parallel-tools/)
- [Wiki page of the project](https://imagej.net/HPC_Workflow_Manager) on [imagej.net](https://imagej.net/)
- [The source code](https://github.com/fiji-hpc/hpc-workflow-manager-full) along with **manual** installation instructions for it. 

## Reporting issues
I'm sure issues will come across :( <br/>
[Please, tell us here.](https://github.com/fiji-hpc/parallel-macro/issues)
