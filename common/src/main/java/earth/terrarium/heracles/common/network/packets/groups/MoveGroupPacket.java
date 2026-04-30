package earth.terrarium.heracles.common.network.packets.groups;

import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.common.handlers.quests.QuestHandler;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.Consumer;

public record MoveGroupPacket(String group, int delta) implements Packet<MoveGroupPacket> {
    public static final ServerboundPacketType<MoveGroupPacket> TYPE = new Type();

    @Override
    public PacketType<MoveGroupPacket> type() {
        return TYPE;
    }

    private static class Type implements ServerboundPacketType<MoveGroupPacket> {
        @Override
        public Class<MoveGroupPacket> type() {
            return MoveGroupPacket.class;
        }

        @Override
        public ResourceLocation id() {
            return new ResourceLocation(Heracles.MOD_ID, "move_group");
        }

        @Override
        public void encode(MoveGroupPacket message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.group);
            buffer.writeInt(message.delta);
        }

        @Override
        public MoveGroupPacket decode(FriendlyByteBuf buffer) {
            return new MoveGroupPacket(buffer.readUtf(), buffer.readInt());
        }

        @Override
        public Consumer<Player> handle(MoveGroupPacket message) {
            return player -> {
                if (!(player instanceof ServerPlayer serverPlayer) || !ModUtils.canEdit(serverPlayer)) return;
                List<String> groups = QuestHandler.groups();
                int idx = groups.indexOf(message.group());
                if (idx < 0) return;
                int target = idx + Integer.signum(message.delta());
                if (target < 0 || target >= groups.size() || target == idx) return;
                String moving = groups.remove(idx);
                groups.add(target, moving);
                QuestHandler.saveGroups();
            };
        }
    }
}
