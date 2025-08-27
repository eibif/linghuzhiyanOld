package org.linghu.mybackend.constants;

public class UserConstants {
    /**
     * 用户角色枚举
     */
    public enum UserRole {
        ADMIN("管理员"),
        TEACHER("教师"),
        ASSISTANT("助教"),
        STUDENT("学生");

        private final String description;

        UserRole(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static String getDescription(UserRole role) {
            return role != null ? role.getDescription() : null;
        }
    }
    
}
