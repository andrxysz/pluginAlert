package br.plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

public final class pluginalert extends JavaPlugin implements PluginMessageListener {

    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String AC = "AlertChannel";
    private static final String ALL_TARGET = "ALL";
    private String prefix;
    private String setinha;
    private boolean espacoEntreAlert;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        prefixLoader();
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, BUNGEE_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, BUNGEE_CHANNEL, this);
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, BUNGEE_CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, BUNGEE_CHANNEL, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("alert")) {
            return false;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /alert <servidor|here|all> <mensagem>");
            return true;
        }

        String target = args[0];
        String message = alertFormat(buildMessage(args));

        if ("here".equalsIgnoreCase(target)) {
            broadcastAlert(message);
            sender.sendMessage(ChatColor.GREEN + "Alert enviado neste servidor.");
            return true;
        }

        String targetServer = "all".equalsIgnoreCase(target) ? ALL_TARGET : target;
        Player carrier = getAnyOnlinePlayer();
        if (carrier == null) {
            sender.sendMessage(ChatColor.RED + "Precisa de ao menos 1 jogador online para enviar entre servidores.");
            return true;
        }

        try {
            serverAlertsender(carrier, targetServer, message);
            if (ALL_TARGET.equals(targetServer)) {
                sender.sendMessage(ChatColor.GREEN + "Alert enviado para todos os servidores.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Alert enviado para " + target + ".");
            }
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Não foi possivel enviar o alert.");
            getLogger().severe("Houve um erro ao encaminhar o alert para: " + target + ": " + e.getMessage());
        }

        return true;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!BUNGEE_CHANNEL.equals(channel)) {
            return;
        }

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            String subChannel = in.readUTF();
            if (!AC.equals(subChannel)) {
                return;
            }
            short length = in.readShort();
            byte[] payload = new byte[length];
            in.readFully(payload);

            DataInputStream payloadIn = new DataInputStream(new ByteArrayInputStream(payload));
            String message = payloadIn.readUTF();
            broadcastAlert(message);
        } catch (IOException e) {
            getLogger().severe("Houve um erro ao processar a mensagem: " + e.getMessage());
        }
    }

    private void serverAlertsender(Player carrier, String targetServer, String message) throws IOException {
        ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
        DataOutputStream payloadOut = new DataOutputStream(payloadBytes);
        payloadOut.writeUTF(message);

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(outBytes);
        out.writeUTF("Forward");
        out.writeUTF(targetServer);
        out.writeUTF(AC);
        out.writeShort(payloadBytes.size());
        out.write(payloadBytes.toByteArray());

        carrier.sendPluginMessage(this, BUNGEE_CHANNEL, outBytes.toByteArray());
    }

    private String buildMessage(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private Player getAnyOnlinePlayer() {
        Iterator<? extends Player> iterator = Bukkit.getOnlinePlayers().iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    private void prefixLoader() {
        prefix = getConfig().getString("prefix", "SERVER");
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "SERVER";
        }

        setinha = getConfig().getString("setinha", ">");
        if (setinha == null || setinha.trim().isEmpty()) { //f
            setinha = ">";
        }

        espacoEntreAlert = getConfig().getBoolean("espaco-entre-alert", true);
    }

    private String alertFormat(String message) {
        return ChatColor.translateAlternateColorCodes('&', "&b&l" + prefix + " &f&l" + setinha + " " + message);
    }

    private void broadcastAlert(String message) {
        if (espacoEntreAlert) {
            Bukkit.broadcastMessage(" ");
        }
        Bukkit.broadcastMessage(message);
        if (espacoEntreAlert) {
            Bukkit.broadcastMessage(" ");
        }
    }
}
