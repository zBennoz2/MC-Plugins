package com.zbennoz.zbencityjobs.util;

import com.zbennoz.zbencityjobs.ZBenCityJobs;
import com.zbennoz.zbencityjobs.model.City;
import com.zbennoz.zbencityjobs.model.Company;
import com.zbennoz.zbencityjobs.model.Job;
import com.zbennoz.zbencityjobs.model.JobStatus;
import com.zbennoz.zbencityjobs.service.CityService;
import com.zbennoz.zbencityjobs.service.CoinService;
import com.zbennoz.zbencityjobs.service.CompanyService;
import com.zbennoz.zbencityjobs.service.JobService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DisplayManager {
    private final ZBenCityJobs plugin;
    private final CoinService coinService;
    private final CityService cityService;
    private final CompanyService companyService;
    private final JobService jobService;

    private final boolean scoreboardEnabled;
    private final String sidebarTitle;
    private final long refreshTicks;
    private final SidebarLines sidebarLines;
    private final String tabHeader;
    private final String tabFooter;

    public DisplayManager(ZBenCityJobs plugin, CoinService coinService, CityService cityService,
                          CompanyService companyService, JobService jobService) {
        this.plugin = plugin;
        this.coinService = coinService;
        this.cityService = cityService;
        this.companyService = companyService;
        this.jobService = jobService;

        this.scoreboardEnabled = plugin.getConfig().getBoolean("scoreboard.enabled", true);
        this.sidebarTitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("scoreboard.title", "§bCityJobs"));
        this.refreshTicks = plugin.getConfig().getLong("scoreboard.refresh-ticks", 100L);
        this.sidebarLines = new SidebarLines(
                plugin.getConfig().getString("scoreboard.lines.coins", "§7Coins: §6{coins}"),
                plugin.getConfig().getString("scoreboard.lines.city", "§7Stadt: §b{city}"),
                plugin.getConfig().getString("scoreboard.lines.company", "§7Firma: §3{company}"),
                plugin.getConfig().getString("scoreboard.lines.job", "§7Job: §e{job}")
        );

        this.tabHeader = plugin.getConfig().getString("branding.tab.header", "§bZBen Network");
        this.tabFooter = plugin.getConfig().getString("branding.tab.footer", "§7Viel Spaß auf dem Server!");
    }

    public void start() {
        if (refreshTicks <= 0) return;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> Bukkit.getOnlinePlayers().forEach(this::updateDisplays), 20L, refreshTicks);
    }

    public void updateDisplays(Player player) {
        updateTab(player);
        if (scoreboardEnabled) {
            updateSidebar(player);
        }
    }

    private void updateTab(Player player) {
        String cityName = getCityName(player.getUniqueId());
        String companyName = getCompanyName(player.getUniqueId());
        String jobStatus = getJobStatus(player.getUniqueId());
        String header = formatPlaceholders(tabHeader, player, cityName, companyName, jobStatus);
        String footer = formatPlaceholders(tabFooter, player, cityName, companyName, jobStatus);
        player.sendPlayerListHeaderAndFooter(Component.text(ChatColor.translateAlternateColorCodes('&', header)),
                Component.text(ChatColor.translateAlternateColorCodes('&', footer)));
    }

    private void updateSidebar(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("zbencity", "dummy", sidebarTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = buildLines(player.getUniqueId());
        int score = lines.size();
        for (String line : lines) {
            objective.getScore(line).setScore(score--);
        }
        player.setScoreboard(scoreboard);
    }

    private List<String> buildLines(UUID uuid) {
        String cityName = getCityName(uuid);
        String companyName = getCompanyName(uuid);
        String jobStatus = getJobStatus(uuid);
        String coinLine = formatLine(sidebarLines.coins(), uuid, cityName, companyName, jobStatus);
        String cityLine = formatLine(sidebarLines.city(), uuid, cityName, companyName, jobStatus);
        String companyLine = formatLine(sidebarLines.company(), uuid, cityName, companyName, jobStatus);
        String jobLine = formatLine(sidebarLines.job(), uuid, cityName, companyName, jobStatus);
        List<String> lines = new ArrayList<>();
        lines.add(coinLine);
        lines.add(cityLine + ChatColor.RESET);
        lines.add(companyLine + ChatColor.WHITE);
        lines.add(jobLine + ChatColor.GRAY);
        return lines;
    }

    private String formatLine(String template, UUID uuid, String cityName, String companyName, String jobStatus) {
        return ChatColor.translateAlternateColorCodes('&', template)
                .replace("{coins}", coinService.formatAmount(coinService.getBalance(uuid)))
                .replace("{currency}", coinService.getCurrencyName())
                .replace("{city}", cityName)
                .replace("{company}", companyName)
                .replace("{job}", jobStatus);
    }

    private String formatPlaceholders(String template, Player player, String cityName, String companyName, String jobStatus) {
        return template
                .replace("{player}", player.getName())
                .replace("{coins}", coinService.formatAmount(coinService.getBalance(player.getUniqueId())))
                .replace("{currency}", coinService.getCurrencyName())
                .replace("{city}", cityName)
                .replace("{company}", companyName)
                .replace("{job}", jobStatus);
    }

    private String getCityName(UUID uuid) {
        Optional<City> city = cityService.findCityForPlayer(uuid);
        return city.map(City::getName).orElse("Keine Stadt");
    }

    private String getCompanyName(UUID uuid) {
        Optional<Company> company = companyService.findForPlayer(uuid);
        return company.map(Company::getName).orElse("Keine Firma");
    }

    private String getJobStatus(UUID uuid) {
        Optional<Job> job = jobService.findActiveJob(uuid);
        if (job.isEmpty()) return "Kein Auftrag";
        Job active = job.get();
        String status;
        if (active.getStatus() == JobStatus.IN_PROGRESS) {
            status = "In Arbeit";
        } else if (active.getStatus() == JobStatus.SUBMITTED) {
            status = "Abgabe prüfen";
        } else {
            status = active.getStatus().name();
        }
        return "#" + active.getId() + " - " + status;
    }

    private record SidebarLines(String coins, String city, String company, String job) {
    }
}

