package com.masitas.chatglobo;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;

public class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("¡ChatGlobo funcionando!");
    }

    @EventHandler
    public void alChatear(AsyncChatEvent event) {
        Player player = event.getPlayer();

        String mensajePlano = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component mensajeConColores = LegacyComponentSerializer.legacyAmpersand().deserialize(mensajePlano);

        getServer().getScheduler().runTask(this, () -> {
            spawnGlobo(player, mensajeConColores);
        });
    }

    private void spawnGlobo(Player player, Component texto) {
        if (!player.getPassengers().isEmpty()) {
            player.getPassengers().forEach(p -> {
                if (p instanceof TextDisplay) p.remove();
            });
        }

        Location loc = player.getLocation();
        
        TextDisplay display = (TextDisplay) player.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);

        display.text(texto);
        display.setBackgroundColor(Color.fromARGB(160, 0, 0, 0));
        display.setBillboard(Display.Billboard.CENTER);

        // --- AJUSTE DE ALTURA REAL (TRANSFORMACIÓN) ---
        Transformation transformacion = display.getTransformation();
        
        transformacion.getTranslation().set(0, 0.25, 0);
        
        display.setTransformation(transformacion);

        player.addPassenger(display);

        getServer().getScheduler().runTaskLater(this, display::remove, 100L);
    }
}