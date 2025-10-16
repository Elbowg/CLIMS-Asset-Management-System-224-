package com.clims.backend.security;

import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Asset;
import org.springframework.stereotype.Component;

@Component
public class AssetSecurity {
    public boolean isAdmin(AppUser user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public boolean canCreate(AppUser user, Long departmentId) {
        if (user == null) return false;
        Role r = user.getRole();
        if (r == Role.ADMIN || r == Role.IT_STAFF) return true;
        if (r == Role.MANAGER) {
            return user.getDepartment() != null && departmentId != null && user.getDepartment().getId().equals(departmentId);
        }
        return false;
    }

    public boolean canModify(AppUser user, Asset asset) {
        if (user == null) return false;
        Role r = user.getRole();
        if (r == Role.ADMIN || r == Role.IT_STAFF) return true;
        if (r == Role.MANAGER) {
            return asset.getDepartment() != null && user.getDepartment() != null && asset.getDepartment().getId().equals(user.getDepartment().getId());
        }
        return false;
    }

    public boolean canDispose(AppUser user, Asset asset) {
        // finance allowed to dispose
        if (user == null) return false;
        if (user.getRole() == Role.FINANCE) return true;
        return canModify(user, asset);
    }
}
