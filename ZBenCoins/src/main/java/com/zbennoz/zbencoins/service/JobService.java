package com.zbennoz.zbencoins.service;

import com.zbennoz.zbencoins.ZBenCoinsPlugin;
import com.zbennoz.zbencoins.database.PlayerDao;
import com.zbennoz.zbencoins.database.TransactionDao;
import com.zbennoz.zbencoins.job.JobDao;
import com.zbennoz.zbencoins.job.JobLogDao;
import com.zbennoz.zbencoins.job.JobRecord;
import com.zbennoz.zbencoins.job.JobStatus;
import com.zbennoz.zbencoins.job.JobQueryOptions;
import com.zbennoz.zbencoins.job.JobType;
import com.zbennoz.zbencoins.util.InventoryUtil;
import com.zbennoz.zbencoins.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Kernlogik für das Jobs-System inklusive Escrow.
 */
public class JobService {

    private final ZBenCoinsPlugin plugin;
    private final JobDao jobDao;
    private final JobLogDao logDao;
    private final PlayerDao playerDao;
    private final TransactionDao transactionDao;
    private final Connection connection;
    private final Map<UUID, JobDraft> drafts = new ConcurrentHashMap<>();
    private final Map<UUID, DraftInput> awaitingInput = new ConcurrentHashMap<>();
    private final Map<UUID, JobQueryOptions> browseOptions = new ConcurrentHashMap<>();
    private final Map<UUID, BrowseInput> browseAwaiting = new ConcurrentHashMap<>();

    private enum BrowseInput {
        SEARCH
    }

    public JobService(ZBenCoinsPlugin plugin, JobDao jobDao, JobLogDao logDao, PlayerDao playerDao,
                      TransactionDao transactionDao, Connection connection) {
        this.plugin = plugin;
        this.jobDao = jobDao;
        this.logDao = logDao;
        this.playerDao = playerDao;
        this.transactionDao = transactionDao;
        this.connection = connection;
        startExpiryTask();
    }

    public JobDraft startDraft(Player player, JobType type) {
        JobDraft draft = new JobDraft(type);
        int defaultHours = plugin.getConfig().getInt("jobs.default-expiry-hours", 48);
        draft.setExpiresAt(Instant.now().plus(defaultHours, ChronoUnit.HOURS));
        drafts.put(player.getUniqueId(), draft);
        return draft;
    }

    public Optional<JobDraft> getDraft(Player player) {
        return Optional.ofNullable(drafts.get(player.getUniqueId()));
    }

    public void clearDraft(Player player) {
        drafts.remove(player.getUniqueId());
        awaitingInput.remove(player.getUniqueId());
    }

    public JobQueryOptions getBrowseOptions(UUID playerId) {
        return browseOptions.computeIfAbsent(playerId, id -> new JobQueryOptions());
    }

    public List<JobRecord> listFiltered(JobQueryOptions options) {
        try {
            List<JobRecord> jobs = jobDao.findAllOpen();
            String term = options.getSearchTerm().toLowerCase(Locale.ROOT);
            return jobs.stream()
                    .filter(job -> term.isBlank() || job.getTitle().toLowerCase(Locale.ROOT).contains(term)
                            || job.getDescription().toLowerCase(Locale.ROOT).contains(term))
                    .filter(job -> options.getTypeFilter() == null || job.getType() == options.getTypeFilter())
                    .filter(job -> options.getStatusFilter() == null || job.getStatus() == options.getStatusFilter())
                    .sorted(resolveComparator(options))
                    .toList();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Jobs nicht filtern", e);
            return List.of();
        }
    }

    private Comparator<JobRecord> resolveComparator(JobQueryOptions options) {
        return switch (options.getSortOption()) {
            case BELOHNUNG_ABSTEIGEND -> Comparator.comparingLong(JobRecord::getReward).reversed();
            case BELOHNUNG_AUFSTEIGEND -> Comparator.comparingLong(JobRecord::getReward);
            case ABLAUFEND -> Comparator.comparing(job -> Optional.ofNullable(job.getExpiresAt()).orElse(Instant.MAX));
            case NEUESTE -> Comparator.comparing(JobRecord::getCreatedAt).reversed();
        };
    }

    public void requestInput(Player player, DraftInput input) {
        awaitingInput.put(player.getUniqueId(), input);
        player.closeInventory();
        player.sendMessage(plugin.getConfigManager().message(input.promptKey));
    }

