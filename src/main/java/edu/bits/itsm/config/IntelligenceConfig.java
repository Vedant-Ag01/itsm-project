package edu.bits.itsm.config;

import edu.bits.itsm.classification.CategoryClassifier;
import edu.bits.itsm.classification.KeywordCategoryClassifier;
import edu.bits.itsm.sla.HeuristicSlaRiskPredictor;
import edu.bits.itsm.sla.SlaCalculator;
import edu.bits.itsm.sla.SlaRiskPredictor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The "smart" parts: category suggestion and SLA risk prediction
@Configuration
public class IntelligenceConfig {

    @Bean
    public CategoryClassifier categoryClassifier() {
        return new KeywordCategoryClassifier(KeywordCategoryClassifier.defaultRules());
    }

    @Bean
    public SlaRiskPredictor slaRiskPredictor(SlaCalculator calculator) {
        return new HeuristicSlaRiskPredictor(calculator);
    }
}
