/*
 * Copyright (c) 2009 - 2011, Simon Waite and Charles Childers
 * Based on the C# implementation
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.xoba.util.ILogger;
import com.xoba.util.LogFactory;
import com.xoba.util.MraUtils;

public class Retro {

	private static final ILogger logger = LogFactory.getDefault().create();

	private int rsp, ip, fp;
	private final int ports[] = new int[12];
	private final byte[] file;

	private final int address[];
	private final IMemory memory;
	private final Stack s;

	public Retro(int dataStackSize, int addressStackSize, int memorySize, File f) throws IOException {
		this.s = new Stack(dataStackSize);
		address = new int[addressStackSize];
		memory = new Memory(memorySize);
		if (f == null) {
			file = new byte[0];
		} else {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				MraUtils.copy(f, out);
			} finally {
				out.close();
			}
			file = out.toByteArray();
			logger.debugf("loaded %,d input bytes", file.length);
		}
	}

	public static final int VM_NOP = 0;
	public static final int VM_LIT = 1;
	public static final int VM_DUP = 2;
	public static final int VM_DROP = 3;
	public static final int VM_SWAP = 4;
	public static final int VM_PUSH = 5;
	public static final int VM_POP = 6;
	public static final int VM_LOOP = 7;
	public static final int VM_JUMP = 8;
	public static final int VM_RETURN = 9;
	public static final int VM_LT_JUMP = 10;
	public static final int VM_GT_JUMP = 11;
	public static final int VM_NE_JUMP = 12;
	public static final int VM_EQ_JUMP = 13;
	public static final int VM_FETCH = 14;
	public static final int VM_STORE = 15;
	public static final int VM_ADD = 16;
	public static final int VM_SUB = 17;
	public static final int VM_MUL = 18;
	public static final int VM_DIVMOD = 19;
	public static final int VM_AND = 20;
	public static final int VM_OR = 21;
	public static final int VM_XOR = 22;
	public static final int VM_SHL = 23;
	public static final int VM_SHR = 24;
	public static final int VM_ZERO_EXIT = 25;
	public static final int VM_INC = 26;
	public static final int VM_DEC = 27;
	public static final int VM_IN = 28;
	public static final int VM_OUT = 29;
	public static final int VM_WAIT = 30;

	/**
	 * Returns the value in the opposite endian
	 * 
	 * @return int
	 */
	public int switchEndian(int value) {
		int b1 = value & 0xff;
		int b2 = (value >> 8) & 0xff;
		int b3 = (value >> 16) & 0xff;
		int b4 = (value >> 24) & 0xff;
		return b1 << 24 | b2 << 16 | b3 << 8 | b4;
	}

	public void loadImage(String name) {
		memory.set(0, 0);
		File f = new File(name);
		if (f.exists()) {
			try {
				RandomAccessFile in = new RandomAccessFile(f, "r");
				long n = in.length() / 4;
				try {
					for (int i = 0; i < n; i++) {
						memory.set(i, switchEndian(in.readInt()));
					}
				} finally {
					in.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void saveImage(String name) {
		try {
			RandomAccessFile out = new RandomAccessFile(name, "rw");
			try {
				for (int i = 0; i < memory.size(); i++) {
					out.writeInt(switchEndian(memory.get(i)));
				}
			} finally {
				out.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initialize() {
		memory.clear();
		loadImage("retroImage");
		if (memory.get(0) == 0) {
			System.out.println("Could not find image file!");
			System.exit(-1);
		}
	}

	private static interface IStack {

		public int pop();

		public int peek();

		public void push(int v);

	}

	private static interface IMemory {

		public int size();

		public int get(int pc);

		public void set(int pc, int value);

		public void set(int pc, int[] buffer);

		public void clear();

	}

	private static class Memory implements IMemory {

		private final int[] memory;

		public Memory(int n) {
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
			return memory.length;
		}

		@Override
		public void clear() {
			for (int i = 0; i < memory.length; i++) {
				memory[i] = 0;
			}
		}
	}

	public void handleDevices() {

		if (ports[0] == 1) {
			return;
		}

		if (ports[0] == 0 && ports[1] == 1) {
			if (fp < file.length) {
				ports[1] = file[fp++];
			} else {
				final byte[] b = { 0, 0, 0 };
				try {
					System.in.read(b, 0, 1);
				} catch (Exception e) {
					System.out.println(e);
				}
				ports[1] = b[0];
			}
			ports[0] = 1;
		}

		if (ports[2] == 1) {
			char c = (char) s.data[s.sp];
			if (s.data[s.sp] < 0) {
				for (c = 0; c < 300; c++)
					System.out.println("\n");
			} else
				System.out.print(c);
			s.sp--;
			ports[2] = 0;
			ports[0] = 1;
		}

		if (ports[4] == 1) {
			saveImage("retroImage");
			ports[4] = 0;
			ports[0] = 1;
		}

		switch (ports[5]) {

		case -1:
			ports[5] = memory.size();
			ports[0] = 1;
			break;
		case -2:
			ports[5] = 0;
			ports[0] = 1;
			break;
		case -3:
			ports[5] = 0;
			ports[0] = 1;
			break;
		case -4:
			ports[5] = 0;
			ports[0] = 1;
			break;
		case -5:
			ports[5] = s.sp;
			ports[0] = 1;
			break;
		case -6:
			ports[5] = rsp;
			ports[0] = 1;
			break;
		case -7:
			ports[5] = 0;
			ports[0] = 1;
			break;
		case -8:
			ports[5] = (int) (System.currentTimeMillis() / 1000L);
			ports[0] = 1;
			break;
		case -9:
			ip = memory.size();
			ports[5] = 0;
			ports[0] = 1;
			break;
		case -10:
			ports[5] = 0;
			ports[0] = 1;
			break;
		case -11:
			ports[5] = 0;
			ports[0] = 1;
			break;
		case -12:
			ports[5] = 0;
			ports[0] = 1;
			break;
		case -13:
			ports[5] = 32;
			ports[0] = 1;
			break;
		case -14:
			ports[5] = 1;
			ports[0] = 1;
			break;
		}

	}

	private static class Stack implements IStack {

		private final int[] data;
		private int sp;

		public Stack(int n) {
			this.data = new int[n];
		}

		@Override
		public int pop() {
			return data[sp--];
		}

		@Override
		public int peek() {
			return data[sp];
		}

		@Override
		public void push(int v) {
			data[++sp] = v;
		}

	}

	/**
	 * Process a single opcode
	 */
	public void process() {

		switch (memory.get(ip)) {

		case VM_NOP: {
			break;
		}

		case VM_LIT: {
			s.push(memory.get(++ip));
			break;
		}

		case VM_DUP: {
			s.push(s.peek());
			break;
		}

		case VM_DROP: {
			s.data[s.sp--] = 0;
			break;
		}

		case VM_SWAP: {
			int x = s.data[s.sp];
			int y = s.data[s.sp - 1];
			s.data[s.sp] = y;
			s.data[s.sp - 1] = x;
			break;
		}

		case VM_PUSH: {
			address[++rsp] = s.data[s.sp--];
			break;
		}

		case VM_POP: {
			s.sp++;
			s.data[s.sp] = address[rsp];
			rsp--;
			break;
		}

		case VM_LOOP: {
			s.data[s.sp]--;
			ip++;
			if (s.data[s.sp] != 0 && s.data[s.sp] > -1)
				ip = memory.get(ip) - 1;
			else
				s.sp--;
			break;
		}

		case VM_JUMP: {
			ip++;
			ip = memory.get(ip) - 1;
			if (memory.get(ip + 1) == 0)
				ip++;
			if (memory.get(ip + 1) == 0)
				ip++;
			break;
		}

		case VM_RETURN: {
			ip = address[rsp];
			rsp--;
			if (memory.get(ip + 1) == 0)
				ip++;
			if (memory.get(ip + 1) == 0)
				ip++;
			break;
		}

		case VM_LT_JUMP: {
			ip++;
			if (s.data[s.sp - 1] > s.data[s.sp])
				ip = memory.get(ip) - 1;
			s.sp = s.sp - 2;
			break;
		}

		case VM_GT_JUMP: {
			ip++;
			if (s.data[s.sp - 1] < s.data[s.sp])
				ip = memory.get(ip) - 1;
			s.sp = s.sp - 2;
			break;
		}

		case VM_NE_JUMP: {
			ip++;
			if (s.data[s.sp - 1] != s.data[s.sp])
				ip = memory.get(ip) - 1;
			s.sp = s.sp - 2;
			break;
		}

		case VM_EQ_JUMP: {
			ip++;
			if (s.data[s.sp - 1] == s.data[s.sp])
				ip = memory.get(ip) - 1;
			s.sp = s.sp - 2;
			break;
		}

		case VM_FETCH: {
			int x = s.data[s.sp];
			s.data[s.sp] = memory.get(x);
			break;
		}

		case VM_STORE: {
			memory.set(s.data[s.sp], s.data[s.sp - 1]);
			s.sp = s.sp - 2;
			break;
		}

		case VM_ADD: {
			s.data[s.sp - 1] += s.data[s.sp];
			s.data[s.sp] = 0;
			s.sp--;
			break;
		}

		case VM_SUB: {
			s.data[s.sp - 1] -= s.data[s.sp];
			s.data[s.sp] = 0;
			s.sp--;
			break;
		}

		case VM_MUL: {
			s.data[s.sp - 1] *= s.data[s.sp];
			s.data[s.sp] = 0;
			s.sp--;
			break;
		}

		case VM_DIVMOD: {
			final int x = s.data[s.sp];
			final int y = s.data[s.sp - 1];
			s.data[s.sp] = y / x;
			s.data[s.sp - 1] = y % x;
			break;
		}

		case VM_AND: {
			final int x = s.data[s.sp];
			final int y = s.data[s.sp - 1];
			s.sp--;
			s.data[s.sp] = x & y;
			break;
		}

		case VM_OR: {
			final int x = s.data[s.sp];
			final int y = s.data[s.sp - 1];
			s.sp--;
			s.data[s.sp] = x | y;
			break;
		}

		case VM_XOR: {
			final int x = s.data[s.sp];
			final int y = s.data[s.sp - 1];
			s.sp--;
			s.data[s.sp] = x ^ y;
			break;
		}

		case VM_SHL: {
			final int x = s.data[s.sp];
			final int y = s.data[s.sp - 1];
			s.sp--;
			s.data[s.sp] = y << x;
			break;
		}

		case VM_SHR: {
			final int x = s.data[s.sp];
			int y = s.data[s.sp - 1];
			s.sp--;
			s.data[s.sp] = y >>= x;
			break;
		}

		case VM_ZERO_EXIT: {
			if (s.data[s.sp] == 0) {
				s.sp--;
				ip = address[rsp];
				rsp--;
			}
			break;
		}

		case VM_INC: {
			s.data[s.sp]++;
			break;
		}

		case VM_DEC: {
			s.data[s.sp]--;
			break;
		}

		case VM_IN: {
			final int x = s.data[s.sp];
			s.data[s.sp] = ports[x];
			ports[x] = 0;
			break;
		}

		case VM_OUT: {
			ports[0] = 0;
			ports[s.data[s.sp]] = s.data[s.sp - 1];
			s.sp = s.sp - 2;
			break;
		}

		case VM_WAIT: {
			handleDevices();
			break;
		}

		default: {
			rsp++;
			address[rsp] = ip;
			ip = memory.get(ip) - 1;
			if (memory.get(ip + 1) == 0)
				ip++;
			if (memory.get(ip + 1) == 0)
				ip++;
			break;
		}
		}
	}

	private void store(int[] stuff, int pc) {
		System.arraycopy(stuff, 0, memory, pc, stuff.length);
	}

	private void dump() {
		for (int i = 0; i < 10; i++) {
			logger.debugf("mem[%2d] = %d", i, memory.get(i));
		}
		for (int i = 0; i < s.sp + 3; i++) {
			logger.debugf("data[%2d] = %d", i, s.data[i]);
		}
	}

	/**
	 * The main entry point. What else needs to be said?
	 */
	public static void main(String[] args) throws Exception {
		final int n = 1000000;
		Retro vm = new Retro(128, 1024, n, new File("test/core.rx"));
		if (false) {
			vm.store(new int[] { VM_LIT, 101 }, 0);
		} else {
			vm.initialize();
		}

		for (vm.ip = 0; vm.ip < n; vm.ip++) {
			vm.process();
		}

		// vm.dump();

	}
}
