package org.requirementsascode.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.requirementsascode.testutil.Names.ALTERNATIVE_FLOW;
import static org.requirementsascode.testutil.Names.ALTERNATIVE_FLOW_2;
import static org.requirementsascode.testutil.Names.CONTINUE;
import static org.requirementsascode.testutil.Names.CONTINUE_2;
import static org.requirementsascode.testutil.Names.CUSTOMER_ENTERS_ALTERNATIVE_TEXT;
import static org.requirementsascode.testutil.Names.CUSTOMER_ENTERS_NUMBER;
import static org.requirementsascode.testutil.Names.CUSTOMER_ENTERS_NUMBER_AGAIN;
import static org.requirementsascode.testutil.Names.CUSTOMER_ENTERS_TEXT;
import static org.requirementsascode.testutil.Names.CUSTOMER_ENTERS_TEXT_AGAIN;
import static org.requirementsascode.testutil.Names.SYSTEM_DISPLAYS_TEXT;
import static org.requirementsascode.testutil.Names.SYSTEM_DISPLAYS_TEXT_AGAIN;
import static org.requirementsascode.testutil.Names.THIS_STEP_SHOULD_BE_SKIPPED;
import static org.requirementsascode.testutil.Names.THIS_STEP_SHOULD_BE_SKIPPED_AS_WELL;
import static org.requirementsascode.testutil.Names.USE_CASE;
import static org.requirementsascode.testutil.Names.USE_CASE_2;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.requirementsascode.Actor;
import org.requirementsascode.TestUseCaseRunner;
import org.requirementsascode.UseCaseModel;
import org.requirementsascode.UseCaseStep;
import org.requirementsascode.testutil.EnterNumber;
import org.requirementsascode.testutil.EnterText;

public class SystemReactionTest extends AbstractTestCase{
	private Actor rightActor;
	private Actor secondActor;
	private Actor actorWithDisabledStep;
	
	private int timesDisplayed;
		
	@Before
	public void setup() {
		setupWith(new TestUseCaseRunner());
		this.rightActor = useCaseModelBuilder.actor("Right Actor");
		this.secondActor = useCaseModelBuilder.actor("Second Actor");
		this.actorWithDisabledStep = useCaseModelBuilder.actor("Actor With Disabled Step");
	}
	
	@Test
	public void useCaseRunnerIsNotRunningAtFirst(){
		assertFalse(useCaseRunner.isRunning());
	}
	
	@Test
	public void useCaseRunnerIsRunningAfterRunCall(){
		useCaseRunner.run();
		assertTrue(useCaseRunner.isRunning());
	}
	
	@Test
	public void useCaseRunnerIsNotRunningWhenBeingStoppedBeforeRunCall(){
		useCaseRunner.stop();
		assertFalse(useCaseRunner.isRunning());
	}
	
	@Test
	public void useCaseRunnerIsNotRunningWhenBeingStoppedAfterRunCall(){
		useCaseRunner.run();
		useCaseRunner.stop();
		assertFalse(useCaseRunner.isRunning());
	}
	
	@Test
	public void printsTextAutonomously() {
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText())
			.build();
		
		useCaseRunner.run(useCaseModel);
		
