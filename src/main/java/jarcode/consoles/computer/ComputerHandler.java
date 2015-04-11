package jarcode.consoles.computer;

import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R2.NBTTagCompound;
import net.minecraft.server.v1_8_R2.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_8_R2.CraftServer;
import org.bukkit.craftbukkit.v1_8_R2.command.VanillaCommandWrapper;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class ComputerHandler implements Listener {

	private static ComputerHandler instance;

	private static final Field ITEM_STACK_HANDLE;
	private static final Constructor ITEM_STACK_CREATE;

	static {
		try {
			ITEM_STACK_HANDLE = CraftItemStack.class.getDeclaredField("handle");
			ITEM_STACK_HANDLE.setAccessible(true);
			ITEM_STACK_CREATE =
					CraftItemStack.class.getDeclaredConstructor(Material.class, int.class, short.class, ItemMeta.class);
			ITEM_STACK_CREATE.setAccessible(true);
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	// we inject a vanilla command intended to be ran by command blocks
	public static void registerLinkCommand() {
		SimpleCommandMap commandMap = ((CraftServer) Bukkit.getServer()).getCommandMap();
		commandMap.register("minecraft:", new VanillaCommandWrapper(new LinkCommand()));
	}

	public static ComputerHandler getInstance() {
		return instance;
	}

	{
		instance = this;
	}


	ShapedRecipe computerRecipe;
	private ArrayList<Computer> computers = new ArrayList<>();
	private HashMap<String, CommandBlock> linkRequests = new HashMap<>();

	{
		computerRecipe = new ShapedRecipe(newComputerStack());
		computerRecipe.shape("AAA", "ABA", "AAA");
		computerRecipe.setIngredient('A', Material.STONE);
		computerRecipe.setIngredient('B', Material.REDSTONE);
		Bukkit.getServer().addRecipe(computerRecipe);
	}

	public ComputerHandler() {
		registerLinkCommand();
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if (isComputer(e.getItemInHand())) {
			try {
				build(e.getPlayer(), e.getBlockPlaced().getLocation());
			}
			finally {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onCraft(CraftItemEvent e) {
		if (isNamedComputer(e.getCurrentItem())) {
			System.out.println("crafted");
			e.setCurrentItem(newComputerStack());
		}
	}

	private List<Computer> getComputers(UUID uuid) {
		return computers.stream()
				.filter(computer -> computer.getOwner().equals(uuid))
				.collect(Collectors.toList());
	}
	private void build(Player player, Location location) {
		BlockFace face = direction(player);
		ManagedComputer computer = new ManagedComputer(findHostname(player), player.getUniqueId());
		computer.create(face, location);
	}
	private String findHostname(Player player) {
		String name = player.getName() + "-";
		int[] index = {0};
		while (computers.stream().filter(comp -> comp.getHostname().equals(name + index[0])).findFirst().isPresent()) {
			index[0]++;
		}
		return name + index[0];
	}
	private BlockFace direction(Player player) {
		// shameless copy-paste from my other math code
		Location eye = player.getEyeLocation();
		double yaw = eye.getYaw() > 0 ? eye.getYaw() : 360 - Math.abs(eye.getYaw()); // remove negative degrees
		yaw += 90; // rotate +90 degrees
		if (yaw > 360)
			yaw -= 360;
		yaw  = (yaw  * Math.PI) / 180;
		double pitch  = ((eye.getPitch() + 90)  * Math.PI) / 180;

		double xp = Math.sin(pitch) * Math.cos(yaw);
		double zp = Math.sin(pitch) * Math.sin(yaw);
		if (Math.abs(xp) > Math.abs(zp)) {
			return xp < 0 ? BlockFace.EAST : BlockFace.WEST;
		}
		else {
			return zp < 0 ? BlockFace.SOUTH : BlockFace.NORTH;
		}
	}
	private boolean isNamedComputer(ItemStack stack) {
		ItemMeta meta = stack.getItemMeta();
		return meta.getDisplayName() != null && meta.getDisplayName().equals(ChatColor.GREEN + "Computer");
	}
	private boolean isComputer(ItemStack stack) {
		ItemMeta meta = stack.getItemMeta();

		net.minecraft.server.v1_8_R2.ItemStack nms;
		try {
			nms = (net.minecraft.server.v1_8_R2.ItemStack) ITEM_STACK_HANDLE.get(stack);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		NBTTagCompound tag = nms.getTag();
		return tag != null && meta.getDisplayName() != null && meta.getDisplayName().equals(ChatColor.GREEN + "Computer")
				&& stack.getType() == Material.STAINED_GLASS && tag.hasKey("computer");
	}
	private ItemStack newComputerStack() {
		return newComputerStack(true);
	}
	@SuppressWarnings("RedundantCast")
	private ItemStack newComputerStack(boolean glow) {
		ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.STAINED_GLASS);
		meta.setDisplayName(ChatColor.GREEN + "Computer");
		meta.setLore(Arrays.asList(ChatColor.RESET + "3x2", ChatColor.RESET + "Place to build"));
		ItemStack stack = null;
		try {
			stack = (ItemStack) ITEM_STACK_CREATE.newInstance(Material.STAINED_GLASS, 1, (short) 15, meta);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		if (glow) {
			net.minecraft.server.v1_8_R2.ItemStack nms;
			try {
				nms = (net.minecraft.server.v1_8_R2.ItemStack) ITEM_STACK_HANDLE.get((CraftItemStack) stack);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			NBTTagCompound comp = nms.getTag();
			comp.set("ench", new NBTTagList());
			comp.setBoolean("computer", true);
			nms.setTag(comp);
		}
		return stack;
	}
	public boolean hostnameTaken(String hostname) {
		return computers.stream().filter(comp -> comp.getHostname().equals(hostname))
				.findFirst().isPresent();
	}
	public Computer find(String hostname) {
		return computers.stream().filter(comp -> comp.getHostname().equals(hostname))
				.findFirst().orElseGet(() -> null);
	}
	public void request(String hostname, CommandBlock block) {
		linkRequests.put(hostname, block);
		find(hostname).requestDevice(block, event -> linkRequests.remove(hostname));
	}
	public void register(Computer computer) {
		computers.add(computer);
	}
	public void unregister(Computer computer) {
		computers.remove(computer);
	}
}
