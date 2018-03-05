package com.github.Jochyoua.MCSF;

import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.Jochyoua.MCSF.checks.bookCheck;
import com.github.Jochyoua.MCSF.checks.signCheck;

public class Main extends JavaPlugin {
	public utils util = null;

	@Override
	public void onEnable() {
		this.util = new utils(this);
		this.util.setupConfig();
		this.getCommand("MCSF").setExecutor(new MCSFCommand(this));
		getServer().getPluginManager().registerEvents(new signCheck(this), this);
		getServer().getPluginManager().registerEvents(new bookCheck(this), this);
		util.generateswearList();
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Server.CHAT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				try {
					PacketContainer packet = event.getPacket();
					StructureModifier<WrappedChatComponent> chatComponents = packet.getChatComponents();
					for (WrappedChatComponent component : chatComponents.getValues()) {

						if (util.enabled(event.getPlayer())
								&& util.getSwears().stream().parallel().anyMatch(org.apache.commons.lang3.StringUtils
										.stripAccents(component.getJson()).toLowerCase()::contains)) {
							component.setJson(org.apache.commons.lang3.StringUtils
									.stripAccents(util.filter(component.getJson())));
							chatComponents.write(0, component);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});

	}

}
