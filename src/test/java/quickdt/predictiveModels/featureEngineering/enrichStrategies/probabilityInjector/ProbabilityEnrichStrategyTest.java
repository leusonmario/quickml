package quickdt.predictiveModels.featureEngineering.enrichStrategies.probabilityInjector;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;
import org.testng.Assert;
import org.testng.annotations.Test;
import quickdt.data.*;
import quickdt.predictiveModels.decisionTree.TreeBuilder;
import quickdt.predictiveModels.featureEngineering.AttributesEnricher;

import java.util.List;

public class ProbabilityEnrichStrategyTest {

    @Test
    public void testCreateAttributesEnricher() throws Exception {
        List<InstanceWithMapOfRegressors> trainingData = Lists.newLinkedList();
        trainingData.add(InstanceWithMapOfRegressors.create("true", "k1", 2, "k2", 1));
        trainingData.add(InstanceWithMapOfRegressors.create("true", "k1", 1, "k2", 2));
        trainingData.add(InstanceWithMapOfRegressors.create("false", "k1", 2, "k2", 2));
        trainingData.add(InstanceWithMapOfRegressors.create("false", "k1", 1, "k2", 2));
        ProbabilityEnrichStrategy probabilityEnrichStrategy = new ProbabilityEnrichStrategy(new TreeBuilder(), Sets.newHashSet("k1", "k2"), "true");
        final AttributesEnricher attributesEnricher = probabilityEnrichStrategy.build(trainingData);
        {
            Map<String, Serializable> inputAttributes = new HashMapAttributes();
            inputAttributes.put("k1", 1);
            inputAttributes.put("k2", 1);
            final Map<String, Serializable> outputAttributes = attributesEnricher.apply(inputAttributes);
            Assert.assertEquals(outputAttributes.get("k1-PROB"), 0.5);
            Assert.assertEquals(outputAttributes.get("k2-PROB"), 1.0);
        }
        {
            Map<String, Serializable> inputAttributes = new HashMapAttributes();
            inputAttributes.put("k1", 2);
            inputAttributes.put("k2", 2);
            final Map<String, Serializable> outputAttributes = attributesEnricher.apply(inputAttributes);
            Assert.assertEquals(outputAttributes.get("k1-PROB"), 0.5);
            Assert.assertEquals(outputAttributes.get("k2-PROB"), 1.0/3.0);
        }
    }
}