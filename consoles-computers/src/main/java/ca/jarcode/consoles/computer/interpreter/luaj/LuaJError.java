package ca.jarcode.consoles.computer.interpreter.luaj;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptError;
import org.luaj.vm2.LuaError;

public class LuaJError extends ScriptError {

	public final LuaError err;

	public LuaJError(LuaError err) {
		this.err = err;
	}

	@Override
	public LuaError underlying() {
		return err;
	}

	@Override
	public String constructMessage() {
		return err.getMessage();
	}

	@Override
	public String getMessage() {
		return err.getMessage();
	}

	@Override
	public Throwable getCause() {
		return err;
	}
}
