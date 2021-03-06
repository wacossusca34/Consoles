package ca.jarcode.consoles.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import ca.jarcode.consoles.internal.ManagedConsole;
import ca.jarcode.consoles.internal.ConsoleHandler;
import ca.jarcode.consoles.Consoles;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashMap;

public class ConsoleBungeeHook implements PluginMessageListener, Listener {

	private final HashMap<String, Object> commands = new HashMap<>();
	private ConsoleHandler handler;

	{
		commands.put("clear", (IncomingHookCommand) (player, input) -> {
			handler.clearAllocations(player);
			for (ManagedConsole console : handler.getConsoles())
				handler.getPainter().updateFor(console, player);
		});
	}

	public ConsoleBungeeHook() {
		this.handler = ConsoleHandler.getInstance();

		Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(Consoles.getInstance(), "Console");
		Bukkit.getServer().getMessenger().registerIncomingPluginChannel(Consoles.getInstance(), "Console", this);

		handler.setHook(this);
	}

	@Override
	public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
		if (s.equals("Console")) {
			ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
			String command = input.readUTF();
			Object cmd = commands.get(command.toLowerCase());
			if (cmd != null && cmd instanceof IncomingHookCommand) {
				((IncomingHookCommand) cmd).handle(player, input);
			}
		}
	}

	public boolean execute(Player player, String command, Object... args) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		Object cmd = commands.get(command);
		if (cmd != null && cmd instanceof OutgoingHookCommand) {
			out.writeUTF(command);
			((OutgoingHookCommand) cmd).handle(player, args, out);
			player.sendPluginMessage(Consoles.getInstance(), "Console", out.toByteArray());
			return true;
		}
		else return false;
	}
}
