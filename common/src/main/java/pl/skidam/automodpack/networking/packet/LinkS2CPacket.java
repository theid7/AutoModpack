package pl.skidam.automodpack.networking.packet;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import pl.skidam.automodpack.TextHelper;
import pl.skidam.automodpack.mixin.ServerLoginNetworkHandlerAccessor;

import java.util.UUID;

import static pl.skidam.automodpack.networking.fabric.ModPacketsImpl.acceptLogin;

public class LinkS2CPacket {
    public static void receive(MinecraftServer server, ServerLoginNetworkHandler handler, boolean b, PacketByteBuf buf, ServerLoginNetworking.LoginSynchronizer sync, PacketSender sender) {
        GameProfile profile = ((ServerLoginNetworkHandlerAccessor) handler).getGameProfile();
        UUID uniqueId = profile.getId();

        if (buf.readBoolean()) { // disconnect
            Text reason = TextHelper.literal("[AutoModpack] Install/Update modpack to join");
            acceptLogin.add(uniqueId);
            ClientConnection connection = ((ServerLoginNetworkHandlerAccessor) handler).getConnection();
            connection.send(new LoginDisconnectS2CPacket(reason));
            connection.disconnect(reason);
        }
    }
    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        if (buf.readBoolean()) { // disconnect
            Text reason = TextHelper.literal("[AutoModpack] Install/Update modpack to join");
            handler.connection.send(new DisconnectS2CPacket(reason));
            handler.connection.disconnect(reason);
        }

        // otherwise just join the server
    }
}
