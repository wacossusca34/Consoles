package ca.jarcode.consoles.v1_8_R2;

import ca.jarcode.consoles.api.nms.GeneralInternals;
import net.minecraft.server.v1_8_R2.*;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R2.CraftServer;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftItemFrame;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R2.util.CraftMagicNumbers;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class GeneralUtils implements GeneralInternals {


	private final Field ITEM_STACK_HANDLE;
	private final Constructor ITEM_STACK_CREATE;

	// get constructor and handle for craftbukkit's item stack.
	{
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

	@Override
	public boolean forceAddFrame(ItemFrame frame, World bukkitWorld) {
		EntityItemFrame nms = ((CraftItemFrame) frame).getHandle();
		net.minecraft.server.v1_8_R2.World world = ((CraftWorld) bukkitWorld).getHandle();
		nms.dead = false;
		return world.addEntity(nms);
	}

	@Override
	public void forceRemoveFrame(ItemFrame frame, World bukkitWorld) {
		net.minecraft.server.v1_8_R2.World world = ((CraftWorld) bukkitWorld).getHandle();
		EntityItemFrame nms = ((CraftItemFrame) frame).getHandle();
		world.removeEntity(nms);
	}

	@Override
	public boolean commandBlocksEnabled() {
		return ((CraftServer) Bukkit.getServer()).getServer().getEnableCommandBlock();
	}

	@Override
	public void setCommandBlocksEnabled(boolean enabled) {
		((CraftServer) Bukkit.getServer()).getServer().getPropertyManager().setProperty("enable-command-block", enabled);
	}

	@Override
	public InitResult initFrame(World world, int x, int y, int z, short globalId, BlockFace face) {
		// get NMS world
		net.minecraft.server.v1_8_R2.World mcWorld = ((CraftWorld) world).getHandle();
		// create item frame entity
		EntityItemFrame itemFrame = new EntityItemFrame(mcWorld, new BlockPosition(
				x, y, z), nmsDirection(face));
		if (world.isChunkLoaded(itemFrame.getBlockPosition().getX() / 16,
				itemFrame.getBlockPosition().getZ() / 16))
			// add the entity if chunk is loaded
			mcWorld.addEntity(itemFrame);
		// set item in frame
		itemFrame.setItem(CraftItemStack.asNMSCopy(new ItemStack(Material.MAP, 1, globalId)));

		return new InitResult() {
			@Override
			public ItemFrame getEntity() {
				return (ItemFrame) itemFrame.getBukkitEntity();
			}

			@Override
			public int getEntityId() {
				return itemFrame.getId();
			}
		};
	}

	@Override
	public void forceDeleteDirectory(File file) throws IOException {
		FileUtils.deleteDirectory(file);
	}

	@Override
	public void fakeEnchantItem(ItemStack stack) {
		net.minecraft.server.v1_8_R2.ItemStack nms = nmsStack(stack);
		NBTTagCompound comp = nms.getTag();
		comp.set("ench", new NBTTagList());
		nms.setTag(comp);
	}

	@Override
	public void setItemNBTString(ItemStack stack, String key, String value) {
		net.minecraft.server.v1_8_R2.ItemStack nms = nmsStack(stack);
		NBTTagCompound comp = nms.getTag();
		if (value != null && key != null)
			comp.setString(key, value);
		nms.setTag(comp);
	}

	@Override
	public void setItemNBTBoolean(ItemStack stack, String key, boolean value) {
		net.minecraft.server.v1_8_R2.ItemStack nms = nmsStack(stack);
		NBTTagCompound comp = nms.getTag();
		comp.setBoolean(key, value);
		nms.setTag(comp);
	}

	@Override
	public String getItemNBTString(ItemStack stack, String key) {
		net.minecraft.server.v1_8_R2.ItemStack nms = nmsStack(stack);
		NBTTagCompound comp = nms.getTag();
		return comp.getString(key);
	}

	@Override
	public boolean getItemNBTBoolean(ItemStack stack, String key) {
		net.minecraft.server.v1_8_R2.ItemStack nms = nmsStack(stack);
		NBTTagCompound comp = nms.getTag();
		return comp.getBoolean(key);
	}

	@Override
	public boolean hasItemNBTTag(ItemStack stack) {
		net.minecraft.server.v1_8_R2.ItemStack nms = nmsStack(stack);
		return nms.getTag() != null;
	}

	@Override
	public void modPlayerHead(ItemStack head, UUID owner, String texValue) {
		if (!hasHandle(head)) {
			initHandle(head);
		}

		net.minecraft.server.v1_8_R2.ItemStack nms = nmsStack(head);
		if (nms == null) {
			throw new IllegalArgumentException();
		}
		NBTTagCompound tag = nms.hasTag() ? nms.getTag() : new NBTTagCompound();
		NBTTagCompound profile = new NBTTagCompound();
		NBTTagCompound properties = new NBTTagCompound();
		NBTTagList textures = new NBTTagList();
		profile.set("Id", new NBTTagString(owner.toString()));
		properties.set("textures", textures);
		textures.add(new NBTTagString(texValue));
		profile.set("Properties", properties);
		tag.set("SkullOwner", profile);
		nms.setTag(tag);
	}

	@Override
	public void initHandle(ItemStack stack) {
		Item item = CraftMagicNumbers.getItem(stack.getType());
		if(item != null) {
			net.minecraft.server.v1_8_R2.ItemStack nms = new net.minecraft.server.v1_8_R2.ItemStack(item,
					stack.getAmount(), stack.getDurability());
			if(stack.hasItemMeta()) {
				CraftItemStack.setItemMeta(nms, stack.getItemMeta());
			}
			try {
				ITEM_STACK_HANDLE.set(stack, nms);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			throw new IllegalArgumentException(stack.toString());
		}
	}

	private boolean hasHandle(ItemStack stack) {
		try {
			return ITEM_STACK_HANDLE.get(stack) != null;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ItemStack itemStackBuild(Material m, int i, short data, ItemMeta meta) {
		try {
			return (ItemStack) ITEM_STACK_CREATE.newInstance(m, i, data, meta);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private net.minecraft.server.v1_8_R2.ItemStack nmsStack(ItemStack stack) {
		try {
			return (net.minecraft.server.v1_8_R2.ItemStack) ITEM_STACK_HANDLE.get(stack);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private EnumDirection nmsDirection(BlockFace face) {
		switch (face) {
			case NORTH: return EnumDirection.NORTH;
			case SOUTH: return EnumDirection.SOUTH;
			case EAST: return EnumDirection.EAST;
			case WEST: return EnumDirection.WEST;
			default: return null;
		}
	}

}
