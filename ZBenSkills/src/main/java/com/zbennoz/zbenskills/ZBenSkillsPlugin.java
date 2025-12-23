package com.zbennoz.zbenskills;

import com.zbennoz.zbenskills.command.SkillsCommand;
import com.zbennoz.zbenskills.config.SkillConfig;
import com.zbennoz.zbenskills.gui.InventoryController;
import com.zbennoz.zbenskills.listener.PlayerDataListener;
import com.zbennoz.zbenskills.listener.SkillPerkListener;
import com.zbennoz.zbenskills.listener.SkillXpListener;
import com.zbennoz.zbenskills.service.AntiExploitService;
import com.zbennoz.zbenskills.service.ChallengeService;
import com.zbennoz.zbenskills.service.MessageService;
import com.zbennoz.zbenskills.service.SkillBenefitService;
import com.zbennoz.zbenskills.service.SkillService;
import com.zbennoz.zbenskills.storage.PlayerSkillRepository;
import org.bukkit.plugin.java.JavaPlugin;

public class ZBenSkillsPlugin extends JavaPlugin {

    private SkillConfig skillConfig;
    private PlayerSkillRepository repository;
    private SkillService skillService;
    private ChallengeService challengeService;
    private AntiExploitService antiExploitService;
    private InventoryController inventoryController;
    private MessageService messages;
    private SkillBenefitService benefitService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("achievements.yml", false);
        this.messages = new MessageService(this);
        this.skillConfig = new SkillConfig(this);
        this.repository = new PlayerSkillRepository(this);
        this.antiExploitService = new AntiExploitService(this);
        this.challengeService = new ChallengeService(this, repository, skillConfig);
        this.skillService = new SkillService(this, repository, skillConfig, antiExploitService, challengeService, messages);
        this.benefitService = new SkillBenefitService(skillConfig, repository);
        this.inventoryController = new InventoryController(this, skillService, skillConfig, challengeService, messages, benefitService);

        SkillsCommand skillsCommand = new SkillsCommand(inventoryController, skillService, skillConfig, repository, messages, benefitService);
        getCommand("skills").setExecutor(skillsCommand);
        getCommand("skills").setTabCompleter(skillsCommand);

        getServer().getPluginManager().registerEvents(new SkillXpListener(skillService, skillConfig), this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(repository, skillService, skillConfig), this);
        getServer().getPluginManager().registerEvents(new SkillPerkListener(this, benefitService), this);

        getLogger().info("ZBenSkills enabled with " + skillConfig.getSkillTypes().size() + " skills.");
    }

    @Override
    public void onDisable() {
        repository.flush();
        repository.close();
    }

    public SkillService getSkillService() {
        return skillService;
    }

    public SkillConfig getSkillConfig() {
        return skillConfig;
    }

    public PlayerSkillRepository getRepository() {
        return repository;
    }

    public MessageService getMessages() {
        return messages;
    }
}
