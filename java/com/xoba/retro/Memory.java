package com.xoba.retro;

public class Memory implements IMemory {

	private final int n;
	private final int[] memory;

	public Memory(int n) {
		this.n = n;
		this.memory = new int[n];
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
	public void set(int pc, int[] buffer) {
		System.arraycopy(buffer, 0, memory, pc, buffer.length);
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
}