package com.xoba.ngaro;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.xoba.ngaro.inf.IMemory;
import com.xoba.ngaro.inf.IOManager;
import com.xoba.ngaro.inf.IStack;

public class NGaroVM {

	private int ip;
	private final IMemory memory;

	private final IMemory ports = new Memory(12);

	private final IStack data, address;

	private final IOManager im;

	public NGaroVM(int dataStackSize, int addressStackSize, IMemory m, IOManager im) throws IOException {
		this.data = new Stack(dataStackSize);
		this.address = new Stack(addressStackSize);
		this.memory = m;
		this.im = im;
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
		ports.clear();
		loadImage("retroImage");
		if (memory.get(0) == 0) {
			System.out.println("Could not find image file!");
			System.exit(-1);
		}
	}

	private final Map<Integer, RandomAccessFile> randomAccessFiles = new HashMap<Integer, RandomAccessFile>();
	private final Map<Integer, File> files = new HashMap<Integer, File>();

	private int findOpenSlot() {
		Random random = new Random();
		while (true) {
			int n = 1 + random.nextInt(100000);
			if (!randomAccessFiles.containsKey(n)) {
				return n;
			}
		}
	}

	private RandomAccessFile create(File f, int mode) {
		try {

			RandomAccessFile raf = null;
			switch (mode) {

			case 0: { // reading
				if (f.exists()) {
					raf = new RandomAccessFile(f, "r");
				}
				break;
			}

			case 1: { // writing
				raf = new RandomAccessFile(f, "rw");
				raf.setLength(0);
				break;
			}

			case 2: { // append
				raf = new RandomAccessFile(f, "rw");
				raf.seek(raf.length());
				break;
			}

			case 3: { // mod

				if (f.exists()) {
					raf = new RandomAccessFile(f, "rw");
				}
				break;

			}

			default: {
				throw new IllegalStateException("illegal mode " + mode);
			}

			}

			return raf;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private int rxOpenFile() {

		int mode = data.pop();

		int slot = findOpenSlot();

		String filename = rxGetString();

		File f = new File(filename);

		RandomAccessFile raf = create(f, mode);

		if (raf == null) {
			return 0;
		} else {
			files.put(slot, f);
			randomAccessFiles.put(slot, raf);
			return slot;
		}

	}

	private int rxReadFile() {
		int slot = data.pop();
		try {
			int c = randomAccessFiles.get(slot).read();
			if (c < 0) {
				return 0;
			} else {
				return c;
			}
		} catch (Exception e) {
			return 0;
		}
	}

	private int rxWriteFile() {
		int slot = data.pop();
		int c = data.pop();
		try {
			randomAccessFiles.get(slot).write(c);
			return 1;
		} catch (Exception e) {
			return 0;
		}
	}

	private int rxCloseFile() {
		try {
			int slot = data.pop();
			try {
				randomAccessFiles.get(slot).close();
				return 0;
			} finally {
				randomAccessFiles.remove(slot);
				files.remove(slot);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private int rxGetFilePosition() {
		int slot = data.pop();
		try {
			return (int) randomAccessFiles.get(slot).getFilePointer();
		} catch (Exception e) {
			return -1;
		}
	}

	private int rxSetFilePosition() {
		int slot = data.pop();
		int pos = data.pop();
		try {
			randomAccessFiles.get(slot).seek(pos);
			return 0;
		} catch (Exception e) {
			return -1;
		}
	}

	private int rxGetFileSize() {
		int slot = data.pop();
		try {
			return (int) randomAccessFiles.get(slot).length();
		} catch (Exception e) {
			return -1;
		}
	}

	private int rxDeleteFile() {
		String filename = rxGetString();
		File f = new File(filename);
		if (f.delete()) {
			return -1;
		} else {
			return 0;
		}
	}

	private String rxGetString() {
		int name = data.pop();
		StringBuffer buf = new StringBuffer();
		int i = 0;
		boolean done = false;
		while (!done) {
			int j = memory.get(name + i);
			if (j == 0) {
				done = true;
			} else {
				buf.append((char) j);
				i++;
			}
		}
		return buf.toString();
	}

	public void handleDevices() {

		if (ports.get(0) == 1) {
			return;
		}

		if (ports.get(0) == 0 && ports.get(1) == 1) {
			final byte[] b = { 0, 0, 0 };
			try {
				b[0] = (byte) im.read();
			} catch (Exception e) {
				System.err.println(e);
			}
			ports.set(1, b[0]);
			ports.set(0, 1);
		}

		if (ports.get(2) == 1) {
			int x = data.pop();
			if (x < 0) {
				for (char c = 0; c < 300; c++)
					im.write('\n');
			} else
				im.write((char) x);
			ports.set(2, 0);
			ports.set(0, 1);
		}

		switch (ports.get(4)) {

		case 0: {
			ports.set(4, 0);
			break;
		}

		case 1: {
			saveImage("retroImage");
			ports.set(0, 1);
			ports.set(4, 0);
			break;
		}

		case 2: {
			try {
				im.pushInputName(rxGetString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			ports.set(4, 0);
			break;
		}

		case -1: // open a file
			ports.set(4, rxOpenFile());
			break;

		case -2: // read a byte from a file
			ports.set(4, rxReadFile());
			break;

		case -3: // write a byte to a file
			ports.set(4, rxWriteFile());
			break;

		case -4: // close a file
			ports.set(4, rxCloseFile());
			break;

		case -5: // return current location in file
			ports.set(4, rxGetFilePosition());
			break;

		case -6: // seek a new location in a file
			ports.set(4, rxSetFilePosition());
			break;

		case -7: // return the size of a file
			ports.set(4, rxGetFileSize());
			break;

		case -8: // delete a file
			ports.set(4, rxDeleteFile());
			break;

		default:
			ports.set(4, 0);

		}

		switch (ports.get(5)) {

		case -1:
			ports.set(5, memory.size());
			ports.set(0, 1);
			break;
		case -5:
			ports.set(5, data.getDepth());
			ports.set(0, 1);
			break;
		case -6:
			ports.set(5, address.getDepth());
			ports.set(0, 1);
			break;
		case -8:
			ports.set(5, (int) (System.currentTimeMillis() / 1000L));
			ports.set(0, 1);
			break;
		case -9:
			ip = memory.size();
			ports.set(5, 0);
			ports.set(0, 1);
			break;
		case -13:
			ports.set(5, 32);
			ports.set(0, 1);
			break;
		case -14:
			ports.set(5, 1);
			ports.set(0, 1);
			break;
		case -15:
			ports.set(5, -1);
			ports.set(0, 1);
			break;

		case -2:
		case -3:
		case -4:
		case -7:
		case -10:
		case -11:
		case -12:
			ports.set(5, 0);
			ports.set(0, 1);
			break;
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
			data.push(memory.get(++ip));
			break;
		}

		case VM_DUP: {
			data.push(data.peek());
			break;
		}

		case VM_DROP: {
			data.pop();
			break;
		}

		case VM_SWAP: {
			int x = data.pop();
			int y = data.pop();
			data.push(x);
			data.push(y);
			break;
		}

		case VM_PUSH: {
			address.push(data.pop());
			break;
		}

		case VM_POP: {
			data.push(address.pop());
			break;
		}

		case VM_LOOP: {
			data.push(data.pop() - 1);
			ip++;
			if (data.peek() != 0 && data.peek() > -1)
				ip = memory.get(ip) - 1;
			else
				data.drop(1);
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
			ip = address.pop();
			if (memory.get(ip + 1) == 0)
				ip++;
			if (memory.get(ip + 1) == 0)
				ip++;
			break;
		}

		case VM_LT_JUMP: {
			ip++;
			if (data.pop() < data.pop())
				ip = memory.get(ip) - 1;
			break;
		}

		case VM_GT_JUMP: {
			ip++;
			if (data.pop() > data.pop())
				ip = memory.get(ip) - 1;
			break;
		}

		case VM_NE_JUMP: {
			ip++;
			if (data.pop() != data.pop())
				ip = memory.get(ip) - 1;
			break;
		}

		case VM_EQ_JUMP: {
			ip++;
			if (data.pop() == data.pop())
				ip = memory.get(ip) - 1;
			break;
		}

		case VM_FETCH: {
			int x = data.pop();
			data.push(memory.get(x));
			break;
		}

		case VM_STORE: {
			memory.set(data.pop(), data.pop());
			break;
		}

		case VM_ADD: {
			int y = data.pop();
			int x = data.pop();
			data.push(x + y);
			break;
		}

		case VM_SUB: {
			int y = data.pop();
			int x = data.pop();
			data.push(x - y);
			break;
		}

		case VM_MUL: {
			int y = data.pop();
			int x = data.pop();
			data.push(x * y);
			break;
		}

		case VM_DIVMOD: {
			final int x = data.pop();
			final int y = data.pop();
			data.push(y % x);
			data.push(y / x);
			break;
		}

		case VM_AND: {
			int y = data.pop();
			int x = data.pop();
			data.push(x & y);
			break;
		}

		case VM_OR: {
			int y = data.pop();
			int x = data.pop();
			data.push(x | y);
			break;
		}

		case VM_XOR: {
			int y = data.pop();
			int x = data.pop();
			data.push(x ^ y);
			break;
		}

		case VM_SHL: {
			int y = data.pop();
			int x = data.pop();
			data.push(x << y);
			break;
		}

		case VM_SHR: {
			int y = data.pop();
			int x = data.pop();
			data.push(x >>= y);
			break;
		}

		case VM_ZERO_EXIT: {
			if (data.peek() == 0) {
				data.drop(1);
				ip = address.pop();
			}
			break;
		}

		case VM_INC: {
			data.push(data.pop() + 1);
			break;
		}

		case VM_DEC: {
			data.push(data.pop() - 1);
			break;
		}

		case VM_IN: {
			final int x = data.pop();
			data.push(ports.get(x));
			ports.set(x, 0);
			break;
		}

		case VM_OUT: {
			ports.set(0, 0);
			int x = data.pop();
			int y = data.pop();
			ports.set(x, y);
			break;
		}

		case VM_WAIT: {
			handleDevices();
			break;
		}

		default: {
			address.push(ip);
			ip = memory.get(ip) - 1;
			if (memory.get(ip + 1) == 0)
				ip++;
			if (memory.get(ip + 1) == 0)
				ip++;
			break;
		}

		}
	}

	public void run() {
		for (ip = 0; ip < memory.size(); ip++) {
			process();
		}
	}

	public static void main(String[] args) throws Exception {
		System.setErr(System.out);
		// for (String f : new String[] { "base.rx", "core.rx", "vocabs.rx" }) {
		for (String f : new String[] { "files.rx" }) {
			IOManager im = new InputManager();
			im.pushInputName("test/" + f);
			NGaroVM vm = new NGaroVM(128, 1024, new Memory(1000000), im);
			vm.initialize();
			vm.run();
			System.out.println("********************************************************* DONE");
		}
	}
}
