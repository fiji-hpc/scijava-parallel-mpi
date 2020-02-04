/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2018 ImageJ developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package com.mycompany.imagej.ops.filter.convolve;

import com.mycompany.imagej.MPIUtils;
import com.mycompany.imagej.Utils;
import com.mycompany.imagej.chunk.Chunk;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.AbstractBinaryComputerOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.mycompany.imagej.Measure.measureCatch;

@Plugin(type = Ops.Filter.Convolve.class, priority = 1000.0D)
public class MPIConvolveNaiveC<I extends RealType<I>, O extends RealType<O> & NativeType<O>, K extends RealType<K>, C extends ComplexType<C> & NativeType<C>> extends AbstractBinaryComputerOp<RandomAccessibleInterval<I>, RandomAccessibleInterval<K>, RandomAccessibleInterval<O>>
		implements Ops.Filter.Convolve
{
	public void compute(RandomAccessibleInterval<I> input, RandomAccessibleInterval<K> kernel, RandomAccessibleInterval<O> output) {
		Chunk<O> chunks = new Chunk<>(Views.flatIterable(output), MPIUtils.getSize());
		measureCatch("convolution", () -> process(input, kernel, chunks.getChunk(MPIUtils.getRank())));
		chunks.sync();
	}

	private void process(RandomAccessibleInterval<I> input, RandomAccessibleInterval<K> kernel, Chunk<O> chunk) {
		final long[] kernelRadius = new long[kernel.numDimensions()];
		for (int i = 0; i < kernelRadius.length; i++) {
			kernelRadius[i] = kernel.dimension(i) / 2;
		}

		final List<Future<Void>> futures = new ArrayList<>();
		int threads = Runtime.getRuntime().availableProcessors();
		if(System.getenv("B_THREADS_NUM") != null) {
			threads = Integer.parseInt(System.getenv("B_THREADS_NUM"));
		}

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		int thread_num = 0;
		for(Chunk<O> job: chunk.split(threads)) {
			Utils.print("Thread " + thread_num++ + ": " + job);
			final Callable<Void> call = () -> {
				final Cursor<O> outC = job.cursor();
				final Cursor<K> kernelC = Views.iterable(kernel).localizingCursor();
				RandomAccess<I> inRA = Views.extendMirrorSingle(input).randomAccess();
				final long[] pos = new long[input.numDimensions()];

				while (outC.hasNext()) {
					// image
					outC.fwd();
					outC.localize(pos);

					// kernel inlined version of the method convolve
					float val = 0;
					inRA.setPosition(pos);

					kernelC.reset();
					while (kernelC.hasNext()) {
						kernelC.fwd();

						for (int i = 0; i < kernelRadius.length; i++) {
							// dimension can have zero extension e.g. vertical 1d kernel
							if (kernelRadius[i] > 0) {
								inRA.setPosition(pos[i] + kernelC.getLongPosition(i) -
										kernelRadius[i], i);
							}
						}

						val += inRA.get().getRealDouble() * kernelC.get().getRealDouble();
					}

					outC.get().setReal(val);
				}

				return null;
			};

			futures.add(executor.submit(call));
		}

		for (final Future<Void> future : futures) {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

		executor.shutdown();
	}
}