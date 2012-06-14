package com.xoba.ngaro;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.xoba.ngaro.inf.IInputManager;

public class InputManager implements IInputManager {

	private final java.util.Stack<InputStream> stack = new java.util.Stack<InputStream>();

	public InputManager() {
		stack.push(System.in);
	}

	@Override
	public void pushInputName(String name) throws IOException {
		stack.push(new FileInputStream(new File(name)));
	}

	@Override
	public int read() throws IOException {
		while (stack.size() > 0) {
			InputStream in = stack.peek();
			int b = in.read();
			if (b >= 0) {
				return b;
			} else {
				in.close();
				stack.pop();
			}
		}
		return -1;
	}

}