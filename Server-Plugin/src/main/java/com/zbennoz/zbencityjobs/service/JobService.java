package com.zbennoz.zbencityjobs.service;

import com.zbennoz.zbencityjobs.ZBenCityJobs;
import com.zbennoz.zbencityjobs.model.Job;
import com.zbennoz.zbencityjobs.model.JobCreationSession;
import com.zbennoz.zbencityjobs.model.JobStatus;
import com.zbennoz.zbencityjobs.model.JobType;
import com.zbennoz.zbencityjobs.repository.JobRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobService {
    private final ZBenCityJobs plugin;
    private final JobRepository repository;
    private final EconomyService economyService;
    private final AuditService auditService;
    private final boolean escrowRequired;
    private final boolean asyncWrites;
    private final Map<Integer, Job> cache = new ConcurrentHashMap<>();

    public JobService(ZBenCityJobs plugin, JobRepository repository, EconomyService economyService, AuditService auditService,
                      boolean escrowRequired, boolean asyncWrites) {
        this.plugin = plugin;
        this.repository = repository;
        this.economyService = economyService;
        this.auditService = auditService;
        this.escrowRequired = escrowRequired;
        this.asyncWrites = asyncWrites;
    }

    public void loadCache() {
        try {
            repository.loadAll().forEach(job -> cache.put(job.getId(), job));
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load jobs: " + e.getMessage());
        }
    }

    public Collection<Job> getJobs() {
        return cache.values();
    }

    public Optional<Job> getJob(int id) {
        return Optional.ofNullable(cache.get(id));
    }

    public Optional<Job> createJob(Player requester, JobCreationSession session) {
        boolean escrow = escrowRequired;
        if (escrow && economyService.hasEconomy()) {
            if (!economyService.withdraw(requester.getUniqueId(), session.getReward())) {
                return Optional.empty();
            }
        }
        Job job = new Job(session.getType(), requester.getUniqueId(), session.getReward(), escrow, session.getDescription(), session.getDeliveryItem());
        try {
            int id = repository.insert(job);
            job.setId(id);
            cache.put(id, job);
            auditService.log(requester.getUniqueId(), "job.create", "job=" + id + ",type=" + session.getType());
            return Optional.of(job);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create job: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean takeJob(int id, Player worker) {
        Optional<Job> optional = getJob(id);
        if (optional.isEmpty()) return false;
        Job job = optional.get();
        if (job.getStatus() != JobStatus.OPEN || job.getWorker() != null) return false;
        job.setWorker(worker.getUniqueId());
        job.setStatus(JobStatus.IN_PROGRESS);
        save(job);
        auditService.log(worker.getUniqueId(), "job.take", "job=" + id);
        return true;
    }

    public boolean submitJob(int id, Player actor) {
        Optional<Job> optional = getJob(id);
        if (optional.isEmpty()) return false;
        Job job = optional.get();
        if (job.getStatus() == JobStatus.CANCELLED || job.getStatus() == JobStatus.COMPLETED) return false;

        if (job.getType() == JobType.DELIVERY) {
            if (!actor.getUniqueId().equals(job.getWorker())) return false;
            ItemStack requirement = job.getDeliveryItem();
            if (requirement == null) return false;
            ItemStack stack = requirement.clone();
            if (!actor.getInventory().containsAtLeast(stack, stack.getAmount())) {
                return false;
            }
            actor.getInventory().removeItem(stack);
            completeJob(job);
            return true;
        } else {
            if (job.getStatus() == JobStatus.IN_PROGRESS && actor.getUniqueId().equals(job.getWorker())) {
                job.setStatus(JobStatus.SUBMITTED);
                save(job);
                auditService.log(actor.getUniqueId(), "job.submit", "job=" + id);
                return true;
            }
            if (job.getStatus() == JobStatus.SUBMITTED && actor.getUniqueId().equals(job.getRequester())) {
                completeJob(job);
                return true;
            }
        }
        return false;
    }

    public boolean cancelJob(int id, Player actor) {
        Optional<Job> optional = getJob(id);
        if (optional.isEmpty()) return false;
        Job job = optional.get();
        if (!actor.getUniqueId().equals(job.getRequester())) return false;
        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED) return false;
        job.setStatus(JobStatus.CANCELLED);
        save(job);
        if (job.isEscrow() && economyService.hasEconomy()) {
            economyService.deposit(job.getRequester(), job.getReward());
        }
        auditService.log(actor.getUniqueId(), "job.cancel", "job=" + id);
        return true;
    }

    private void completeJob(Job job) {
        job.setStatus(JobStatus.COMPLETED);
        save(job);
        if (job.isEscrow() && economyService.hasEconomy() && job.getWorker() != null) {
            economyService.deposit(job.getWorker(), job.getReward());
        }
        auditService.log(job.getRequester(), "job.complete", "job=" + job.getId());
    }

    private void save(Job job) {
        Runnable task = () -> {
            try {
                repository.update(job);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update job: " + e.getMessage());
            }
        };
        if (asyncWrites) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            task.run();
        }
    }
}
