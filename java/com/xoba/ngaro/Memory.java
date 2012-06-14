package com.xoba.ngaro;

import java.util.Arrays;

import com.xoba.ngaro.inf.IMemory;

public class Memory implements IMemory {

	private final int n;
	private final int[] memory;

	public Memory(int n) {
		this.n = n;
		this.memory = new int[this.n];
	}

	@Override
	public int get(int pc) {
		return memory[pc];
	}

	@Override
	public void set(int pc, int value) {
		memory[pc] = value;
	}

	@Override
	public int size() {
		return n;
	}

	@Override
	public void clear() {
		for (int i = 0; i < n; i++) {
			memory[i] = 0;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof IMemory) {
			IMemory m = (IMemory) o;
			if (m.size() != this.size()) {
				return false;
			}
			for (int i = 0; i < n; i++) {
				if (this.get(i) != m.get(i)) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}

	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(memory);
	}
}