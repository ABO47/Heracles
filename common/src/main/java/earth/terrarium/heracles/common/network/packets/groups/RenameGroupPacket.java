package earth.terrarium.heracles.common.network.packets.groups;

import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.api.quests.GroupDisplay;
import earth.terrarium.heracles.common.handlers.quests.QuestHandler;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.function.Consumer;

public record RenameGroupPacket(String oldName, String newName) implements Packet<RenameGroupPacket> {
    public static final ServerboundPacketType<RenameGroupPacket> TYPE = new Type();

    @Override
    public PacketType<RenameGroupPacket> type() {
        return TYPE;
    }

    private static class Type implements ServerboundPacketType<RenameGroupPacket> {
        @Override
        public Class<RenameGroupPacket> type() {
            return RenameGroupPacket.class;
        }

        @Override
        public ResourceLocation id() {
            return new ResourceLocation(Heracles.MOD_ID, "rename_group");
        }

        @Override
        public void encode(RenameGroupPacket message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.oldName);
            buffer.writeUtf(message.newName);
        }

        @Override
        public RenameGroupPacket decode(FriendlyByteBuf buffer) {
            return new RenameGroupPacket(buffer.readUtf(), buffer.readUtf());
        }

        @Override
        public Consumer<Player> handle(RenameGroupPacket message) {
            return player -> {
                if (!(player instanceof ServerPlayer serverPlayer) || !ModUtils.canEdit(serverPlayer)) return;
                String oldName = message.oldName().trim();
                String newName = message.newName().trim();
                if (oldName.isEmpty() || newName.isEmpty() || oldName.equals(newName)) return;
                List<String> groups = QuestHandler.groups();
                int idx = groups.indexOf(oldName);
                if (idx < 0 || groups.contains(newName)) return;
                groups.set(idx, newName);

                QuestHandler.quests().forEach((id, quest) -> {
                    GroupDisplay display = quest.display().groups().remove(oldName);
                    if (display == null) return;
                    quest.display().groups().put(newName, display.withId(newName));
                    QuestHandler.markDirty(id);
                });

                QuestHandler.saveGroups();
            };
        }
    }
}