		assertEquals(SYSTEM_DISPLAYS_TEXT + ";", runStepNames());
	}
	
	@Test
	public void printsTextAutonomouslyOnlyIfActorIsRight() {
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(SYSTEM_DISPLAYS_TEXT).as(customer).system(displayConstantText())
					.step(SYSTEM_DISPLAYS_TEXT_AGAIN).as(secondActor).system(displayConstantText())
			.build();
		
		useCaseRunner.as(customer).run(useCaseModel);
		
		assertEquals(SYSTEM_DISPLAYS_TEXT +";", runStepNames());
	}
	
	@Test
	public void printsTextAutonomouslyTwice() {
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText())
					.step(SYSTEM_DISPLAYS_TEXT_AGAIN).system(displayConstantText())
			.build();
		
		useCaseRunner.run(useCaseModel);
		
		assertEquals(SYSTEM_DISPLAYS_TEXT +";" + SYSTEM_DISPLAYS_TEXT_AGAIN + ";", runStepNames());
	}
	
	@Test
	public void doesNotReactToAlreadyRunStep() { 	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";", runStepNames());
	}
	
	@Test
	public void oneStepCannotReactIfEventIsWrong() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
			.build();
				
		useCaseRunner.run(useCaseModel);
		
		boolean canReact = useCaseRunner.canReactTo(enterNumber().getClass());
		assertFalse(canReact);
	}
	
	@Test
	public void oneStepCanReactIfEventIsRight() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
			.build();
				
		useCaseRunner.run(useCaseModel);
		
		boolean canReact = useCaseRunner.canReactTo(enterText().getClass());
		assertTrue(canReact);
		
		Set<UseCaseStep> stepsThatCanReact = useCaseRunner.stepsThatCanReactTo(enterText().getClass());
		assertEquals(1, stepsThatCanReact.size());
		assertEquals(CUSTOMER_ENTERS_TEXT, stepsThatCanReact.iterator().next().name().toString());
	}
	
	@Test
	public void moreThanOneStepCanReact() { 
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow().when(run -> true)
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
				.flow("Alternative Flow: Could react as well").when(run -> true)
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
			.build();
		
		useCaseRunner.run(useCaseModel);
		
		boolean canReact = useCaseRunner.canReactTo(enterText().getClass());
		assertTrue(canReact);
		
		Set<UseCaseStep> stepsThatCanReact = useCaseRunner.stepsThatCanReactTo(enterText().getClass());
		assertEquals(2, stepsThatCanReact.size());
	}
	
	@Test
	public void oneStepReacts() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
				.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
			.build();
				
		useCaseRunner.run(useCaseModel);
		Optional<UseCaseStep> latestStepRun = useCaseRunner.reactTo(enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT, latestStepRun.get().name());
	}
	
	@Test
	public void twoSequentialStepsReactToEventsOfSameType() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
				.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
				.step(CUSTOMER_ENTERS_TEXT_AGAIN).user(EnterText.class).system(displayEnteredText())
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT +";" + CUSTOMER_ENTERS_TEXT_AGAIN +";", runStepNames());
	}
	
	@Test
	public void twoSequentialStepsReactToEventsOfDifferentType() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT +";" + CUSTOMER_ENTERS_NUMBER +";", runStepNames());
	}
	
	@Test
	public void twoSequentialStepsReactOnlyWhenActorIsRight() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)			
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.as(rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN)
						.as(secondActor).user(EnterText.class).system(displayEnteredText())
			.build();
		
		useCaseRunner.as(rightActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT +";", runStepNames());
	}
	
	@Test
	public void twoSequentialStepsReactWhenOneOfTheActorsIsRight() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)			
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.as(rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN)
						.as(secondActor).user(EnterText.class).system(displayEnteredText())
			.build();
		
		useCaseRunner.as(rightActor, secondActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT +";" + CUSTOMER_ENTERS_TEXT_AGAIN +";", runStepNames());
	}
	
	@Test
	public void twoSequentialStepsReactWhenSeveralActorsContainRightActorAtFirstPosition() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.as(rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN)
						.as(rightActor, secondActor).user(EnterText.class).system(displayEnteredText())
			.build();
		
		useCaseRunner.as(rightActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT +";" + CUSTOMER_ENTERS_TEXT_AGAIN +";", runStepNames());
	}
	
	@Test
	public void twoSequentialStepsReactWhenSeveralActorsContainRightActorAtSecondPosition() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.as(rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN)
						.as(secondActor, rightActor).user(EnterText.class).system(displayEnteredText())
				.build();
		
		useCaseRunner.as(rightActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT +";" + CUSTOMER_ENTERS_TEXT_AGAIN +";", runStepNames());
	}
	
	@Test
	public void twoSequentialStepsReactWhenRunningWithDifferentActors() { 		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.as(customer).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER)
						.as(secondActor).user(EnterNumber.class).system(displayEnteredNumber())
				.build();
		
		useCaseRunner.as(customer).run(useCaseModel);
		useCaseRunner.reactTo(enterText());
		
		useCaseRunner.as(secondActor).run(useCaseModel);
		useCaseRunner.reactTo(enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT +";" + CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void onlyStepWithTruePredicateReacts() { 	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()		
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())			
				.flow("Alternative Flow: Skipped").when(r -> false)
					.step(THIS_STEP_SHOULD_BE_SKIPPED).user(EnterText.class).system(throwRuntimeException())
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT +";", runStepNames());
	}
	
	@Test
	public void stepThasHasTruePredicateReactsEvenIfOtherStepWouldBePerformedBySystem() { 		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()	
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())			
				.flow("Alternative Flow: Skipped").when(r -> false)
					.step(THIS_STEP_SHOULD_BE_SKIPPED).system(r -> {System.out.println("You should not see this!");})
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";", runStepNames());
	}
	
	@Test
	public void onlyStepWithRightActorReacts() { 		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()	
					.step(CUSTOMER_ENTERS_TEXT)
						.as(rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN)
						.as(secondActor).user(EnterText.class).system(throwRuntimeException())
			.build();
		
		useCaseRunner.as(rightActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";", runStepNames());
	}
	
	@Test
	public void onlyStepWithRightActorInDifferentFlowReacts() { 
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()	
					.step(CUSTOMER_ENTERS_TEXT)
						.as(secondActor).user(EnterText.class).system(throwRuntimeException())			
			.flow(ALTERNATIVE_FLOW).when(textIsNotAvailable())
				.step(CUSTOMER_ENTERS_TEXT_AGAIN)
					.as(rightActor).user(EnterText.class).system(displayEnteredText())
			.build();
		
		useCaseRunner.as(rightActor).run(useCaseModel);
		Optional<UseCaseStep> lastStepRun = useCaseRunner.reactTo(enterText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT_AGAIN, lastStepRun.get().name());
	}
	
	@Test
	public void stepWithWrongActorInDifferentFlowDoesNotReact() { 
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()	
					.step(CUSTOMER_ENTERS_TEXT)
						.as(rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT)
						.as(rightActor).user(EnterText.class).system(displayEnteredText())			
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_ALTERNATIVE_TEXT)
					.step(THIS_STEP_SHOULD_BE_SKIPPED)
						.as(secondActor).user(EnterText.class).system(throwRuntimeException())
			.build();
		
		useCaseRunner.as(rightActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		assertEquals(CUSTOMER_ENTERS_ALTERNATIVE_TEXT, latestStepName());
	}
	
	@Test
	public void doesNotReactIfStepHasRightActorButFalsePredicate() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.as(customer).user(EnterText.class).system(displayEnteredText())				
				.flow(ALTERNATIVE_FLOW).when(r -> false)
					.step(THIS_STEP_SHOULD_BE_SKIPPED)
						.as(actorWithDisabledStep).user(EnterText.class).system(displayEnteredText())
			.build();
				
		useCaseRunner.as(actorWithDisabledStep).run(useCaseModel);
		Optional<UseCaseStep> lastStepRun = useCaseRunner.reactTo(enterText());
		
		assertFalse(lastStepRun.isPresent());
	}
	
	@Test
	public void reactsToFirstStepAlternativeWhen() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()					
					.step(THIS_STEP_SHOULD_BE_SKIPPED).user(EnterText.class).system(throwRuntimeException())
					.step(THIS_STEP_SHOULD_BE_SKIPPED_AS_WELL).user(EnterText.class).system(throwRuntimeException())		
			.flow(ALTERNATIVE_FLOW).when(textIsNotAvailable())
				.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
		.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText());
		
		assertEquals(CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";", runStepNames());
	}
	
	@Test
	public void reactsToSecondStepAlternativeWhen() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(THIS_STEP_SHOULD_BE_SKIPPED).user(EnterText.class).system(throwRuntimeException())		
				.flow(ALTERNATIVE_FLOW).when(r -> CUSTOMER_ENTERS_TEXT.equals(latestStepName()))
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterAlternativeText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";", runStepNames());
	}
	
	@Test
	public void doesNotReenterAlternativeFlowEvenIfItHasTruePredicate() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()
					.step(THIS_STEP_SHOULD_BE_SKIPPED).user(EnterText.class).system(throwRuntimeException())
					.step(THIS_STEP_SHOULD_BE_SKIPPED_AS_WELL).user(EnterText.class).system(throwRuntimeException())		
				.flow(ALTERNATIVE_FLOW).when(r -> true)
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner
			.reactTo(enterAlternativeText(), enterNumber(), enterAlternativeText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";" + CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	 
	@Test
	public void reactsToAlternativeAfterFirstStep() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)		
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(THIS_STEP_SHOULD_BE_SKIPPED).user(EnterText.class).system(throwRuntimeException())		
				.flow(ALTERNATIVE_FLOW).insteadOf(THIS_STEP_SHOULD_BE_SKIPPED)
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterAlternativeText());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";", runStepNames());
	}
	
	@Test
	public void reactsToAlternativeAtFirstStep() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(THIS_STEP_SHOULD_BE_SKIPPED).user(EnterText.class).system(throwRuntimeException())
					.step(THIS_STEP_SHOULD_BE_SKIPPED_AS_WELL).user(EnterText.class).system(throwRuntimeException())		
				.flow(ALTERNATIVE_FLOW).insteadOf(THIS_STEP_SHOULD_BE_SKIPPED)
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
			.build();
		
		useCaseRunner.run(useCaseModel);
		Optional<UseCaseStep> latestStep = useCaseRunner.reactTo(enterText());
		
		assertEquals(CUSTOMER_ENTERS_ALTERNATIVE_TEXT, latestStep.get().name());
		assertEquals(CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";", runStepNames());
	}
	
	@Test
	public void startsOneOfTwoParallelUseCasesByDifferentEvent() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
			.useCase(USE_CASE_2)
				.basicFlow()
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void startsTwoUseCasesSequentially() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
			.useCase(USE_CASE_2)
				.basicFlow().when(textIsAvailable())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT +";" + CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void doesNotReactWhileConditionNotFulfilled() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)			
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.user(EnterText.class).system(displayEnteredText())
							.reactWhile(r -> false)
					.step(CUSTOMER_ENTERS_NUMBER)
						.user(EnterNumber.class).system(displayEnteredNumber())
			.build();

		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterNumber());
		
		assertEquals("", runStepNames());
	}
	
	@Test
	public void interruptsReactWhileBefore() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)			
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN)
						.user(EnterText.class).system(displayEnteredText())
							.reactWhile(r -> true)
					.step(CUSTOMER_ENTERS_NUMBER)
						.user(EnterNumber.class).system(displayEnteredNumber())
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN)
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT)
						.user(EnterText.class).system(displayEnteredText())
			.build();
				
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";", runStepNames());
	}
	
	@Test
	public void interruptsReactWhileAfter() {
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN)
						.user(EnterText.class).system(displayEnteredText())
							.reactWhile(r -> true)
					.step(CUSTOMER_ENTERS_NUMBER)
						.user(EnterNumber.class).system(displayEnteredNumber())
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_NUMBER)
					.step(CUSTOMER_ENTERS_NUMBER_AGAIN)
						.user(EnterNumber.class).system(displayEnteredNumber())
			.build();
				
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_TEXT_AGAIN + ";" +
			CUSTOMER_ENTERS_NUMBER_AGAIN +";", runStepNames());
	}
	
	@Test
	public void reactToStepOnce() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.user(EnterText.class).system(displayEnteredText())
							.reactWhile(r -> true)
					.step(CUSTOMER_ENTERS_NUMBER)
						.user(EnterNumber.class).system(displayEnteredNumber())
			.build();
				
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void reactToStepTwice() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.user(EnterText.class).system(displayEnteredText())
							.reactWhile(r -> true)
					.step(CUSTOMER_ENTERS_NUMBER)
						.user(EnterNumber.class).system(displayEnteredNumber())
			.build();
				
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_TEXT + ";" +
			CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void reactToStepThreeTimes() {		
		timesDisplayed = 0;
		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT)
						.user(EnterText.class).system(displayEnteredTextAndIncrementCounter())
							.reactWhile(r -> timesDisplayed < 3)
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())
			.build();
				
		// Create way to many events to see if the repeat stops after three events
		useCaseRunner.run(useCaseModel);
		useCaseRunner
			.reactTo(enterText(), enterText(), enterText(),
					enterText(), enterText(), enterText(),
					enterText(), enterText(), enterText(),
					enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_TEXT + ";" +
			CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	private Consumer<EnterText> displayEnteredTextAndIncrementCounter(){
		return enteredText -> {
			displayEnteredText().accept(enteredText);
			timesDisplayed++;
		};
	}
	
	@Test
	public void continueAtThirdStepCalledFromFirstStepOfAlternativeFlowWithoutEvent() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN)
					.step(CONTINUE).continueAt(CUSTOMER_ENTERS_NUMBER)
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterNumber());
		 
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CONTINUE + ";" +
			CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueAtCalledFromFirstStepOfAlternativeFlowWithRightActor() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()			
					.step(CUSTOMER_ENTERS_TEXT).as(secondActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).as(secondActor, rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).as(secondActor, rightActor).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT)
					.step(CONTINUE).as(rightActor).continueAt(CUSTOMER_ENTERS_TEXT_AGAIN)
			.build();
		
		useCaseRunner.as(rightActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterNumber());
		 
		assertEquals(CONTINUE + ";" + CUSTOMER_ENTERS_TEXT_AGAIN + ";" + CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueAtNotCalledWhenActorIsWrong() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()				
					.step(CUSTOMER_ENTERS_TEXT).as(secondActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).as(secondActor, rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).as(secondActor, rightActor).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT)
					.step(CONTINUE).as(rightActor).continueAt(CUSTOMER_ENTERS_TEXT_AGAIN)
			.build();
		
		useCaseRunner.as(secondActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText(), enterNumber());
		 
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_TEXT_AGAIN + ";" +  CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueAtCalledFromSecondStepOfAlternativeFlow() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()			
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN)
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CONTINUE).continueAt(CUSTOMER_ENTERS_NUMBER)
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterAlternativeText(), enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";" + CONTINUE + ";" +
			 CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueAtCalledFromMultipleMutuallyExclusiveAlternativeFlows() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()		
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN).when(textIsAvailable())
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CONTINUE).continueAt(CUSTOMER_ENTERS_NUMBER)		
				.flow(ALTERNATIVE_FLOW_2).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN).when(textIsNotAvailable())
					.step("Customer enters alternative number").user(EnterNumber.class).system(displayEnteredNumber())
					.step(CONTINUE_2).continueAt(CUSTOMER_ENTERS_NUMBER)
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterAlternativeText(), enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";" + CONTINUE + ";" + 
			CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueAfterSecondStepCalledFromFirstStepOfAlternativeFlowWithoutEvent() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN)
					.step(CONTINUE).continueAfter(CUSTOMER_ENTERS_TEXT_AGAIN)
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterNumber());
		 
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CONTINUE + ";" +
			CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueAfterCalledFromFirstStepOfAlternativeFlowWithRightActor() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).as(secondActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).as(secondActor, rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).as(secondActor, rightActor).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT)
					.step(CONTINUE).as(rightActor).continueAfter(CUSTOMER_ENTERS_TEXT)
			.build();
		
		useCaseRunner.as(rightActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterNumber());
		 
		assertEquals(CONTINUE + ";" + CUSTOMER_ENTERS_TEXT_AGAIN + ";" + CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueAfterNotCalledWhenActorIsWrong() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()			
					.step(CUSTOMER_ENTERS_TEXT).as(secondActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).as(secondActor, rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).as(secondActor, rightActor).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT)
					.step(CONTINUE).as(rightActor).continueAfter(CUSTOMER_ENTERS_TEXT)
			.build();
		
		useCaseRunner.as(secondActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText(), enterNumber());
		 
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_TEXT_AGAIN + ";" +  CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueAfterCalledFromSecondStepOfAlternativeFlow() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()	
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN)
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CONTINUE).continueAfter(CUSTOMER_ENTERS_TEXT_AGAIN)
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterAlternativeText(), enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";" + CONTINUE + ";" +
			 CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueAfterCalledFromMultipleMutuallyExclusiveAlternativeFlows() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN).when(textIsAvailable())
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CONTINUE).continueAfter(CUSTOMER_ENTERS_TEXT_AGAIN)		
				.flow(ALTERNATIVE_FLOW_2).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN).when(textIsNotAvailable())
					.step("Customer enters alternative number").user(EnterNumber.class).system(displayEnteredNumber())
					.step(CONTINUE_2).continueAfter(CUSTOMER_ENTERS_TEXT_AGAIN)
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterAlternativeText(), enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";" + CONTINUE + ";" + 
			CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueWithoutAlternativeAtFirstStepCalledFromFirstStepOfAlternativeFlowWithoutEvent() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT)
					.step(CONTINUE).continueWithoutAlternativeAt(CUSTOMER_ENTERS_TEXT)
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText());
		 
		assertEquals(CONTINUE + ";" + CUSTOMER_ENTERS_TEXT + ";" +
				CUSTOMER_ENTERS_TEXT_AGAIN + ";", runStepNames());
	}
	
	@Test
	public void continueWithoutAlternativeAtCalledFromFirstStepOfAlternativeFlowWithRightActor() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()
					.step(CUSTOMER_ENTERS_TEXT).as(rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).as(secondActor, rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).as(secondActor, rightActor).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT)
					.step(CONTINUE).as(rightActor).continueWithoutAlternativeAt(CUSTOMER_ENTERS_TEXT)
			.build();
		
		useCaseRunner.as(rightActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText(), enterNumber());
				 
		assertEquals(CONTINUE + ";" + CUSTOMER_ENTERS_TEXT + ";" +
			CUSTOMER_ENTERS_TEXT_AGAIN + ";" + CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueWithoutAlternativeAtNotCalledWhenActorIsWrong() {	
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()		
					.step(CUSTOMER_ENTERS_TEXT).as(secondActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).as(secondActor, rightActor).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).as(secondActor, rightActor).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT)
					.step(CONTINUE).as(rightActor).continueWithoutAlternativeAt(CUSTOMER_ENTERS_TEXT)
			.build();
		
		useCaseRunner.as(secondActor).run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterText(), enterNumber());
		 
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_TEXT_AGAIN + ";" +  CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueWithoutAlternativeAtCalledFromSecondStepOfAlternativeFlow() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()				
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN)
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CONTINUE).continueWithoutAlternativeAt(CUSTOMER_ENTERS_TEXT_AGAIN)
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterAlternativeText(), enterText(), enterNumber());
				
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";" + CONTINUE + ";" +
			CUSTOMER_ENTERS_TEXT_AGAIN + ";" + CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
	
	@Test
	public void continueWithoutAlternativeAtCalledFromMultipleMutuallyExclusiveAlternativeFlows() {		
		UseCaseModel useCaseModel = useCaseModelBuilder
			.useCase(USE_CASE)
				.basicFlow()		
					.step(CUSTOMER_ENTERS_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_TEXT_AGAIN).user(EnterText.class).system(displayEnteredText())
					.step(CUSTOMER_ENTERS_NUMBER).user(EnterNumber.class).system(displayEnteredNumber())		
				.flow(ALTERNATIVE_FLOW).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN).when(textIsAvailable())
					.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).user(EnterText.class).system(displayEnteredText())
					.step(CONTINUE).continueWithoutAlternativeAt(CUSTOMER_ENTERS_TEXT_AGAIN)		
				.flow(ALTERNATIVE_FLOW_2).insteadOf(CUSTOMER_ENTERS_TEXT_AGAIN).when(textIsNotAvailable())
					.step(CUSTOMER_ENTERS_NUMBER_AGAIN).user(EnterNumber.class).system(displayEnteredNumber())
					.step(CONTINUE_2).continueWithoutAlternativeAt(CUSTOMER_ENTERS_TEXT_AGAIN)
			.build();
		
		useCaseRunner.run(useCaseModel);
		useCaseRunner.reactTo(enterText(), enterAlternativeText(), enterText(), enterNumber());
		
		assertEquals(CUSTOMER_ENTERS_TEXT + ";" + CUSTOMER_ENTERS_ALTERNATIVE_TEXT + ";" + CONTINUE + ";" + 
			CUSTOMER_ENTERS_TEXT_AGAIN + ";" + CUSTOMER_ENTERS_NUMBER + ";", runStepNames());
	}
}