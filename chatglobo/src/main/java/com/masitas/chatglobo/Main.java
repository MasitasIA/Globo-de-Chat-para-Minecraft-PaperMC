package com.masitas.chatglobo;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    // --- VARIABLES ---
    private boolean globalActivo = true;
    private double alturaGlobo = 0.625; // Altura por defecto
    private final Set<UUID> usuariosOcultos = new HashSet<>();

    // --- INICIO Y CIERRE ---
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Cargar variables simples
        globalActivo = getConfig().getBoolean("global-activo", true);
        alturaGlobo = getConfig().getDouble("altura-globo", 0.625);
        
        // Cargar lista de usuarios
        List<String> listaGuardada = getConfig().getStringList("usuarios-ocultos");
        for (String idString : listaGuardada) {
            try {
                usuariosOcultos.add(UUID.fromString(idString));
            } catch (IllegalArgumentException e) {
                // Ignorar ID inválido
            }
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("¡ChatGlobo cargado! Altura actual: " + alturaGlobo);
    }

    @Override
    public void onDisable() {
        guardarDatos();
    }

    private void guardarDatos() {
        getConfig().set("global-activo", globalActivo);
        getConfig().set("altura-globo", alturaGlobo);
        
        List<String> listaParaGuardar = new ArrayList<>();
        for (UUID uuid : usuariosOcultos) {
            listaParaGuardar.add(uuid.toString());
        }
        getConfig().set("usuarios-ocultos", listaParaGuardar);
        
        saveConfig();
    }

    // --- COMANDOS ---
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        
        // COMANDO GLOBAL
        if (command.getName().equalsIgnoreCase("globoglobal")) {
            if (!sender.hasPermission("chatglobo.admin")) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            
            globalActivo = !globalActivo; 
            guardarDatos(); 
            
            if (globalActivo) {
                getServer().broadcast(Component.text("🎈 Globos de chat ACTIVADOS globalmente.", NamedTextColor.GREEN));
            } else {
                getServer().broadcast(Component.text("🎈 Globos de chat DESACTIVADOS globalmente.", NamedTextColor.RED));
            }
            return true;
        }

        // COMANDO DE ALTURA
        if (command.getName().equalsIgnoreCase("globoaltura")) {
            if (!sender.hasPermission("chatglobo.admin")) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(Component.text("Uso: /globoaltura <numero> (Ej: 0.6)", NamedTextColor.RED));
                return true;
            }

            try {
                double nuevaAltura = Double.parseDouble(args[0]);
                alturaGlobo = nuevaAltura;
                guardarDatos();
                sender.sendMessage(Component.text("🎈 Altura guardada: " + alturaGlobo, NamedTextColor.GREEN));
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Error: Debes poner un número válido (usa punto, no coma).", NamedTextColor.RED));
            }
            return true;
        }

        // COMANDO PERSONAL
        if (command.getName().equalsIgnoreCase("globo")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Solo jugadores.", NamedTextColor.RED));
                return true;
            }

            UUID id = player.getUniqueId();
            
            if (usuariosOcultos.contains(id)) {
                usuariosOcultos.remove(id);
                player.sendMessage(Component.text("🎈 Ahora tus mensajes MOSTRARÁN globos.", NamedTextColor.GREEN));
            } else {
                usuariosOcultos.add(id);
                player.sendMessage(Component.text("🎈 Ahora tus mensajes NO mostrarán globos.", NamedTextColor.YELLOW));
            }
            guardarDatos();
            return true;
        }
        
        return false;
    }

    // --- EVENTOS ---
    @EventHandler
    public void alChatear(AsyncChatEvent event) {
        if (!globalActivo) return;
        if (usuariosOcultos.contains(event.getPlayer().getUniqueId())) return;

        Player player = event.getPlayer();
        String mensajePlano = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component mensajeConColores = LegacyComponentSerializer.legacyAmpersand().deserialize(mensajePlano);

        getServer().getScheduler().runTask(this, () -> {
            spawnGlobo(player, mensajeConColores);
        });
    }

    // --- MÉTODOS ---
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

        Transformation transformacion = display.getTransformation();
        // Aquí usamos la variable "alturaGlobo" en lugar del número fijo
        transformacion.getTranslation().set(0, (float) alturaGlobo, 0); 
        display.setTransformation(transformacion);

        player.addPassenger(display);

        getServer().getScheduler().runTaskLater(this, display::remove, 100L);
    }
}