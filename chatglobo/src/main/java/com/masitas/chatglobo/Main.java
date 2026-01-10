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
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    // --- VARIABLES ---
    private boolean globalActivo = true;
    private double alturaGlobo = 0.05;
    private int tiempoVida = 5;
    private double separacionExtra = 0.05; 
    
    // Listas
    private final Set<UUID> usuariosOcultos = new HashSet<>();
    private final Set<UUID> usuariosMuteados = new HashSet<>(); 
    private final Pattern patronColores = Pattern.compile("&[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);

    // --- INICIO Y CIERRE ---
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Cargar configuración
        globalActivo = getConfig().getBoolean("global-activo", true);
        alturaGlobo = getConfig().getDouble("altura-globo", 0.625);
        tiempoVida = getConfig().getInt("tiempo-vida", 5);
        separacionExtra = getConfig().getDouble("separacion-extra", 0.05); 
        
        cargarLista("usuarios-ocultos", usuariosOcultos);
        cargarLista("usuarios-muteados", usuariosMuteados);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("¡ChatGlobo cargado! Modo Zona de Seguridad activo.");
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
        getConfig().set("tiempo-vida", tiempoVida);
        getConfig().set("separacion-extra", separacionExtra); 
        
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
        if (command.getName().equalsIgnoreCase("globoglobal")) {
            if (!sender.hasPermission("chatglobo.admin")) return noPermiso(sender);
            globalActivo = !globalActivo; 
            guardarDatos(); 
            if (globalActivo) getServer().broadcast(Component.text("🎈 Globos ACTIVADOS globalmente.", NamedTextColor.GREEN));
            else {
                getServer().broadcast(Component.text("🎈 Globos DESACTIVADOS globalmente.", NamedTextColor.RED));
                limpiarTodosLosGlobos();
            }
            return true;
        }

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

        if (command.getName().equalsIgnoreCase("globotiempo")) {
            if (!sender.hasPermission("chatglobo.admin")) return noPermiso(sender);
            if (args.length == 0) return true;
            try {
                int val = Integer.parseInt(args[0]);
                if (val < 1) val = 1;
                tiempoVida = val;
                guardarDatos();
                sender.sendMessage(Component.text("🎈 Tiempo: " + tiempoVida + "s", NamedTextColor.GREEN));
            } catch (NumberFormatException e) { }
            return true;
        }

        if (command.getName().equalsIgnoreCase("globoseparacion")) {
            if (!sender.hasPermission("chatglobo.admin")) return noPermiso(sender);
            if (args.length == 0) {
                sender.sendMessage(Component.text("Actual: " + separacionExtra, NamedTextColor.YELLOW));
                return true;
            }
            try {
                separacionExtra = Double.parseDouble(args[0]);
                guardarDatos();
                sender.sendMessage(Component.text("🎈 Separación: " + separacionExtra, NamedTextColor.GREEN));
            } catch (NumberFormatException e) { }
            return true;
        }

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

        if (command.getName().equalsIgnoreCase("globoclear")) {
            if (!sender.hasPermission("chatglobo.admin")) return true;
            limpiarTodosLosGlobos();
            return true;
        }

        if (command.getName().equalsIgnoreCase("globo")) {
            if (!(sender instanceof Player player)) return true;
            UUID id = player.getUniqueId();
            if (usuariosMuteados.contains(id)) return true;
            
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

    @EventHandler
    public void alChatear(AsyncChatEvent event) {
        if (!globalActivo) return;
        if (usuariosMuteados.contains(event.getPlayer().getUniqueId())) return;
        if (usuariosOcultos.contains(event.getPlayer().getUniqueId())) return;

        Player player = event.getPlayer();
        
        String textoCrudo = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        Component mensajeConColores = LegacyComponentSerializer.legacyAmpersand().deserialize(textoCrudo);

        String textoLimpio = patronColores.matcher(textoCrudo).replaceAll("");

        getServer().getScheduler().runTask(this, () -> spawnGlobo(player, mensajeConColores, textoLimpio));
    }

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

    // Cálculo de líneas según el ancho en píxeles
    private int calcularLineasExactas(String texto, int anchoLimite) {
        if (texto == null || texto.isEmpty()) return 1;
        
        int lineas = 1;
        int pixelesLineaActual = 0;
        
        String[] palabras = texto.split(" ");
        
        for (String palabra : palabras) {
            int anchoPalabra = 0;
            for (char c : palabra.toCharArray()) {
                anchoPalabra += obtenerAnchoPixel(c);
            }
            
            int espacio = (pixelesLineaActual == 0) ? 0 : 4;
            
            if (pixelesLineaActual + espacio + anchoPalabra <= anchoLimite) {
                pixelesLineaActual += espacio + anchoPalabra;
            } else {
                lineas++;
                pixelesLineaActual = anchoPalabra;
            }
        }
        return lineas;
    }

    private int obtenerAnchoPixel(char c) {
        if (c == ' ') return 4;
        if (c == 'i' || c == '!' || c == '.' || c == ',' || c == ':' || c == ';' || c == '|' || c == '\'') return 2;
        if (c == 'l' || c == '`' || c == '[' || c == ']') return 3;
        if (c == 'I' || c == 't' || c == '(' || c == ')') return 4;
        if (c == '@' || c == '~') return 7; 
        return 6;
    }

    private void spawnGlobo(Player player, Component textoComponent, String textoPlano) {
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        if (player.hasMetadata("vanished")) return;
        if (player.isInvisible()) return;

        // --- CONFIGURACIÓN DE SEGURIDAD ---
        int anchoEntidad = 200;  // Ancho REAL del globo en el juego
        int anchoCalculo = 170;  // Ancho REDUCIDO para el cálculo matemático (Zona de Seguridad)

        float alturaPorLinea = 0.25f; // Altura un poco más generosa (0.25 bloque = 1/4 bloque)
        float paddingBase = 0.2f;     // Margen fijo de seguridad (arriba y abajo)

        int maxGlobos = 3;

        // 1. CÁLCULO
        int lineasReales = calcularLineasExactas(textoPlano, anchoCalculo);
        
        float alturaNuevoGlobo = (lineasReales * alturaPorLinea) + paddingBase;
        float empujeRequerido = alturaNuevoGlobo + (float) separacionExtra; 

        // 2. MOVER VIEJOS
        List<Entity> pasajeros = new ArrayList<>(player.getPassengers());
        List<TextDisplay> globosViejos = new ArrayList<>();

        for (Entity p : pasajeros) {
            if (p instanceof TextDisplay td) {
                globosViejos.add(td);
            }
        }

        while (globosViejos.size() >= maxGlobos) {
            globosViejos.get(0).remove();
            globosViejos.remove(0);
        }

        for (TextDisplay viejo : globosViejos) {
            Transformation t = viejo.getTransformation();
            float nuevaY = t.getTranslation().y() + empujeRequerido;
            t.getTranslation().set(0, nuevaY, 0);
            viejo.setInterpolationDuration(3);
            viejo.setTransformation(t);
        }

        // 3. GENERAR NUEVO
        Location loc = player.getLocation();
        TextDisplay display = (TextDisplay) player.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);

        display.text(textoComponent);
        display.setLineWidth(anchoEntidad); // Usamos el ancho REAL (200) para mostrarlo
        display.setBackgroundColor(Color.fromARGB(160, 0, 0, 0));
        display.setAlignment(TextDisplay.TextAlignment.CENTER); 
        display.setBillboard(Display.Billboard.CENTER);

        Transformation transformacion = display.getTransformation();
        float mitadAltura = alturaNuevoGlobo / 2.0f;
        transformacion.getTranslation().set(0, (float) alturaGlobo + mitadAltura, 0); 
        display.setTransformation(transformacion);

        player.addPassenger(display);
        
        long ticksVida = tiempoVida * 20L;
        getServer().getScheduler().runTaskLater(this, display::remove, ticksVida);
    }
}