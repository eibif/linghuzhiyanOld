package org.linghu.mybackend.constants;

/**
 * 任务类型枚举
 */
public enum TaskType {
    CODE("编程题"),
    OTHER("其他");
    
    private final String description;
    
    TaskType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static String getDescription(TaskType type) {
        return type != null ? type.getDescription() : null;
    }
}
