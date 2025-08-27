package org.linghu.mybackend.constants;

/**
 * 实验相关常量和枚举类管理
 */
public class ExperimentConstants {
    
    /**
     * 任务类型枚举
     */
    public enum TaskType {
        CHOICE("选择题"),  // 选择题
        FILL("填空题"),    // 填空题
        QA("问答题"),      // 问答题
        CODING("编程题");  // 编程题
        
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
    
    /**
     * 问题类型枚举（适用于选择题）
     */
    public enum QuestionType {
        SINGLE("单选题"),     // 单选题
        MULTIPLE("多选题");   // 多选题
        
        private final String description;
        
        QuestionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static String getDescription(QuestionType type) {
            return type != null ? type.getDescription() : null;
        }
    }
    
    /**
     * 资源类型枚举
     */
    public enum ResourceType {
        DOCUMENT("文档"),   // 文档
        IMAGE("图片"),      // 图片
        VIDEO("视频"),      // 视频
        CODE("代码"),       // 代码
        OTHER("其他");      // 其他
        
        private final String description;
        
        ResourceType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
          public static String getDescription(ResourceType type) {
            return type != null ? type.getDescription() : null;
        }
    }
    
    /**
     * 实验任务相关常量
     */
    public static class TaskConstants {
        public static final int DEFAULT_MAX_SCORE = 100;
        public static final boolean DEFAULT_REQUIRED = true;
        public static final boolean DEFAULT_AUTO_GRADABLE = false;
        public static final boolean DEFAULT_CASE_SENSITIVE = false;
    }
    
    /**
     * 实验状态常量
     */
    public static class ExperimentStatus {
        public static final String DRAFT = "DRAFT";
        public static final String PUBLISHED = "PUBLISHED";
    }
    
    private ExperimentConstants() {
        // 私有构造函数，防止实例化
    }
}
