package earth.terrarium.heracles.common.network.packets.quests;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.screens.quests.QuestsWidget;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.resources.ResourceLocation;

public record ClientboundAddQuestPacket(
    String id, Quest quest
) implements Packet<ClientboundAddQuestPacket> {

    public static final ClientboundPacketType<ClientboundAddQuestPacket> TYPE = new Type();

    @Override
    public PacketType<ClientboundAddQuestPacket> type() {
        return TYPE;
    }

    private static class Type implements ClientboundPacketType<ClientboundAddQuestPacket>, CodecPacketType<ClientboundAddQuestPacket> {

        private static final ByteCodec<ClientboundAddQuestPacket> CODEC = ObjectByteCodec.create(
            ByteCodec.STRING.fieldOf(ClientboundAddQuestPacket::id),
            ModUtils.toByteCodec(Quest.CODEC).fieldOf(ClientboundAddQuestPacket::quest),
            ClientboundAddQuestPacket::new
        );

        @Override
        public Class<ClientboundAddQuestPacket> type() {
            return ClientboundAddQuestPacket.class;
        }

        @Override
        public ResourceLocation id() {
            return new ResourceLocation(Heracles.MOD_ID, "add_client_quest");
        }

        @Override
        public ByteCodec<ClientboundAddQuestPacket> codec() {
            return CODEC;
        }

        @Override
        public Runnable handle(ClientboundAddQuestPacket message) {
            return () -> {
                ClientQuests.get(message.id).ifPresentOrElse(existing -> {
                    Quest existingQuest = existing.value();
                    Quest newQuest = message.quest;
                    existingQuest.display().setIcon(newQuest.display().icon());
                    existingQuest.display().setIconBackground(newQuest.display().iconBackground());
                    existingQuest.display().setTitle(newQuest.display().title());
                    existingQuest.display().setSubtitle(newQuest.display().subtitle());
                    existingQuest.display().setDescription(newQuest.display().description());
                    existingQuest.display().groups().clear();
                    existingQuest.display().groups().putAll(newQuest.display().groups());
                    existingQuest.settings().update(newQuest.settings());
                    existingQuest.dependencies().clear();
                    existingQuest.dependencies().addAll(newQuest.dependencies());
                    existingQuest.tasks().clear();
                    existingQuest.tasks().putAll(newQuest.tasks());
                    existingQuest.rewards().clear();
                    existingQuest.rewards().putAll(newQuest.rewards());
                    existing.dependencies().clear();
                    for (String depId : existingQuest.dependencies()) {
                        ClientQuests.get(depId).ifPresent(depEntry -> existing.dependencies().add(depEntry));
                    }
                    existing.dependents().clear();
                    for (ClientQuests.QuestEntry e : ClientQuests.entries()) {
                        if (!e.value().dependencies().contains(existing.key())) continue;
                        existing.dependents().add(e);
                    }
                    QuestsWidget.refreshOpenScreens();
                }, () -> ClientQuests.addQuest(message.id, message.quest));
                QuestsWidget.refreshOpenScreens();
            };
        }
    }
}
