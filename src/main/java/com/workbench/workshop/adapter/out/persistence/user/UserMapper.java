package com.workbench.workshop.adapter.out.persistence.user;

import com.workbench.workshop.domain.user.Group;
import com.workbench.workshop.domain.user.Role;
import com.workbench.workshop.domain.user.User;
import com.workbench.workshop.infrastructure.persistence.user.GroupEntity;
import com.workbench.workshop.infrastructure.persistence.user.RoleEntity;
import com.workbench.workshop.infrastructure.persistence.user.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toDomain(UserEntity entity);
    Role toDomain(RoleEntity entity);
    Group toDomain(GroupEntity entity);
}
