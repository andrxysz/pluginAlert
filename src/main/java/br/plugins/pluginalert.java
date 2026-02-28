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
    private static final String ALERT_SUB_CHANNEL = "AlertChannel";
    private static final String PREFIX = ChatColor.YELLOW + "[Alert] " + ChatColor.RESET;

    @Override
    public void onEnable() {
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
            sender.sendMessage(ChatColor.RED + "Uso: /alert <servidor|here> <mensagem>");
            return true;
        }

        String target = args[0];
        String message = ChatColor.translateAlternateColorCodes('&', buildMessage(args, 1));

        if ("here".equalsIgnoreCase(target)) {
            Bukkit.broadcastMessage(message);
            sender.sendMessage(PREFIX + ChatColor.GREEN + "Alerta enviado para este servidor.");
            return true;
        }

        Player carrier = getAnyOnlinePlayer();
        if (carrier == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Precisa de ao menos 1 jogador online para enviar entre servidores.");
            return true;
        }

        try {
            sendAlertToServer(carrier, target, message);
            sender.sendMessage(PREFIX + ChatColor.GREEN + "Alerta enviado para " + target + ".");
        } catch (IOException e) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Falha ao encaminhar o alerta.");
            getLogger().severe("Erro ao enviar alerta para " + target + ": " + e.getMessage());
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
            if (!ALERT_SUB_CHANNEL.equals(subChannel)) {
                return;
            }

            short length = in.readShort();
            byte[] payload = new byte[length];
            in.readFully(payload);

            DataInputStream payloadIn = new DataInputStream(new ByteArrayInputStream(payload));
            String message = payloadIn.readUTF();
            Bukkit.broadcastMessage(message);
        } catch (IOException e) {
            getLogger().severe("Erro ao processar mensagem recebida: " + e.getMessage());
        }
    }

    private void sendAlertToServer(Player carrier, String targetServer, String message) throws IOException {
        ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
        DataOutputStream payloadOut = new DataOutputStream(payloadBytes);
        payloadOut.writeUTF(message);

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(outBytes);
        out.writeUTF("Forward");
        out.writeUTF(targetServer);
        out.writeUTF(ALERT_SUB_CHANNEL);
        out.writeShort(payloadBytes.size());
        out.write(payloadBytes.toByteArray());

        carrier.sendPluginMessage(this, BUNGEE_CHANNEL, outBytes.toByteArray());
    }

    private String buildMessage(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
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
}
