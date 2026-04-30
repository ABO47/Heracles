package earth.terrarium.heracles.client.screens.quest;

import earth.terrarium.heracles.api.client.settings.SettingInitializer;
import earth.terrarium.heracles.api.client.settings.Settings;
import earth.terrarium.heracles.api.rewards.QuestReward;
import earth.terrarium.heracles.api.rewards.QuestRewardType;
import earth.terrarium.heracles.api.tasks.QuestTask;
import earth.terrarium.heracles.api.tasks.QuestTaskType;
import earth.terrarium.heracles.client.widgets.modals.EditObjectModal;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class QuestObjectModalOps {
    private QuestObjectModalOps() {
    }

    static <T extends QuestTask<?, ?, T>> void taskPopup(
        QuestTaskType<T> type,
        String id,
        @Nullable T task,
        Consumer<T> consumer,
        Supplier<EditObjectModal> findOrCreateEditWidget,
        Component title
    ) {
        SettingInitializer<?> setting = Settings.getFactory(type);
        if (setting == null) return;

        SettingInitializer.CreationData data = setting.create(ModUtils.cast(task));
        if (data.isEmpty()) {
            var newTask = setting.create(id, ModUtils.cast(task), new SettingInitializer.Data());
            if (newTask == null) return;
            consumer.accept(ModUtils.cast(newTask));
        } else {
            EditObjectModal widget = findOrCreateEditWidget.get();
            widget.init(type.id(), data, savedData -> {
                var newTask = setting.create(id, ModUtils.cast(task), savedData);
                if (newTask == null) return;
                consumer.accept(ModUtils.cast(newTask));
            });
            widget.setTitle(title);
        }
    }

    static <T extends QuestReward<T>> void rewardPopup(
        QuestRewardType<T> type,
        String id,
        @Nullable T reward,
        Consumer<T> consumer,
        Supplier<EditObjectModal> findOrCreateEditWidget,
        Component title
    ) {
        SettingInitializer<?> setting = Settings.getFactory(type);
        if (setting == null) return;

        SettingInitializer.CreationData data = setting.create(ModUtils.cast(reward));
        if (data.isEmpty()) {
            var newReward = setting.create(id, ModUtils.cast(reward), new SettingInitializer.Data());
            if (newReward == null) return;
            consumer.accept(ModUtils.cast(newReward));
        } else {
            EditObjectModal widget = findOrCreateEditWidget.get();
            widget.init(type.id(), data, savedData -> {
                var newReward = setting.create(id, ModUtils.cast(reward), savedData);
                if (newReward == null) return;
                consumer.accept(ModUtils.cast(newReward));
            });
            widget.setTitle(title);
        }
    }
}
