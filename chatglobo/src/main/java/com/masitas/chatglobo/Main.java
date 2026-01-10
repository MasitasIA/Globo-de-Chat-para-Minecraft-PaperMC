package com.masitas.chatglobo;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
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

    // --- VARIABLES ---
    private boolean globalActivo = true;
    private double alturaGlobo = 0.625;
    private int tiempoVida = 5; // Tiempo por defecto en segundos
    
    // Lista 1: Preferencia del usuario
    private final Set<UUID> usuariosOcultos = new HashSet<>();
    // Lista 2: Castigo de admin
    private final Set<UUID> usuariosMuteados = new HashSet<>(); 

    // --- INICIO Y CIERRE ---
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        globalActivo = getConfig().getBoolean("global-activo", true);
        alturaGlobo = getConfig().getDouble("altura-globo", 0.625);
        tiempoVida = getConfig().getInt("tiempo-vida", 5); // Cargar tiempo
        
        cargarLista("usuarios-ocultos", usuariosOcultos);
        cargarLista("usuarios-muteados", usuariosMuteados);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("¡ChatGlobo cargado! Duración configurada: " + tiempoVida + "s");
    }

    @Override
    public void onDisable() {
        limpiarTodosLosGlobos();
        guardarDatos();
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
        getConfig().set("tiempo-vida", tiempoVida); // Guardar tiempo
        
        List<String> listaOcultos = new ArrayList<>();
        for (UUID uuid : usuariosOcultos) listaOcultos.add(uuid.toString());
        getConfig().set("usuarios-ocultos", listaOcultos);

        List<String> listaMuteados = new ArrayList<>();
        for (UUID uuid : usuariosMuteados) listaMuteados.add(uuid.toString());
        getConfig().set("usuarios-muteados", listaMuteados);
        
        saveConfig();
    }

    // --- COMANDOS ---
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        
        // 1. GLOBAL (Admin)
        if (command.getName().equalsIgnoreCase("globoglobal")) {
            if (!sender.hasPermission("chatglobo.admin")) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            globalActivo = !globalActivo; 
            guardarDatos(); 
            if (globalActivo) getServer().broadcast(Component.text("🎈 Globos de chat ACTIVADOS globalmente.", NamedTextColor.GREEN));
            else {
                getServer().broadcast(Component.text("🎈 Globos de chat DESACTIVADOS globalmente.", NamedTextColor.RED));
                limpiarTodosLosGlobos();
            }
            return true;
        }

        // 2. ALTURA (Admin)
        if (command.getName().equalsIgnoreCase("globoaltura")) {
            if (!sender.hasPermission("chatglobo.admin")) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            if (args.length == 0) {
                 sender.sendMessage(Component.text("Uso: /globoaltura <numero>", NamedTextColor.RED));
                 return true;
            }
            try {
                alturaGlobo = Double.parseDouble(args[0]);
                guardarDatos();
                sender.sendMessage(Component.text("🎈 Altura guardada: " + alturaGlobo, NamedTextColor.GREEN));
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Error: Número inválido.", NamedTextColor.RED));
            }
            return true;
        }

        // 3. TIEMPO (Admin) - NUEVO COMANDO
        if (command.getName().equalsIgnoreCase("globotiempo")) {
            if (!sender.hasPermission("chatglobo.admin")) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }

            // Si está vacío, resetear a 5 segundos
            if (args.length == 0) {
                tiempoVida = 5;
                guardarDatos();
                sender.sendMessage(Component.text("🎈 Tiempo restaurado al defecto: 5 segundos.", NamedTextColor.GREEN));
                return true;
            }

            try {
                int nuevosSegundos = Integer.parseInt(args[0]);
                if (nuevosSegundos < 1) nuevosSegundos = 1; // Mínimo 1 segundo
                
                tiempoVida = nuevosSegundos;
                guardarDatos();
                sender.sendMessage(Component.text("🎈 Los globos ahora durarán " + tiempoVida + " segundos.", NamedTextColor.GREEN));
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Error: Debes poner un número entero.", NamedTextColor.RED));
            }
            return true;
        }

        // 4. MUTE (Admin)
        if (command.getName().equalsIgnoreCase("globomute")) {
            if (!sender.hasPermission("chatglobo.admin")) {
                sender.sendMessage(Component.text("No tienes permiso.", NamedTextColor.RED));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(Component.text("Uso: /globomute <jugador>", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
                return true;
            }
            
            UUID id = target.getUniqueId();
            if (usuariosMuteados.contains(id)) {
                usuariosMuteados.remove(id);
                sender.sendMessage(Component.text("🎈 Has DESMUTEADO a " + target.getName(), NamedTextColor.GREEN));
                target.sendMessage(Component.text("🎈 Un admin ha permitido tus globos de nuevo.", NamedTextColor.GREEN));
            } else {
                usuariosMuteados.add(id);
                sender.sendMessage(Component.text("🎈 Has MUTEADO a " + target.getName(), NamedTextColor.RED));
                target.sendMessage(Component.text("🎈 Un admin ha bloqueado tus globos de chat.", NamedTextColor.RED));
            }
            guardarDatos();
            return true;
        }

        // 5. CLEAR (Admin)
        if (command.getName().equalsIgnoreCase("globoclear")) {
            if (!sender.hasPermission("chatglobo.admin")) return true;
            int n = limpiarTodosLosGlobos();
            sender.sendMessage(Component.text("🎈 Eliminados " + n + " globos.", NamedTextColor.GREEN));
            return true;
        }

        // 6. PERSONAL (Jugador)
        if (command.getName().equalsIgnoreCase("globo")) {
            if (!(sender instanceof Player player)) return true;
            UUID id = player.getUniqueId();
            
            if (usuariosMuteados.contains(id)) {
                player.sendMessage(Component.text("❌ Tus globos están bloqueados por un administrador.", NamedTextColor.RED));
                return true;
            }
            
            if (usuariosOcultos.contains(id)) {
                usuariosOcultos.remove(id);
                player.sendMessage(Component.text("🎈 Globos personales ACTIVADOS.", NamedTextColor.GREEN));
            } else {
                usuariosOcultos.add(id);
                player.sendMessage(Component.text("🎈 Globos personales DESACTIVADOS.", NamedTextColor.YELLOW));
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
        UUID uuid = event.getPlayer().getUniqueId();
        
        if (usuariosMuteados.contains(uuid)) return;
        if (usuariosOcultos.contains(uuid)) return;

        Player player = event.getPlayer();
        String mensajePlano = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component mensajeConColores = LegacyComponentSerializer.legacyAmpersand().deserialize(mensajePlano);

        getServer().getScheduler().runTask(this, () -> spawnGlobo(player, mensajeConColores));
    }

    // --- AUXILIARES ---
    private int limpiarTodosLosGlobos() {
        int c = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            List<Entity> pasajeros = new ArrayList<>(p.getPassengers());
            for (Entity pas : pasajeros) {
                if (pas instanceof TextDisplay) {
                    pas.remove();
                    c++;
                }
            }
        }
        return c;
    }

    private void spawnGlobo(Player player, Component texto) {
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        if (player.hasMetadata("vanished")) return;
        if (player.isInvisible()) return;

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

        Transformation t = display.getTransformation();
        t.getTranslation().set(0, (float) alturaGlobo, 0); 
        display.setTransformation(t);

        player.addPassenger(display);
        
        // AQUI ESTA EL CAMBIO: Convertimos segundos a ticks (segundos * 20)
        long ticksVida = tiempoVida * 20L;
        getServer().getScheduler().runTaskLater(this, display::remove, ticksVida);
    }
}