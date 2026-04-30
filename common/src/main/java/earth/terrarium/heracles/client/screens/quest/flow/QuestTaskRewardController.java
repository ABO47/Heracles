package earth.terrarium.heracles.client.screens.quest.flow;

import earth.terrarium.heracles.api.client.settings.Settings;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.api.rewards.QuestReward;
import earth.terrarium.heracles.api.rewards.QuestRewardType;
import earth.terrarium.heracles.api.rewards.QuestRewards;
import earth.terrarium.heracles.api.tasks.QuestTask;
import earth.terrarium.heracles.api.tasks.QuestTaskType;
import earth.terrarium.heracles.api.tasks.QuestTasks;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.screens.quest.rewards.RewardListWidget;
import earth.terrarium.heracles.client.screens.quest.tasks.TaskListWidget;
import earth.terrarium.heracles.client.widgets.modals.CreateObjectModal;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.common.handlers.progress.QuestProgress;
import earth.terrarium.heracles.common.utils.ModUtils;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class QuestTaskRewardController {
    public record Widgets(TaskListWidget taskList, RewardListWidget rewardList) {
    }

    private QuestTaskRewardController() {
    }

    @FunctionalInterface
    public interface TaskCreator {
        void create(String id, QuestTaskType<?> type);
    }

    @FunctionalInterface
    public interface RewardCreator {
        void create(String id, QuestRewardType<?> type);
    }

    public static Widgets createWidgets(
        int contentX,
        int contentY,
        int contentWidth,
        int contentHeight,
        String contentId,
        String fromGroup,
        ClientQuests.QuestEntry entry,
        QuestProgress progress,
        Map<String, ModUtils.QuestStatus> quests,
        BiConsumer<QuestTask<?, ?, ?>, Boolean> onTaskClick,
        Runnable onTaskCreate,
        BiConsumer<QuestReward<?>, Boolean> onRewardClick,
        Runnable onRewardCreate
    ) {
        TaskListWidget taskList = new TaskListWidget(
            contentX,
            contentY,
            contentWidth,
            contentHeight,
            5.0D,
            5.0D,
            contentId,
            entry,
            progress,
            quests,
            onTaskClick,
            onTaskCreate
        );

        RewardListWidget rewardList = new RewardListWidget(
            contentX,
            contentY,
            contentWidth,
            contentHeight,
            5.0D,
            5.0D,
            entry,
            progress,
            onRewardClick,
            onRewardCreate
        );
        return new Widgets(taskList, rewardList);
    }

    public static void openTaskCreateModal(
        CreateObjectModal createModal,
        Supplier<Quest> questSupplier,
        TaskCreator creator
    ) {
        Quest quest = questSupplier.get();
        createModal.setVisible(true);
        createModal.update(
            "task",
            (type, id) -> creator.create(id, QuestTasks.get(type)),
            (type, id) -> !quest.tasks().containsKey(id) && QuestTasks.types().containsKey(type),
            ConstantComponents.Tasks.CREATE,
            QuestTasks.types().values()
                .stream()
                .filter(questTaskType -> Settings.getFactory(questTaskType) != null)
                .map(QuestTaskType::id)
                .toList()
        );
    }

    public static void openRewardCreateModal(
        CreateObjectModal createModal,
        Supplier<Quest> questSupplier,
        RewardCreator creator
    ) {
        Quest quest = questSupplier.get();
        createModal.setVisible(true);
        createModal.update(
            "reward",
            (type, id) -> creator.create(id, QuestRewards.get(type)),
            (type, id) -> !quest.rewards().containsKey(id) && QuestRewards.types().containsKey(type),
            ConstantComponents.Rewards.CREATE,
            QuestRewards.types().values()
                .stream()
                .filter(questRewardType -> Settings.getFactory(questRewardType) != null)
                .map(QuestRewardType::id)
                .toList()
        );
    }
}