    public boolean handleChat(Player player, String message) {
        BrowseInput browseInput = browseAwaiting.remove(player.getUniqueId());
        if (browseInput == BrowseInput.SEARCH) {
            JobQueryOptions options = getBrowseOptions(player.getUniqueId());
            options.setSearchTerm(message);
            options.setPage(0);
            player.sendMessage(Text.colorize("&aJobsuche gesetzt."));
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getGuiManager().openGui(player,
                            new com.zbennoz.zbencoins.gui.JobBrowseGui(plugin, this, options.copy(), player)));
            return true;
        }
        DraftInput input = awaitingInput.remove(player.getUniqueId());
        if (input == null) {
            return false;
        }
        Optional<JobDraft> optionalDraft = getDraft(player);
        if (optionalDraft.isEmpty()) {
            return false;
        }
        JobDraft draft = optionalDraft.get();
        switch (input) {
            case TITLE -> draft.setTitle(message.trim());
            case DESCRIPTION -> draft.setDescription(message.trim());
            case REWARD -> handleReward(player, draft, message);
            case AMOUNT -> handleAmount(player, draft, message);
            case EXPIRY -> handleExpiry(player, draft, message);
        }
        Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getGuiManager().openGui(player,
                        new com.zbennoz.zbencoins.gui.JobCreateGui(plugin, this, player)));
        return true;
    }

    public void requestBrowseSearch(Player player) {
        browseAwaiting.put(player.getUniqueId(), BrowseInput.SEARCH);
        player.sendMessage(Text.colorize("&eGib einen Titel oder eine Beschreibung zur Suche ein."));
        player.closeInventory();
    }

    private void handleReward(Player player, JobDraft draft, String message) {
        try {
            long reward = Long.parseLong(message.trim());
            if (reward <= 0) {
                player.sendMessage(plugin.getConfigManager().message("invalid-amount"));
                return;
            }
            draft.setReward(reward);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().message("invalid-amount"));
        }
    }

    private void handleAmount(Player player, JobDraft draft, String message) {
        try {
            int amount = Integer.parseInt(message.trim());
            if (amount <= 0) {
                player.sendMessage(plugin.getConfigManager().message("invalid-amount"));
                return;
            }
            draft.setItemAmount(amount);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().message("invalid-amount"));
        }
    }

    private void handleExpiry(Player player, JobDraft draft, String message) {
        try {
            int hours = Integer.parseInt(message.trim());
            if (hours <= 0) {
                draft.setExpiresAt(null);
                return;
            }
            draft.setExpiresAt(Instant.now().plus(hours, ChronoUnit.HOURS));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().message("invalid-amount"));
        }
    }

    public Optional<String> publish(Player player) {
        Optional<JobDraft> optionalDraft = getDraft(player);
        if (optionalDraft.isEmpty()) {
            return Optional.of(plugin.getConfigManager().message("job-no-draft"));
        }
        JobDraft draft = optionalDraft.get();
        if (!player.hasPermission("zbencoins.jobs.create")) {
            return Optional.of(plugin.getConfigManager().message("no-permission"));
        }
        if (draft.getTitle().isBlank() || draft.getDescription().isBlank() || draft.getReward() <= 0) {
            return Optional.of(plugin.getConfigManager().message("job-missing"));
        }
        if (draft.getType() == JobType.LIEFERUNG) {
            if (draft.getItemType() == null || draft.getItemAmount() <= 0) {
                return Optional.of(plugin.getConfigManager().message("job-delivery-missing"));
            }
        }
        try {
            int active = jobDao.countActiveForCreator(player.getUniqueId());
            int max = resolveMaxCreator(player);
            if (active >= max) {
                return Optional.of(plugin.getConfigManager().message("job-limit-creator"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Joblimit nicht prüfen", e);
            return Optional.of(plugin.getConfigManager().message("error"));
        }

        long balance = plugin.getCoinService().getBalance(player.getUniqueId());
        if (balance < draft.getReward()) {
            return Optional.of(plugin.getConfigManager().message("not-enough-coins"));
        }

        try {
            connection.setAutoCommit(false);
            playerDao.addCoins(player.getUniqueId(), -draft.getReward());
            JobRecord record = jobDao.insert(draft.getType(), draft.getTitle(), draft.getDescription(), draft.getReward(),
                    player.getUniqueId(), player.getName(), draft.getExpiresAt(), draft.getItemType(),
                    draft.getItemAmount());
            transactionDao.insert(player.getUniqueId(), "JOB_ESCROW", -draft.getReward(),
                    "Job #" + record.getId() + " eingestellt");
            logDao.log(record.getId(), "CREATE", player.getUniqueId(), player.getName(),
                    "Job erstellt (" + draft.getType() + ")");
            connection.commit();
            connection.setAutoCommit(true);
            clearDraft(player);
            return Optional.empty();
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Rollback fehlgeschlagen", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Konnte Job nicht speichern", e);
            return Optional.of(plugin.getConfigManager().message("error"));
        }
    }

    public Optional<String> accept(Player player, JobRecord record) {
        if (!player.hasPermission("zbencoins.jobs.accept")) {
            return Optional.of(plugin.getConfigManager().message("no-permission"));
        }
        if (record.getCreatorUuid().equals(player.getUniqueId())) {
            return Optional.of(plugin.getConfigManager().message("job-own"));
        }
        try {
            int active = jobDao.countActiveForAssignee(player.getUniqueId());
            int max = resolveMaxAssignee(player);
            if (active >= max) {
                return Optional.of(plugin.getConfigManager().message("job-limit-assignee"));
            }
            connection.setAutoCommit(false);
            boolean reserved = jobDao.acceptJob(record.getId(), player.getUniqueId(), player.getName());
            if (!reserved) {
                connection.rollback();
                connection.setAutoCommit(true);
                return Optional.of(plugin.getConfigManager().message("job-not-available"));
            }
            logDao.log(record.getId(), "ACCEPT", player.getUniqueId(), player.getName(), "Job angenommen");
            connection.commit();
            connection.setAutoCommit(true);
            notifyCreator(record.getCreatorUuid(), plugin.getConfigManager().message("job-accepted"));
            return Optional.empty();
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Rollback fehlgeschlagen", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Job annehmen fehlgeschlagen", e);
            return Optional.of(plugin.getConfigManager().message("error"));
        }
    }

    public Optional<String> cancel(Player actor, JobRecord record) {
        if (!record.getCreatorUuid().equals(actor.getUniqueId()) &&
                !actor.hasPermission("zbencoins.jobs.admin")) {
            return Optional.of(plugin.getConfigManager().message("no-permission"));
        }
        if (record.getStatus() != JobStatus.OFFEN && record.getStatus() != JobStatus.ANGENOMMEN) {
            return Optional.of(plugin.getConfigManager().message("job-not-open"));
        }
        try {
            connection.setAutoCommit(false);
            boolean updated = jobDao.updateStatus(record.getId(), record.getStatus(), JobStatus.ABGEBROCHEN);
            if (!updated) {
                connection.rollback();
                connection.setAutoCommit(true);
                return Optional.of(plugin.getConfigManager().message("job-not-available"));
            }
            refundCreator(record);
            logDao.log(record.getId(), "CANCEL", actor.getUniqueId(), actor.getName(), "Job abgebrochen");
            connection.commit();
            connection.setAutoCommit(true);
            return Optional.empty();
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Rollback fehlgeschlagen", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Konnte Job nicht abbrechen", e);
            return Optional.of(plugin.getConfigManager().message("error"));
        }
    }

    public Optional<String> requestCompletion(Player assignee, JobRecord record) {
        if (!assignee.getUniqueId().equals(record.getAssigneeUuid())) {
            return Optional.of(plugin.getConfigManager().message("no-permission"));
        }
        if (record.getType() != JobType.DIENSTLEISTUNG || record.getStatus() != JobStatus.ANGENOMMEN) {
            return Optional.of(plugin.getConfigManager().message("job-not-open"));
        }
        try {
            if (!jobDao.requestCompletion(record.getId())) {
                return Optional.of(plugin.getConfigManager().message("job-not-available"));
            }
            logDao.log(record.getId(), "REQUEST", assignee.getUniqueId(), assignee.getName(), "Abschluss angefragt");
            notifyCreator(record.getCreatorUuid(), plugin.getConfigManager().message("job-completion-requested"));
            return Optional.empty();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Abschluss nicht anfragen", e);
            return Optional.of(plugin.getConfigManager().message("error"));
        }
    }

    public Optional<String> completeService(Player creator, JobRecord record) {
        if (!creator.getUniqueId().equals(record.getCreatorUuid())) {
            return Optional.of(plugin.getConfigManager().message("no-permission"));
        }
        if (record.getStatus() != JobStatus.ANGENOMMEN || record.getType() != JobType.DIENSTLEISTUNG) {
            return Optional.of(plugin.getConfigManager().message("job-not-open"));
        }
        if (record.getAssigneeUuid() == null) {
            return Optional.of(plugin.getConfigManager().message("job-not-available"));
        }
        try {
            connection.setAutoCommit(false);
            boolean updated = jobDao.updateStatusWithRequest(record.getId(), JobStatus.ANGENOMMEN,
                    JobStatus.ABGESCHLOSSEN, false);
            if (!updated) {
                connection.rollback();
                connection.setAutoCommit(true);
                return Optional.of(plugin.getConfigManager().message("job-not-available"));
            }
            payout(record.getAssigneeUuid(), record.getReward(), "JOB_DONE", "Job #" + record.getId() + " abgeschlossen");
            logDao.log(record.getId(), "COMPLETE", creator.getUniqueId(), creator.getName(), "Dienstleistung bestätigt");
            connection.commit();
            connection.setAutoCommit(true);
            return Optional.empty();
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Rollback fehlgeschlagen", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Konnte Dienstleistung nicht abschließen", e);
            return Optional.of(plugin.getConfigManager().message("error"));
        }
    }

    public Optional<String> completeDelivery(Player assignee, JobRecord record) {
        if (!assignee.getUniqueId().equals(record.getAssigneeUuid())) {
            return Optional.of(plugin.getConfigManager().message("no-permission"));
        }
        if (record.getType() != JobType.LIEFERUNG || record.getStatus() != JobStatus.ANGENOMMEN) {
            return Optional.of(plugin.getConfigManager().message("job-not-open"));
        }
        if (record.getItemType() == null || record.getItemAmount() <= 0) {
            return Optional.of(plugin.getConfigManager().message("job-delivery-missing"));
        }
        ItemStack template = new ItemStack(record.getItemType(), 1);
        if (!InventoryUtil.hasEnough(assignee, template, record.getItemAmount())) {
            return Optional.of(plugin.getConfigManager().message("not-enough-items"));
        }
        if (!InventoryUtil.remove(assignee, template, record.getItemAmount())) {
            return Optional.of(plugin.getConfigManager().message("not-enough-items"));
        }
        try {
            connection.setAutoCommit(false);
            boolean updated = jobDao.updateStatus(record.getId(), JobStatus.ANGENOMMEN, JobStatus.ABGESCHLOSSEN);
            if (!updated) {
                connection.rollback();
                connection.setAutoCommit(true);
                ItemStack refund = new ItemStack(record.getItemType(), record.getItemAmount());
                InventoryUtil.giveItem(assignee, refund);
                return Optional.of(plugin.getConfigManager().message("job-not-available"));
            }
            payout(record.getAssigneeUuid(), record.getReward(), "JOB_DONE", "Lieferjob #" + record.getId());
            logDao.log(record.getId(), "DELIVER", assignee.getUniqueId(), assignee.getName(), "Lieferung abgegeben");
            connection.commit();
            connection.setAutoCommit(true);
            return Optional.empty();
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Rollback fehlgeschlagen", ex);
            }
            InventoryUtil.giveItem(assignee, new ItemStack(record.getItemType(), record.getItemAmount()));
            plugin.getLogger().log(Level.SEVERE, "Konnte Lieferung nicht abschließen", e);
            return Optional.of(plugin.getConfigManager().message("error"));
        }
    }

    public List<JobRecord> listOpen(int page, int size) {
        try {
            return jobDao.findOpen(size, Math.max(page, 0) * size);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte offene Jobs nicht lesen", e);
            return List.of();
        }
    }

    public Optional<JobRecord> findById(int id) {
        try {
            return jobDao.findById(id);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Job konnte nicht geladen werden", e);
            return Optional.empty();
        }
    }

    public List<JobRecord> listCreated(Player player) {
        try {
            return jobDao.findForCreator(player.getUniqueId(), JobStatus.values());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte eigene Jobs nicht laden", e);
            return List.of();
        }
    }

    public List<JobRecord> listAccepted(Player player) {
        try {
            return jobDao.findForAssignee(player.getUniqueId(), JobStatus.values());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte angenommene Jobs nicht laden", e);
            return List.of();
        }
    }

    public List<String> recentLogs(Player player) {
        int limit = plugin.getConfig().getInt("jobs.history-limit", 10);
        try {
            return logDao.findRecentForPlayer(player.getUniqueId(), limit);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte Job-Logs nicht laden", e);
            return List.of();
        }
    }

    public void applyItem(Player player) {
        getDraft(player).ifPresent(draft -> {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType() == Material.AIR) {
                player.sendMessage(plugin.getConfigManager().message("no-item-in-hand"));
                return;
            }
            draft.setItemType(inHand.getType());
            draft.setItemAmount(inHand.getAmount());
            player.sendMessage(plugin.getConfigManager().message("job-item-set"));
        });
    }

    private void payout(UUID target, long reward, String type, String note) throws SQLException {
        playerDao.addCoins(target, reward);
        transactionDao.insert(target, type, reward, note);
    }

    private void refundCreator(JobRecord record) throws SQLException {
        playerDao.addCoins(record.getCreatorUuid(), record.getReward());
        transactionDao.insert(record.getCreatorUuid(), "JOB_REFUND", record.getReward(),
                "Job #" + record.getId() + " storniert");
    }

    private void notifyCreator(UUID creator, String message) {
        Player online = Bukkit.getPlayer(creator);
        if (online != null) {
            online.sendMessage(Text.colorize(message));
        }
    }

    private void startExpiryTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (JobRecord record : jobDao.findExpiredActive()) {
                        connection.setAutoCommit(false);
                        boolean updated = jobDao.updateStatus(record.getId(), record.getStatus(), JobStatus.ABGELAUFEN);
                        if (updated) {
                            refundCreator(record);
                            logDao.log(record.getId(), "EXPIRE", null, null, "Job abgelaufen");
                            notifyCreator(record.getCreatorUuid(), plugin.getConfigManager().message("job-expired"));
                            if (record.getAssigneeUuid() != null) {
                                notifyCreator(record.getAssigneeUuid(), plugin.getConfigManager().message("job-expired"));
                            }
                        }
                        connection.commit();
                        connection.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Fehler beim Job-Ablauf", e);
                    try {
                        connection.rollback();
                        connection.setAutoCommit(true);
                    } catch (SQLException ex) {
                        plugin.getLogger().log(Level.SEVERE, "Rollback fehlgeschlagen", ex);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L * 60L);
    }

    public int resolveMaxCreator(Player player) {
        if (player.hasPermission("zbencoins.jobs.limit.creator.bypass")) {
            return Integer.MAX_VALUE;
        }
        int max = plugin.getConfig().getInt("jobs.default-max-creator", 5);
        for (int i = 1; i <= 64; i++) {
            if (player.hasPermission("zbencoins.jobs.limit.creator." + i)) {
                max = Math.max(max, i);
            }
        }
        return max;
    }

    public int resolveMaxAssignee(Player player) {
        if (player.hasPermission("zbencoins.jobs.limit.worker.bypass")) {
            return Integer.MAX_VALUE;
        }
        int max = plugin.getConfig().getInt("jobs.default-max-assignee", 3);
        for (int i = 1; i <= 64; i++) {
            if (player.hasPermission("zbencoins.jobs.limit.worker." + i)) {
                max = Math.max(max, i);
            }
        }
        return max;
    }

    public enum DraftInput {
        TITLE("job-enter-title"),
        DESCRIPTION("job-enter-description"),
        REWARD("job-enter-reward"),
        AMOUNT("job-enter-amount"),
        EXPIRY("job-enter-expiry");

        final String promptKey;

        DraftInput(String promptKey) {
            this.promptKey = promptKey;
        }
    }

    public static class JobDraft {
        private JobType type;
        private String title = "";
        private String description = "";
        private long reward;
        private Instant expiresAt;
        private Material itemType;
        private int itemAmount;

        public JobDraft(JobType type) {
            this.type = type;
        }

        public JobType getType() {
            return type;
        }

        public void setType(JobType type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public long getReward() {
            return reward;
        }

        public void setReward(long reward) {
            this.reward = reward;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }

        public Material getItemType() {
            return itemType;
        }

        public void setItemType(Material itemType) {
            this.itemType = itemType;
        }

        public int getItemAmount() {
            return itemAmount;
        }

        public void setItemAmount(int itemAmount) {
            this.itemAmount = itemAmount;
        }
    }
}
