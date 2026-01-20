package com.masitas.chatglobo;

// --- Importaciones ---
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
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

    // --- CONSTANTES ---
    private static final int ANCHO_GLOBO = 200;
    
    // --- VARIABLES CONFIG ---
    private boolean globalActivo = true;
    private double alturaGlobo = 0.25;
    private int tiempoVida = 5;
    private long ticksAparicion = 1; 
    
    // --- LISTAS ---
    private final Set<UUID> usuariosOcultos = new HashSet<>();
    private final Set<UUID> usuariosMuteados = new HashSet<>(); 
    private final Set<UUID> globosEstaticos = new HashSet<>();
    
    // Herramientas
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    // --- Inicio y cierre ---
    @Override
    public void onEnable() {
        saveDefaultConfig();
        cargarConfiguracion();
        
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("¡ChatGlobo cargado! Versión Minimalista.");
    }

    @Override
    public void onDisable() {
        limpiarTodosLosGlobos();
        guardarDatos();
    }

    // --- Lógica de carga de configuración ---
    private void cargarConfiguracion() {
        reloadConfig();
        
        globalActivo = getConfig().getBoolean("global-activo", true);
        alturaGlobo = getConfig().getDouble("altura-globo", 0.25);
        tiempoVida = getConfig().getInt("tiempo-vida", 5);
        ticksAparicion = getConfig().getLong("ticks-aparicion", 10); // Cargamos el delay
        
        // Evitar números negativos
        if (ticksAparicion < 0) ticksAparicion = 0;

        usuariosOcultos.clear();
        usuariosMuteados.clear();
        cargarLista("usuarios-ocultos", usuariosOcultos);
        cargarLista("usuarios-muteados", usuariosMuteados);
    }

    private void cargarLista(String path, Set<UUID> setDestino) {
        List<String> lista = getConfig().getStringList(path);
        for (String idString : lista) {
            try {
                setDestino.add(UUID.fromString(idString));
            } catch (IllegalArgumentException e) { /* Ignorar */ }
        }
    }

    private void guardarDatos() {
        getConfig().set("global-activo", globalActivo);
        getConfig().set("altura-globo", alturaGlobo);
        getConfig().set("tiempo-vida", tiempoVida);
        getConfig().set("ticks-aparicion", ticksAparicion); // Guardamos el delay
        
        List<String> listaOcultos = new ArrayList<>();
        for (UUID uuid : usuariosOcultos) listaOcultos.add(uuid.toString());
        getConfig().set("usuarios-ocultos", listaOcultos);

        List<String> listaMuteados = new ArrayList<>();
        for (UUID uuid : usuariosMuteados) listaMuteados.add(uuid.toString());
        getConfig().set("usuarios-muteados", listaMuteados);
        
        saveConfig();
    }

    // --- Comandos ---
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        
        // Reload
        if (command.getName().equalsIgnoreCase("globoreload")) {
            if (!sender.hasPermission("chatglobo.admin")) return noPermiso(sender);
            
            cargarConfiguracion(); 
            sender.sendMessage(Component.text("✅ Configuración recargada correctamente.", NamedTextColor.GREEN));
            return true;
        }

        // Globo Spawn
        if (command.getName().equalsIgnoreCase("globospawn")) {
            if (!sender.hasPermission("chatglobo.admin")) return noPermiso(sender);
            if (!(sender instanceof Player player)) return true;
            if (args.length == 0) return true;

            Block bloqueMirado = player.getTargetBlockExact(20);
            Location spawnLoc;

            if (bloqueMirado != null) {
                spawnLoc = bloqueMirado.getLocation().add(0.5, 1.5, 0.5);
            } else {
                spawnLoc = player.getLocation().add(0, 1.5, 0);
            }

            String textoCrudo = String.join(" ", args);
            Component mensaje = serializer.deserialize(textoCrudo);

            spawnGloboEstatico(spawnLoc, mensaje);
            return true;
        }

        // Clear
        if (command.getName().equalsIgnoreCase("globoclear")) {
            if (!sender.hasPermission("chatglobo.admin")) return true;
            int eliminados = limpiarTodosLosGlobos();
            sender.sendMessage(Component.text("🎈 Eliminados " + eliminados + " globos.", NamedTextColor.GREEN));
            return true;
        }

        // Global
        if (command.getName().equalsIgnoreCase("globoglobal")) {
            if (!sender.hasPermission("chatglobo.admin")) return noPermiso(sender);
            globalActivo = !globalActivo; 
            guardarDatos(); 
            sender.sendMessage(Component.text("🎈 Global: " + (globalActivo ? "ON" : "OFF"), globalActivo ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (!globalActivo) limpiarTodosLosGlobos();
            return true;
        }

        // Altura
        if (command.getName().equalsIgnoreCase("globoaltura")) {
            if (!sender.hasPermission("chatglobo.admin")) return noPermiso(sender);
            if (args.length == 0) return true;
            try {
                alturaGlobo = Double.parseDouble(args[0]);
                guardarDatos();
                sender.sendMessage(Component.text("🎈 Altura base: " + alturaGlobo, NamedTextColor.GREEN));
            } catch (NumberFormatException e) { }
            return true;
        }

        // Tiempo Vida
        if (command.getName().equalsIgnoreCase("globotiempo")) {
            if (!sender.hasPermission("chatglobo.admin")) return noPermiso(sender);
            if (args.length == 0) return true;
            try {
                int val = Integer.parseInt(args[0]);
                if (val < 1) val = 1;
                tiempoVida = val;
                guardarDatos();
                sender.sendMessage(Component.text("🎈 Tiempo vida: " + tiempoVida + "s", NamedTextColor.GREEN));
            } catch (NumberFormatException e) { }
            return true;
        }

        // NUEVO: Tiempo Delay (Aparición)
        if (command.getName().equalsIgnoreCase("globodelay")) {
            if (!sender.hasPermission("chatglobo.admin")) return noPermiso(sender);
            if (args.length == 0) {
                sender.sendMessage(Component.text("Uso: /globodelay <ticks> (20 ticks = 1 seg)", NamedTextColor.RED));
                return true;
            }
            try {
                long val = Long.parseLong(args[0]);
                if (val < 0) val = 0;
                ticksAparicion = val;
                guardarDatos();
                sender.sendMessage(Component.text("🎈 Delay aparición: " + ticksAparicion + " ticks", NamedTextColor.GREEN));
            } catch (NumberFormatException e) { }
            return true;
        }

        // Mutear
        if (command.getName().equalsIgnoreCase("globomute")) {
            if (!sender.hasPermission("chatglobo.admin")) return noPermiso(sender);
            if (args.length == 0) return true;
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) return true;
            UUID id = target.getUniqueId();
            if (usuariosMuteados.contains(id)) {
                usuariosMuteados.remove(id);
                sender.sendMessage(Component.text("🎈 DESMUTEADO: " + target.getName(), NamedTextColor.GREEN));
            } else {
                usuariosMuteados.add(id);
                sender.sendMessage(Component.text("🎈 MUTEADO: " + target.getName(), NamedTextColor.RED));
            }
            guardarDatos();
            return true;
        }

        // Personal toggle
        if (command.getName().equalsIgnoreCase("globo")) {
            if (!(sender instanceof Player player)) return true;
            UUID id = player.getUniqueId();
            
            if (usuariosOcultos.contains(id)) {
                usuariosOcultos.remove(id);
                player.sendMessage(Component.text("🎈 ACTIVADO.", NamedTextColor.GREEN));
            } else {
                usuariosOcultos.add(id);
                player.sendMessage(Component.text("🎈 DESACTIVADO.", NamedTextColor.YELLOW));
            }
            guardarDatos();
            return true;
        }

        return false;
    }

    private boolean noPermiso(CommandSender sender) {
        sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
        return true;
    }

    // --- Evento de chat ---
    @EventHandler
    public void alChatear(AsyncChatEvent event) {
        if (!globalActivo) return;
        if (usuariosMuteados.contains(event.getPlayer().getUniqueId())) return;
        if (usuariosOcultos.contains(event.getPlayer().getUniqueId())) return;

        Player player = event.getPlayer();
        String textoCrudo = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component mensaje = serializer.deserialize(textoCrudo);

        getServer().getScheduler().runTask(this, () -> spawnGloboJugador(player, mensaje));
    }

    // --- Limpieza ---
    private int limpiarTodosLosGlobos() {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Entity pas : p.getPassengers()) {
                if (pas instanceof TextDisplay) {
                    pas.remove();
                    count++;
                }
            }
        }
        for (UUID uuid : globosEstaticos) {
            Entity e = Bukkit.getEntity(uuid);
            if (e != null && !e.isDead()) {
                e.remove();
                count++;
            }
        }
        globosEstaticos.clear();
        return count;
    }

    // --- Helper creación de globos ---
    private TextDisplay crearGloboBase(Location location, Component texto) {
        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        display.text(texto);
        display.setLineWidth(ANCHO_GLOBO);
        display.setBackgroundColor(Color.fromARGB(160, 0, 0, 0));
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setBillboard(Display.Billboard.CENTER);
        return display;
    }

    // --- Spawn de globo para jugador ---
    private void spawnGloboJugador(Player player, Component textoComponent) {
        if (player.getGameMode() == GameMode.SPECTATOR || player.hasMetadata("vanished") || player.isInvisible()) return;

        // Limpiar anterior inmediatamente
        for (Entity e : player.getPassengers()) {
            if (e instanceof TextDisplay) e.remove();
        }

        // 1. Nace TOTALMENTE INVISIBLE
        TextDisplay display = player.getWorld().spawn(player.getLocation(), TextDisplay.class, entity -> {
            entity.setVisibleByDefault(false);
            
            entity.text(textoComponent);
            entity.setLineWidth(ANCHO_GLOBO);
            entity.setBackgroundColor(Color.fromARGB(160, 0, 0, 0));
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setBillboard(Display.Billboard.CENTER);
            
            // Transformación
            Transformation transformacion = entity.getTransformation();
            transformacion.getTranslation().set(0, (float) alturaGlobo, 0);
            entity.setTransformation(transformacion);
        });

        player.addPassenger(display);

        // 2. Usamos 'ticksAparicion' desde la config
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!display.isDead() && player.isOnline()) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.showEntity(this, display);
                }
            }
        }, ticksAparicion);

        // 3. Programar su muerte (Vida + Delay)
        getServer().getScheduler().runTaskLater(this, display::remove, (tiempoVida * 20L) + ticksAparicion);
    }

    // --- Spawn de globo estático ---
    private void spawnGloboEstatico(Location location, Component textoComponent) {
        TextDisplay display = crearGloboBase(location, textoComponent);
        display.setPersistent(false);
        globosEstaticos.add(display.getUniqueId());

        getServer().getScheduler().runTaskLater(this, () -> {
            if (!display.isDead()) {
                display.remove();
                globosEstaticos.remove(display.getUniqueId());
            }
        }, tiempoVida * 20L);
    }
}