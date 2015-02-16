package quickml.supervised.crossValidation.crossValLossFunctions;
import org.apache.mahout.classifier.evaluation.Auc;
import quickml.data.PredictionMap;
import quickml.supervised.alternative.crossvalidation.ClassifierLossFunction;
import quickml.supervised.alternative.crossvalidation.PredictionMapResult;
import quickml.supervised.alternative.crossvalidation.PredictionMapResults;

//TODO[mk] add Test
public class NonWeightedAUCCrossValLossFunction extends ClassifierLossFunction {

    @Override
    public Double getLoss(PredictionMapResults results) {
        Auc auc = new Auc();
        for (PredictionMapResult result : results) {
            int trueValue = (Double) result.getLabel() == 1.0 ? 1 : 0;
            PredictionMap classifierPrediction = result.getPrediction();
            double probabilityOfCorrectInstance = classifierPrediction.get(result.getLabel());
            double score = 0;
            score = (trueValue == 1) ? probabilityOfCorrectInstance: 1 - probabilityOfCorrectInstance;
            auc.add(trueValue, score);
        }
        return 1 - auc.auc();
    }

    @Override
    public String getName() {
        return "NonWeightedAUCCrossValLossFunction";
    }
}