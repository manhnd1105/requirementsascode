package org.requirementsascode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class BuildModelTest extends AbstractTestCase{			
	@Before
	public void setup() {
		setupWith(new TestUseCaseModelRunner());
	}
	
	@Test
	public void createsNoUseCase() {
		UseCaseModel useCaseModel = useCaseModelBuilder.build();
		
		Collection<UseCase> useCases = useCaseModel.useCases();
		assertEquals(0, useCases.size());
	}

	@Test
	public void createsSingleUseCase() {
		useCaseModelBuilder.useCase(USE_CASE);
		UseCaseModel useCaseModel = useCaseModelBuilder.build();
		
		assertTrue(useCaseModel.hasUseCase(USE_CASE));

		Collection<UseCase> useCases = useCaseModel.useCases();
		assertEquals(1, useCases.size());
		assertEquals(USE_CASE, useCases.iterator().next().name());
	}

	@Test
	public void createsTwoUseCasesInOneGo() {
		useCaseModelBuilder.useCase(USE_CASE);
		useCaseModelBuilder.useCase(USE_CASE_2);
		UseCaseModel useCaseModel = useCaseModelBuilder.build();

		Collection<UseCase> useCases = useCaseModel.useCases();
		assertEquals(2, useCases.size());
	}
	
	@Test
	public void createsTwoUseCasesByBuildingOnExistingModel() {
		UseCaseModel useCaseModel = useCaseModelBuilder.useCase(USE_CASE).build();
		UseCaseModelBuilder.builderOf(useCaseModel).useCase(USE_CASE_2);

		Collection<UseCase> useCases = useCaseModel.useCases();
		assertEquals(2, useCases.size());
	}
	
	@Test
	public void implicitlyCreatesBasicFlow() {
		useCaseModelBuilder.useCase(USE_CASE);
		UseCaseModel useCaseModel = useCaseModelBuilder.build();

		UseCase useCase = useCaseModel.findUseCase(USE_CASE);
		Collection<Flow> flows = useCase.flows();
		
		assertEquals(1, flows.size());
		assertTrue(useCase.hasFlow(BASIC_FLOW));
		assertEquals(BASIC_FLOW, flows.iterator().next().name());
	}

	@Test
	public void createsNoSteps() {
		useCaseModelBuilder.useCase(USE_CASE);
		UseCaseModel useCaseModel = useCaseModelBuilder.build();

		UseCase useCase = useCaseModel.findUseCase(USE_CASE);
		Collection<Step> steps = useCase.steps();
		assertEquals(0, steps.size());
	}
	
	@Test
	public void createsSingleStepThatHandlesUserEvent() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		UseCaseModel useCaseModel =
			useCasePart
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
			.build();
		
		UseCase useCase = useCasePart.useCase();
		Collection<Step> steps = useCase.steps();
		assertEquals(1, steps.size());
		
		Step step = steps.iterator().next();
		assertEquals(CUSTOMER_ENTERS_TEXT, step.name());
		assertEquals(USE_CASE, step.useCase().name());	
		assertEquals(useCaseModel.userActor(), step.getActors()[0]);
	}
	
	@Test
	public void createsSingleStepThatHandlesSystemEvent() {		
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		UseCaseModel useCaseModel = 
			useCasePart
				.basicFlow()
					.step(SYSTEM_DISPLAYS_TEXT).handle(EnterText.class).system(displayEnteredText())
			.build();
		
		UseCase useCase = useCaseModel.findUseCase(USE_CASE);
		Collection<Step> steps = useCase.steps();
		assertEquals(1, steps.size());
		
		Step step = steps.iterator().next();
		assertEquals(SYSTEM_DISPLAYS_TEXT, step.name());
		assertEquals(USE_CASE, step.useCase().name());	
		assertEquals(useCaseModel.systemActor(), step.getActors()[0]);
	}
	
	@Test
	public void createsSingleStepThatPerformsSystemReactionAutomatically() {	
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		UseCaseModel useCaseModel = 
			useCasePart
				.basicFlow()
					.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText())
			.build();
						
		Collection<Step> steps = useCasePart.useCase().steps();
		assertEquals(1, steps.size());
		
		Step step = steps.iterator().next();
		assertEquals(SYSTEM_DISPLAYS_TEXT, step.name());
		assertEquals(USE_CASE, step.useCase().name());	
		assertEquals(useCaseModel.systemActor(), step.getActors()[0]);
	}
	
	@Test
	public void createsSingleStepThatPerformsSystemReactionAutomaticallyForSpecificActor() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);

		UseCaseModel useCaseModel = 
			useCasePart
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).as(customer).system(displayConstantText())
			.build();
				
		Collection<Step> steps = useCasePart.useCase().steps();
		assertEquals(1, steps.size());
		
		Step step = steps.iterator().next();
		assertEquals(CUSTOMER_ENTERS_TEXT, step.name());
		assertEquals(USE_CASE, step.useCase().name());
		assertTrue(useCaseModel.hasActor(customer.name()));
		assertEquals(customer, step.getActors()[0]);
	}
	
	@Test
	public void createsSingleActorWithSingleUseCase() {	
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		UseCaseModel useCaseModel = 
			useCasePart
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).as(customer).user(EnterText.class).system(displayEnteredText())
			.build();
		
		Actor customerActor = useCaseModel.findActor(CUSTOMER);
		Set<UseCase> useCases = customerActor.getUseCases();
		assertEquals(1, useCases.size());
		
		UseCase actualUseCase = useCases.iterator().next();
		assertEquals(USE_CASE, actualUseCase.name());
	}
	
	@Test
	public void createsSingleActorWithSingleUseCaseStep() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		UseCaseModel useCaseModel = 
			useCasePart
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).as(customer).user(EnterText.class).system(displayEnteredText())
			.build();

		Actor customerActor = useCaseModel.findActor(customer.name());
		List<Step> steps = customerActor.getStepsOf(useCasePart.useCase());
		Step step = steps.get(0);
		
		assertEquals(CUSTOMER_ENTERS_TEXT, step.name());
		assertEquals(CUSTOMER_ENTERS_TEXT, step.toString());
		assertEquals(USE_CASE, step.useCase().name());		
		assertEquals(customer, step.getActors()[0]);
	}
	
	@Test
	public void createsTwoActorsWithSingleUseCaseStep() {
		Actor anotherActor = useCaseModelBuilder.actor("Another Actor");
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
				
		UseCaseModel useCaseModel = 
			useCasePart
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).as(customer, anotherActor).user(EnterText.class).system(displayEnteredText())
			.build();

		Actor customerActor = useCaseModel.findActor(customer.name());		
		List<Step> steps = customerActor.getStepsOf(useCasePart.useCase());
		Step step = steps.get(0);
		
		assertEquals(CUSTOMER_ENTERS_TEXT, step.name());
		assertEquals(CUSTOMER_ENTERS_TEXT, step.toString());
		assertEquals(USE_CASE, step.useCase().name());		
		assertEquals(customer, step.getActors()[0]);
		
		steps = anotherActor.getStepsOf(useCasePart.useCase());
		step = steps.get(0);
		
		assertEquals(CUSTOMER_ENTERS_TEXT, step.name());
		assertEquals(anotherActor, step.getActors()[1]);
	}
	
	@Test
	public void createsSingleStepWithNoPreviousStep() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		useCasePart
			.basicFlow()
				.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText());

		Collection<Step> steps = useCasePart.useCase().steps();
		assertEquals(1, steps.size());
		
		Optional<Step> previousStep = steps.iterator().next().previousStepInFlow();
		
		assertFalse(previousStep.isPresent());
	}

	@Test
	public void createsTwoSteps() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);		
		
		UseCaseModel useCaseModel = 
			useCasePart
				.basicFlow()
					.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText()) 
					.step(SYSTEM_DISPLAYS_NUMBER)
						.as(customer).user(EnterNumber.class).system(displayEnteredNumber())
				.build();

		assertTrue(useCaseModel.actors().contains(useCaseModel.systemActor()));
		assertTrue(useCaseModel.actors().contains(customer));
		
		Collection<Step> steps = useCasePart.useCase().steps();
		assertEquals(2, steps.size());

		Iterator<Step> stepIt = steps.iterator();
		Step step = stepIt.next();
		assertEquals(SYSTEM_DISPLAYS_TEXT, step.name());
		assertEquals(USE_CASE, step.useCase().name());

		step = stepIt.next();
		assertEquals(SYSTEM_DISPLAYS_NUMBER, step.name());
		assertEquals(USE_CASE, step.useCase().name());
	}
	
	@Test
	public void createsTwoStepsAndCheckIfTheyExistInUseCaseByName() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);	
		
		useCasePart
			.basicFlow()
				.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText())
				.step(SYSTEM_DISPLAYS_TEXT_AGAIN).system(displayConstantText());
		
		Collection<Step> steps = useCasePart.useCase().steps();
		assertEquals(2, steps.size());

		boolean firstUseCaseStepExists = useCasePart.useCase().hasStep(SYSTEM_DISPLAYS_TEXT);
		boolean secondUseCaseStepExists = useCasePart.useCase().hasStep(SYSTEM_DISPLAYS_TEXT_AGAIN);
		
		assertTrue(firstUseCaseStepExists);
		assertTrue(secondUseCaseStepExists);
	}
	
	@Test
	public void createsTwoStepsInBasicFlowAndCheckIfTheyExistByIndex() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		useCasePart
			.basicFlow()
				.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText())
				.step(SYSTEM_DISPLAYS_TEXT_AGAIN).system(displayConstantText());
		
		List<Step> steps = useCasePart.useCase().basicFlow().steps();
		assertEquals(2, steps.size());
		
		assertEquals(SYSTEM_DISPLAYS_TEXT, steps.get(0).name());
		assertEquals(SYSTEM_DISPLAYS_TEXT_AGAIN, steps.get(1).name());
	}
	
	@Test
	public void createsOneStepInAlternatvieFlowAndCheckIfItExistsByIndex() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		useCasePart
			.basicFlow()
				.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText())
			.flow(ALTERNATIVE_FLOW)
				.step(SYSTEM_DISPLAYS_TEXT_AGAIN).system(displayConstantText());
			
		List<Step> steps = useCasePart.useCase().findFlow(ALTERNATIVE_FLOW).steps();
		assertEquals(1, steps.size());
		
		assertEquals(SYSTEM_DISPLAYS_TEXT_AGAIN, steps.get(0).name());
	}
	
	@Test
	public void createsTwoStepsAndPreviousStepOfSecondOneIsFirstOne() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		useCasePart
			.basicFlow()
				.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText())
				.step(SYSTEM_DISPLAYS_TEXT_AGAIN).system(displayConstantText());

		Step firstUseCaseStep = useCasePart.useCase().findStep(SYSTEM_DISPLAYS_TEXT);
		Step secondUseCaseStep = useCasePart.useCase().findStep(SYSTEM_DISPLAYS_TEXT_AGAIN);
		
		assertEquals(firstUseCaseStep, secondUseCaseStep.previousStepInFlow().get());
	}
	
	@Test
	public void createsTwoStepsInDifferentFlowsThatBothHaveNoPreviousSteps() { 
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		useCasePart
			.basicFlow()
				.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText())	
			.flow(ALTERNATIVE_FLOW)
				.step(SYSTEM_DISPLAYS_TEXT_AGAIN).system(displayConstantText());

		Step firstUseCaseStep = useCasePart.useCase().findStep(SYSTEM_DISPLAYS_TEXT);
		Step secondUseCaseStep = useCasePart.useCase().findStep(SYSTEM_DISPLAYS_TEXT_AGAIN);
		
		assertFalse(firstUseCaseStep.previousStepInFlow().isPresent());
		assertFalse(secondUseCaseStep.previousStepInFlow().isPresent());
	}
	
	@Test
	public void useCasesAreUniquelyIdentifiedByName() {	
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		UseCaseModel useCaseModel = 
			useCasePart
				.basicFlow()
					.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText())		
				.flow(ALTERNATIVE_FLOW)
					.step(SYSTEM_DISPLAYS_TEXT_AGAIN).system(displayConstantText())
				.build();
		
		assertEquals(1, useCaseModel.useCases().size());
		assertEquals(useCaseModel.findUseCase(USE_CASE), useCaseModel.useCases().iterator().next());
	}
	
	@Test
	public void flowsAreUniquelyIdentifiedByName() {		
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
				
		UseCase useCase = useCasePart.useCase();
		useCase.newFlow(ALTERNATIVE_FLOW);
		Flow existingFlow = useCase.findFlow(ALTERNATIVE_FLOW);
		
		assertEquals(2, useCase.flows().size()); // This is 2 because the basic flow always exists
		
		Iterator<Flow> flowIt = useCase.flows().iterator();
		assertEquals(useCase.basicFlow(), flowIt.next());
		assertEquals(existingFlow, flowIt.next());
	}
	
	@Test
	public void thereIsOnlyOneBasicFlowPerUseCase() {				
		useCaseModelBuilder.useCase(USE_CASE);
		
		UseCaseModel useCaseModel = useCaseModelBuilder.build();
		UseCase uc = useCaseModel.findUseCase(USE_CASE);
		assertEquals(1, uc.flows().size());
	}
	
	@Test
	public void actorsCanBeReusedInUseCase() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		UseCaseModel useCaseModel = 
			useCasePart
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.as(customer).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN)
						.as(customer).user(EnterText.class).system(displayEnteredText())
			.build();
		
		Collection<Step> steps = useCaseModel.steps();
		assertEquals(2, steps.size());
		
		Iterator<Step> stepsIt = steps.iterator();
		Actor actor1 = stepsIt.next().getActors()[0];
		Actor actor2 = stepsIt.next().getActors()[0];
		
		assertTrue(actor1 == actor2);
		assertEquals(customer, actor1);
	}
	
	@Test
	public void actorsCanBeReusedBetweenUseCases() {
		UseCasePart useCasePart = useCaseModelBuilder.useCase(USE_CASE);
		
		UseCaseModel useCaseModel = 
			useCasePart
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.as(customer).user(EnterText.class).system(displayEnteredText())
			.useCase(USE_CASE_2)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT_AGAIN)
						.as(customer).user(EnterText.class).system(displayEnteredText())
			.build();
				
		Collection<Step> steps = useCaseModel.steps();
		assertEquals(2, steps.size());
		
		Iterator<Step> stepsIt = steps.iterator();
		Actor actor1 = stepsIt.next().getActors()[0];
		Actor actor2 = stepsIt.next().getActors()[0];
		
		assertTrue(actor1 == actor2);
		assertEquals(customer, actor1);
	}
}
