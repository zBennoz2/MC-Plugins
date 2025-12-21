package com.zbennoz.zbencityjobs.service;

import com.zbennoz.zbencityjobs.model.Company;
import com.zbennoz.zbencityjobs.model.CompanyRole;
import com.zbennoz.zbencityjobs.repository.CompanyRepository;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class CompanyService {
    private final CompanyRepository repository;
    private final AuditService auditService;

    public CompanyService(CompanyRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public Optional<Company> createCompany(Player creator, String name) {
        Company company = new Company(name, creator.getUniqueId());
        try {
            int id = repository.insert(company);
            company.setId(id);
            auditService.log(creator.getUniqueId(), "company.create", "company=" + name);
            return Optional.of(company);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public Optional<Company> findByName(String name) {
        try {
            return repository.findByName(name);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public Optional<Company> findOwnedCompany(Player owner) {
        try {
            return repository.findByOwner(owner.getUniqueId());
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public boolean invite(Company company, Player actor, UUID target) {
        if (!company.getOwner().equals(actor.getUniqueId())) return false;
        company.getMembers().put(target, CompanyRole.MEMBER);
        try {
            repository.upsertMember(company.getId(), target, CompanyRole.MEMBER);
        } catch (SQLException e) {
            return false;
        }
        auditService.log(actor.getUniqueId(), "company.invite", "company=" + company.getName());
        return true;
    }

    public boolean kick(Company company, Player actor, UUID target) {
        if (!company.getOwner().equals(actor.getUniqueId())) return false;
        company.getMembers().remove(target);
        try {
            repository.removeMember(company.getId(), target);
        } catch (SQLException e) {
            return false;
        }
        auditService.log(actor.getUniqueId(), "company.kick", "company=" + company.getName());
        return true;
    }

    public boolean setRole(Company company, Player actor, UUID target, CompanyRole role) {
        if (!company.getOwner().equals(actor.getUniqueId())) return false;
        company.getMembers().put(target, role);
        try {
            repository.upsertMember(company.getId(), target, role);
        } catch (SQLException e) {
            return false;
        }
        auditService.log(actor.getUniqueId(), "company.role", "company=" + company.getName() + ",role=" + role);
        return true;
    }
}
