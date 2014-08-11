package quickdt.crossValidation;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickdt.crossValidation.crossValLossFunctions.LabelPredictionWeight;
import quickdt.crossValidation.dateTimeExtractors.DateTimeExtractor;
import quickdt.data.AbstractInstance;
import quickdt.predictiveModels.PredictiveModel;
import quickdt.predictiveModels.PredictiveModelBuilder;
import quickdt.crossValidation.crossValLossFunctions.CrossValLossFunction;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by alexanderhawk on 5/5/14.
 */
public class OutOfTimeCrossValidator<R, P> extends CrossValidator<R, P>{

    private static final Logger logger = LoggerFactory.getLogger(OutOfTimeCrossValidator.class);

    List<AbstractInstance<R>> allTrainingData;
    List<AbstractInstance<R>> trainingDataToAddToPredictiveModel;
    List<AbstractInstance<R>> validationSet;

    final private CrossValLossFunction<P> crossValLossFunction;
    private double fractionOfDataForCrossValidation = 0.25;

    private final DateTimeExtractor dateTimeExtractor;
    final Period durationOfValidationSet;
    private DateTime maxTime;
    private double weightOfValidationSet;
    private int currentTrainingSetSize = 0;

    public OutOfTimeCrossValidator(CrossValLossFunction<P> crossValLossFunction, double fractionOfDataForCrossValidation, int validationTimeSliceHours, DateTimeExtractor dateTimeExtractor) {
        this.crossValLossFunction = crossValLossFunction;
        this.fractionOfDataForCrossValidation = fractionOfDataForCrossValidation;
        this.dateTimeExtractor = dateTimeExtractor;
        this.durationOfValidationSet = new Period(validationTimeSliceHours, 0, 0, 0);
    }

    @Override
    public <PM extends PredictiveModel<R, P>> double getCrossValidatedLoss(PredictiveModelBuilder<PM> predictiveModelBuilder, Iterable<? extends AbstractInstance<R>> rawTrainingData) {

        initializeTrainingAndValidationSets(rawTrainingData);

        double runningLoss = 0;
        double runningWeightOfValidationSet = 0;
        while (!validationSet.isEmpty()) {
            PM predictiveModel = predictiveModelBuilder.buildPredictiveModel(trainingDataToAddToPredictiveModel);
            List<LabelPredictionWeight<P>> labelPredictionWeights = predictiveModel.createLabelPredictionWeights(validationSet);
            runningLoss += crossValLossFunction.getLoss(labelPredictionWeights) * weightOfValidationSet;
            runningWeightOfValidationSet += weightOfValidationSet;
            logger.debug("Running average Loss: " + runningLoss / runningWeightOfValidationSet + ", running weight: " + runningWeightOfValidationSet);
            updateTrainingSet();
            updateCrossValidationSet();
        }
        final double averageLoss = runningLoss / runningWeightOfValidationSet;
        logger.info("Average loss: " + averageLoss + ", runningWeight: " + runningWeightOfValidationSet);
        return averageLoss;
    }

    private void initializeTrainingAndValidationSets(Iterable<? extends AbstractInstance<R>> rawTrainingData) {
        setAndSortAllTrainingData(rawTrainingData);
        setMaxValidationTime();

        int initialTrainingSetSize = getInitialSizeForTrainData();
        trainingDataToAddToPredictiveModel = Lists.<AbstractInstance<R>>newArrayListWithExpectedSize(initialTrainingSetSize);
        validationSet = Lists.<AbstractInstance<R>>newArrayList();

        DateTime timeOfInstance;
        DateTime timeOfFirstInstanceInValidationSet = dateTimeExtractor.extractDateTime(allTrainingData.get(initialTrainingSetSize));
        DateTime leastUpperBoundOfValidationSet = timeOfFirstInstanceInValidationSet.plus(durationOfValidationSet);

        weightOfValidationSet = 0;
        for (AbstractInstance<R> instance : allTrainingData) {
            timeOfInstance = dateTimeExtractor.extractDateTime(instance);
            if (timeOfInstance.isBefore(timeOfFirstInstanceInValidationSet)) {
                trainingDataToAddToPredictiveModel.add(instance);
            } else if (timeOfInstance.isBefore(leastUpperBoundOfValidationSet)) {
                validationSet.add(instance);
                weightOfValidationSet += instance.getWeight();
            } else {
                break;
            }
        }
        currentTrainingSetSize = trainingDataToAddToPredictiveModel.size();
    }

    private void updateTrainingSet() {
        trainingDataToAddToPredictiveModel = validationSet;
        currentTrainingSetSize += trainingDataToAddToPredictiveModel.size();
    }

    private void updateCrossValidationSet() {
        clearValidationSet();
        if (!newValidationSetExists()) {
            return;
        }
        DateTime timeOfFirstInstanceInValidationSet = dateTimeExtractor.extractDateTime(allTrainingData.get(currentTrainingSetSize));
        DateTime leastOuterBoundOfValidationSet = timeOfFirstInstanceInValidationSet.plus(durationOfValidationSet);

        while(validationSet.isEmpty()) {
            for (int i = currentTrainingSetSize; i < allTrainingData.size(); i++) {
                AbstractInstance<R> instance = allTrainingData.get(i);
                DateTime timeOfInstance = dateTimeExtractor.extractDateTime(instance);
                if (timeOfInstance.isBefore(leastOuterBoundOfValidationSet)) {
                    validationSet.add(instance);
                    weightOfValidationSet += instance.getWeight();
                } else
                    break;
            }
            leastOuterBoundOfValidationSet = leastOuterBoundOfValidationSet.plus(durationOfValidationSet);
        }
    }

    private void clearValidationSet() {
        weightOfValidationSet = 0;
        validationSet = Lists.<AbstractInstance<R>>newArrayList();
    }

    private void setMaxValidationTime() {
        AbstractInstance<R> latestInstance = allTrainingData.get(allTrainingData.size() - 1);
        maxTime = dateTimeExtractor.extractDateTime(latestInstance);
    }

    private int getInitialSizeForTrainData() {
        int initialTrainingSetSize = (int) (allTrainingData.size() * (1 - fractionOfDataForCrossValidation));
        verifyInitialValidationSetExists(initialTrainingSetSize);
        return initialTrainingSetSize;
    }

    private void verifyInitialValidationSetExists(int initialTrainingSetSize) {
        if (initialTrainingSetSize == allTrainingData.size()) {
            throw new RuntimeException("fractionOfDataForCrossValidation must be non zero");
        }
    }

    private boolean newValidationSetExists() {
        return currentTrainingSetSize < allTrainingData.size();
    }

    private void setAndSortAllTrainingData(Iterable<? extends AbstractInstance<R>> rawTrainingData) {
        this.allTrainingData = Lists.<AbstractInstance<R>>newArrayList();
        for (AbstractInstance<R> instance : rawTrainingData) {
            this.allTrainingData.add(instance);
        }

        Comparator<AbstractInstance<R>> comparator = new Comparator<AbstractInstance<R>>() {
            @Override
            public int compare(AbstractInstance<R> o1, AbstractInstance<R> o2) {
                DateTime firstInstance = dateTimeExtractor.extractDateTime(o1);
                DateTime secondInstance = dateTimeExtractor.extractDateTime(o2);
                if (firstInstance.isAfter(secondInstance)) {
                    return 1;
                } else if (firstInstance.isEqual(secondInstance)) {
                    return 0;
                } else {
                    return -1;
                }
            }
        };

        Collections.sort(this.allTrainingData, comparator);
    }
}