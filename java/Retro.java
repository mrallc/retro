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

	private int sp, rsp, ip, fp;
	private final int ports[] = new int[12];
	private final byte[] file;

	private final int data[], address[], memory[];

	public Retro(int dataStackSize, int addressStackSize, int memorySize, File f) throws IOException {
		data = new int[dataStackSize];
		address = new int[addressStackSize];
		memory = new int[memorySize];
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
		memory[0] = 0;
		File f = new File(name);
		if (f.exists()) {
			try {
				RandomAccessFile in = new RandomAccessFile(f, "r");
				long n = in.length() / 4;
				try {
					for (int i = 0; i < n; i++) {
						memory[i] = switchEndian(in.readInt());
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
				for (int i = 0; i < memory.length; i++) {
					out.writeInt(switchEndian(memory[i]));
				}
			} finally {
				out.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initialize() {
		for (int i = 0; i < memory.length; i++) {
			memory[i] = 0;
		}
		loadImage("retroImage");
		if (memory[0] == 0) {
			System.out.println("Could not find image file!");
			System.exit(-1);
		}
	}

	private static interface IMemory {

		public int get(int pc);

		public void set(int pc, int value);

		public void set(int pc, int[] buffer);

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
			char c = (char) data[sp];
			if (data[sp] < 0) {
				for (c = 0; c < 300; c++)
					System.out.println("\n");
			} else
				System.out.print(c);
			sp--;
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
			ports[5] = memory.length;
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
			ports[5] = sp;
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
			ip = memory.length;
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

	private int get(int ip) {
		return memory[ip];
	}

	private void set(int ip, int v) {
		memory[ip] = v;
	}

	/**
	 * Process a single opcode
	 */
	public void process() {

		switch (get(ip)) {

		case VM_NOP: {
			break;
		}

		case VM_LIT: {
			data[++sp] = get(++ip);
			break;
		}

		case VM_DUP: {
			sp++;
			data[sp] = data[sp - 1];
			break;
		}

		case VM_DROP: {
			data[sp--] = 0;
			break;
		}

		case VM_SWAP: {
			int x = data[sp];
			int y = data[sp - 1];
			data[sp] = y;
			data[sp - 1] = x;
			break;
		}

		case VM_PUSH: {
			address[++rsp] = data[sp--];
			break;
		}

		case VM_POP: {
			sp++;
			data[sp] = address[rsp];
			rsp--;
			break;
		}

		case VM_LOOP: {
			data[sp]--;
			ip++;
			if (data[sp] != 0 && data[sp] > -1)
				ip = get(ip) - 1;
			else
				sp--;
			break;
		}

		case VM_JUMP: {
			ip++;
			ip = get(ip) - 1;
			if (get(ip + 1) == 0)
				ip++;
			if (get(ip + 1) == 0)
				ip++;
			break;
		}

		case VM_RETURN: {
			ip = address[rsp];
			rsp--;
			if (get(ip + 1) == 0)
				ip++;
			if (get(ip + 1) == 0)
				ip++;
			break;
		}

		case VM_LT_JUMP: {
			ip++;
			if (data[sp - 1] > data[sp])
				ip = get(ip) - 1;
			sp = sp - 2;
			break;
		}

		case VM_GT_JUMP: {
			ip++;
			if (data[sp - 1] < data[sp])
				ip = get(ip) - 1;
			sp = sp - 2;
			break;
		}

		case VM_NE_JUMP: {
			ip++;
			if (data[sp - 1] != data[sp])
				ip = get(ip) - 1;
			sp = sp - 2;
			break;
		}

		case VM_EQ_JUMP: {
			ip++;
			if (data[sp - 1] == data[sp])
				ip = get(ip) - 1;
			sp = sp - 2;
			break;
		}

		case VM_FETCH: {
			int x = data[sp];
			data[sp] = get(x);
			break;
		}

		case VM_STORE: {
			set(data[sp], data[sp - 1]);
			sp = sp - 2;
			break;
		}

		case VM_ADD: {
			data[sp - 1] += data[sp];
			data[sp] = 0;
			sp--;
			break;
		}

		case VM_SUB: {
			data[sp - 1] -= data[sp];
			data[sp] = 0;
			sp--;
			break;
		}

		case VM_MUL: {
			data[sp - 1] *= data[sp];
			data[sp] = 0;
			sp--;
			break;
		}

		case VM_DIVMOD: {
			final int x = data[sp];
			final int y = data[sp - 1];
			data[sp] = y / x;
			data[sp - 1] = y % x;
			break;
		}

		case VM_AND: {
			final int x = data[sp];
			final int y = data[sp - 1];
			sp--;
			data[sp] = x & y;
			break;
		}

		case VM_OR: {
			final int x = data[sp];
			final int y = data[sp - 1];
			sp--;
			data[sp] = x | y;
			break;
		}

		case VM_XOR: {
			final int x = data[sp];
			final int y = data[sp - 1];
			sp--;
			data[sp] = x ^ y;
			break;
		}

		case VM_SHL: {
			final int x = data[sp];
			final int y = data[sp - 1];
			sp--;
			data[sp] = y << x;
			break;
		}

		case VM_SHR: {
			final int x = data[sp];
			int y = data[sp - 1];
			sp--;
			data[sp] = y >>= x;
			break;
		}

		case VM_ZERO_EXIT: {
			if (data[sp] == 0) {
				sp--;
				ip = address[rsp];
				rsp--;
			}
			break;
		}

		case VM_INC: {
			data[sp]++;
			break;
		}

		case VM_DEC: {
			data[sp]--;
			break;
		}

		case VM_IN: {
			final int x = data[sp];
			data[sp] = ports[x];
			ports[x] = 0;
			break;
		}

		case VM_OUT: {
			ports[0] = 0;
			ports[data[sp]] = data[sp - 1];
			sp = sp - 2;
			break;
		}

		case VM_WAIT: {
			handleDevices();
			break;
		}

		default: {
			rsp++;
			address[rsp] = ip;
			ip = get(ip) - 1;
			if (get(ip + 1) == 0)
				ip++;
			if (get(ip + 1) == 0)
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
			logger.debugf("mem[%2d] = %d", i, memory[i]);
		}
		for (int i = 0; i < sp + 3; i++) {
			logger.debugf("data[%2d] = %d", i, data[i]);
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

		vm.dump();

	}
}
