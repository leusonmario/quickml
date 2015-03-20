package quickml.supervised.classifier;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.mutable.MutableInt;
import quickml.data.InstanceWithAttributesMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by alexanderhawk on 3/18/15.
 */
public class ClassificationProperties {

    final ImmutableMap<Serializable, Long> classificationsAndCounts;
    protected ClassificationProperties(HashMap<Serializable, Long> classificationsAndCounts) {
        this.classificationsAndCounts = new ImmutableMap.Builder<Serializable,Long>()
                                             .putAll(classificationsAndCounts)
                                             .build();
    }

    public static <T extends InstanceWithAttributesMap> ClassificationProperties getClassificationProperties(Iterable<T> trainingData) {
        HashMap<Serializable, Long> classificationsAndCounts = getClassificationsAndCounts(trainingData);
        if (classificationsAndCounts.keySet().size()>2) {
            return new ClassificationProperties(classificationsAndCounts);
        }
        return BinaryClassificationProperties.createClassificationPropertiesOfBinaryData(classificationsAndCounts);
    }

    private static <T extends InstanceWithAttributesMap> HashMap<Serializable, Long> getClassificationsAndCounts(Iterable<T> trainingData) {
        HashMap<Serializable, Long> classificationsAndCounts = Maps.newHashMap();
        for (T instance : trainingData) {
            Serializable classification = instance.getLabel();

            if (classificationsAndCounts.containsKey(classification)) {
                classificationsAndCounts.put(classification, classificationsAndCounts.get(classification) + 1L);

            } else
                classificationsAndCounts.put(classification, 1L);

        }
        return classificationsAndCounts;
    }

    public Set<Serializable> getClassifications() {
        return Sets.newHashSet(classificationsAndCounts.keySet());
    }

    public boolean classificationsAreBinary(){
        return classificationsAndCounts.size() == 2;
    }

    public ImmutableMap<Serializable, Long> getClassificationsAndCounts() {
        return classificationsAndCounts;
    }

}