package org.requirementsascode;

import static org.hamcrest.core.Is.isA;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.requirementsascode.exception.ElementAlreadyInModel;
import org.requirementsascode.exception.MissingUseCaseStepPart;
import org.requirementsascode.exception.MoreThanOneStepCanReact;
import org.requirementsascode.exception.NoSuchElementInModel;
import org.requirementsascode.exception.UnhandledException;

public class ExceptionsThrownTest extends AbstractTestCase{		
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Before
	public void setup() {
		setupWith(new TestUseCaseModelRunner());
	}
	
	@Test
	public void throwsExceptionIfInsteadOfStepNotExistsInSameUseCase() {	
		thrown.expect(NoSuchElementInModel.class);
		thrown.expectMessage(CUSTOMER_ENTERS_TEXT);
		
		useCaseModelBuilder.useCase(USE_CASE)
			.basicFlow().insteadOf(CUSTOMER_ENTERS_TEXT);
	}
	
	@Test
	public void throwsExceptionIfAfterStepNotExistsInSameUseCase() {		
		thrown.expect(NoSuchElementInModel.class);
		thrown.expectMessage(CUSTOMER_ENTERS_TEXT);
		
		useCaseModelBuilder.useCase(USE_CASE)
			.basicFlow().after(CUSTOMER_ENTERS_TEXT);
	}
	
	@Test
	public void throwsExceptionIfContinueAfterNotExists() {		
		thrown.expect(NoSuchElementInModel.class);
		thrown.expectMessage(CONTINUE);
		
		useCaseModelBuilder.useCase(USE_CASE)
			.basicFlow().step("S1").continueAfter(CONTINUE);
	}
	
	@Test
	public void throwsExceptionIfContinueAtNotExists() {		
		thrown.expect(NoSuchElementInModel.class);
		thrown.expectMessage(CONTINUE);
		
		useCaseModelBuilder.useCase(USE_CASE)
			.basicFlow().step("S1").continueAt(CONTINUE);
	}
	
	@Test
	public void throwsExceptionIfContinueWithoutAlternativeAtNotExists() {		
		thrown.expect(NoSuchElementInModel.class);
		thrown.expectMessage(CONTINUE);
		
		useCaseModelBuilder.useCase(USE_CASE)
			.basicFlow().step("S1").continueWithoutAlternativeAt(CONTINUE);
	}
	
	@Test
	public void throwsExceptionIfActorIsCreatedTwice() {		
		thrown.expect(ElementAlreadyInModel.class);
		thrown.expectMessage(CUSTOMER);
		
		useCaseModelBuilder.actor(CUSTOMER);
		useCaseModelBuilder.actor(CUSTOMER);
	}
	
	@Test
	public void throwsExceptionIfUseCaseIsCreatedTwiceInOneGo() {		
		thrown.expect(ElementAlreadyInModel.class);
		thrown.expectMessage(USE_CASE);
		
		useCaseModelBuilder.useCase(USE_CASE);
		useCaseModelBuilder.useCase(USE_CASE);
	}
	
	@Test
	public void throwsExceptionIfUseCaseIsCreatedTwiceByBuildingOnExistingModel() {
		thrown.expect(ElementAlreadyInModel.class);
		thrown.expectMessage(USE_CASE);
		
		UseCaseModel useCaseModel = useCaseModelBuilder.useCase(USE_CASE).build();
		UseCaseModelBuilder.builderOf(useCaseModel).useCase(USE_CASE);

		Collection<UseCase> useCases = useCaseModel.getUseCases();
		assertEquals(2, useCases.size());
	}
	
	@Test
	public void throwsExceptionIfFlowIsCreatedTwice() {		
		thrown.expect(ElementAlreadyInModel.class);
		thrown.expectMessage(ALTERNATIVE_FLOW);
		
		useCaseModelBuilder.useCase(USE_CASE)
			.flow(ALTERNATIVE_FLOW)
				.step(SYSTEM_DISPLAYS_TEXT).system(displayConstantText())
			.flow(ALTERNATIVE_FLOW);
	}
	
	@Test
	public void throwsExceptionIfStepIsCreatedTwice() {		
		thrown.expect(ElementAlreadyInModel.class);
		thrown.expectMessage(CUSTOMER_ENTERS_TEXT);
		
		useCaseModelBuilder.useCase(USE_CASE)
			.basicFlow()
				.step(CUSTOMER_ENTERS_TEXT).system(displayConstantText())			
				.step(CUSTOMER_ENTERS_TEXT).system(displayConstantText());
	}
	
	@Test
	public void throwsExceptionIfMoreThanOneStepCouldReact() { 	 
		thrown.expect(MoreThanOneStepCanReact.class);
		thrown.expectMessage(CUSTOMER_ENTERS_TEXT);
		thrown.expectMessage(CUSTOMER_ENTERS_ALTERNATIVE_TEXT);
		
		UseCaseModel useCaseModel = useCaseModelBuilder.useCase(USE_CASE)
			.basicFlow().when(run -> true)
				.step(CUSTOMER_ENTERS_TEXT).system(displayConstantText())
			.flow(ALTERNATIVE_FLOW).when(run -> true)
				.step(CUSTOMER_ENTERS_ALTERNATIVE_TEXT).system(displayConstantText())
			.build();
		
		useCaseModelRunner.run(useCaseModel);
	}
	
	@Test
	public void throwsExceptionIfActorPartIsNotSpecified() {		
		thrown.expect(MissingUseCaseStepPart.class);
		thrown.expectMessage(CUSTOMER_ENTERS_TEXT);
		
		useCaseModelBuilder.useCase(USE_CASE)
			.basicFlow()
				.step(CUSTOMER_ENTERS_TEXT);
		
		UseCaseModel useCaseModel = useCaseModelBuilder.build();
			
		useCaseModelRunner.run(useCaseModel);
	}
	
	@Test
	public void throwsExceptionIfSystemPartIsNotSpecified() {		
		thrown.expect(MissingUseCaseStepPart.class);
		thrown.expectMessage(CUSTOMER_ENTERS_TEXT);
		
		useCaseModelBuilder.useCase(USE_CASE)
			.basicFlow()
				.step(CUSTOMER_ENTERS_TEXT)
					.as(customer).user(EnterText.class);
		
		UseCaseModel useCaseModel = useCaseModelBuilder.build();
			
		useCaseModelRunner.as(customer).run(useCaseModel);
		useCaseModelRunner.reactTo(enterText());		
	}
	
	@Test
	public void throwsUncaughtExceptionIfExceptionIsNotHandled() {		
		thrown.expect(UnhandledException.class);
		thrown.expectCause(isA(IllegalStateException.class));
		
		useCaseModelBuilder.useCase(USE_CASE)
			.basicFlow()
				.step(CUSTOMER_ENTERS_TEXT)
					.system(r -> {throw new IllegalStateException();});
		
		UseCaseModel useCaseModel = useCaseModelBuilder.build();
		
		useCaseModelRunner.run(useCaseModel);
	}
}
