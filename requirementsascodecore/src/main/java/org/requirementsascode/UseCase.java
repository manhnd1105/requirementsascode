package org.requirementsascode;

import static org.requirementsascode.ModelElementContainer.findModelElement;
import static org.requirementsascode.ModelElementContainer.getModelElements;
import static org.requirementsascode.ModelElementContainer.hasModelElement;
import static org.requirementsascode.ModelElementContainer.saveModelElement;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.requirementsascode.exception.ElementAlreadyInModel;
import org.requirementsascode.exception.NoSuchElementInModel;
import org.requirementsascode.predicate.After;

/**
 * A use case, as part of a use case model.
 *
 * <p>As an example, a use case for an ATM is "Get cash". As another example, a use case for an
 * online flight reservation system is "Book flight".
 *
 * <p>The use case itself defines no behavior. The use case steps that are part of the use case
 * define the behavior of the use case. As steps are often performed one after the other, in
 * sequence, they are grouped in use case flows.
 *
 * @author b_muth
 */
public class UseCase extends UseCaseModelElement {
  private Map<String, Flow> nameToFlowMap;
  private Map<String, Step> nameToStepMap;
  private Flow basicFlow;

  /**
   * Creates a use case with the specified name that belongs to the specified use case model.
   *
   * @param useCaseName the name of the use case to be created
   * @param useCaseModel the use case model that will contain the new use case
   */
  UseCase(String useCaseName, UseCaseModel useCaseModel) {
    super(useCaseName, useCaseModel);
    this.nameToFlowMap = new LinkedHashMap<>();
    this.nameToStepMap = new LinkedHashMap<>();
    this.basicFlow = newFlow("basic flow");
  }

  /**
   * The basic flow defines the 'happy day scenario' of the use case: no exceptions are handled in
   * it, all steps are assumed to go well.
   *
   * <p>The basic flow is a sequence of use case steps that lead the user to the user's goal. There
   * is exactly one basic flow per use case.
   *
   * @return the basic flow of the use case
   */
  public Flow getBasicFlow() {
    return basicFlow;
  }

  /**
   * Checks whether this use case contains the specified flow.
   *
   * @param flowName the name of the flow whose existence to check
   * @return true if this use case contains the specified flow, false otherwise
   */
  public boolean hasFlow(String flowName) {
    boolean hasFlow = hasModelElement(flowName, nameToFlowMap);
    return hasFlow;
  }

  /**
   * Checks whether this use case contains the specified step.
   *
   * @param stepName the name of the step whose existence to check
   * @return true if this use case contains the specified step, false otherwise
   */
  public boolean hasStep(String stepName) {
    boolean hasStep = hasModelElement(stepName, nameToStepMap);
    return hasStep;
  }

  /**
   * Creates a new flow in this use case.
   *
   * @param flowName the name of the flow to be created.
   * @return the newly created flow
   * @throws ElementAlreadyInModel if a flow with the specified name already exists in the use case
   */
  Flow newFlow(String flowName) {
    Flow flow = new Flow(flowName, this);
    saveModelElement(flow, nameToFlowMap);
    return flow;
  }

  /**
   * Creates a new step with the specified parameters.
   *
   * @param stepName the name of the step
   * @param flow the flow the step shall belong to
   * @param previousStep the previous step in the flow, if there is one
   * @param predicate the complete predicate of the step, or else the default predicate is: after
   *     previous step, unless interrupted by other step (e.g "insteadOf").
   * @return the newly created step
   */
  Step newStep(
      String stepName,
      Flow flow,
      Optional<Step> previousStep,
      Optional<Predicate<UseCaseModelRunner>> predicate) {
	  
    Step step = new Step(stepName, flow, previousStep);
    step.setPredicate(predicate.orElse(afterPreviousStepUnlessOtherStepCouldReact(step)));
    saveModelElement(step, nameToStepMap);
    return step;
  }

  private Predicate<UseCaseModelRunner> afterPreviousStepUnlessOtherStepCouldReact(
      Step currentStep) {
	  
    Optional<Step> previousStepInFlow = currentStep.getPreviousStepInFlow();
    Predicate<UseCaseModelRunner> afterPreviousStep = new After(previousStepInFlow);
    return afterPreviousStep.and(noOtherStepCouldReactThan(currentStep));
  }

  private Predicate<UseCaseModelRunner> noOtherStepCouldReactThan(Step theStep) {
    return useCaseModelRunner -> {
      Class<?> theStepsEventClass = theStep.getUserEventClass();
      UseCaseModel useCaseModel = theStep.getUseCaseModel();

      Stream<Step> otherStepsStream =
          useCaseModel.getModifiableSteps().stream().filter(step -> !step.equals(theStep));

      Set<Step> otherStepsThatCouldReact =
          useCaseModelRunner.stepsInStreamThatCanReactTo(theStepsEventClass, otherStepsStream);
      return otherStepsThatCouldReact.size() == 0;
    };
  }

  /**
   * Finds the flow with the specified name, contained in this use case.
   *
   * @param flowName the name of the flow to look for
   * @return the flow if found, or else an empty optional
   * @throws NoSuchElementInModel if no flow with the specified flowName is found in the current use
   *     case
   */
  public Flow findFlow(String flowName) {
    Flow flow = findModelElement(flowName, nameToFlowMap);
    return flow;
  }

  /**
   * Finds the step with the specified name, contained in this use case.
   *
   * @param stepName the name of the step to look for
   * @return the step if found, or else an empty optional
   * @throws NoSuchElementInModel if no step with the specified stepName is found in the current use
   *     case
   */
  public Step findStep(String stepName) {
    Step step = findModelElement(stepName, nameToStepMap);
    return step;
  }

  /**
   * Returns the flows contained in this use case.
   *
   * @return a collection of the flows
   */
  public Collection<Flow> getFlows() {
    Collection<Flow> modifiableFlows = getModelElements(nameToFlowMap);
    return Collections.unmodifiableCollection(modifiableFlows);
  }

  /**
   * Returns the steps contained in this use case.
   *
   * @return a collection of the steps
   */
  public Collection<Step> getSteps() {
    Collection<Step> modifiableSteps = getModifiableSteps();
    return Collections.unmodifiableCollection(modifiableSteps);
  }

  Collection<Step> getModifiableSteps() {
    return getModelElements(nameToStepMap);
  }
}
