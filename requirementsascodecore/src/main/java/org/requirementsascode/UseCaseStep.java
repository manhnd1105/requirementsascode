package org.requirementsascode;

import static org.requirementsascode.UseCaseStepPredicate.afterStep;
import static org.requirementsascode.UseCaseStepPredicate.atFirstStep;
import static org.requirementsascode.UseCaseStepPredicate.noOtherStepIsEnabledThan;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class UseCaseStep extends UseCaseModelElement{
	private UseCaseFlow useCaseFlow;
	private Optional<UseCaseStep> previousStep;
	private Optional<Predicate<UseCaseRunner>> predicate;
	
	private ActorPart<?> actorPart;
	private SystemPart<?> systemPart;
		
	UseCaseStep(String stepName, UseCaseFlow useCaseFlow, Optional<UseCaseStep> previousStep, Optional<Predicate<UseCaseRunner>> predicate) {
		super(stepName, useCaseFlow.getUseCaseModel());
		Objects.requireNonNull(previousStep);
		Objects.requireNonNull(predicate);
		
		this.useCaseFlow = useCaseFlow;
		this.previousStep = previousStep;
		this.predicate = predicate;		
	}
	
	public <U> UseCaseStep.ActorPart<U> actor(Actor actor, Class<U> eventClass) {
		Objects.requireNonNull(actor);
		Objects.requireNonNull(eventClass);
		
		ActorPart<U> newActorPart = new ActorPart<>(actor, eventClass);
		this.actorPart = newActorPart;
		return newActorPart;
	}
	
	public UseCaseStep.SystemPart<?> system(Runnable autonomousSystemReaction) {
		Objects.requireNonNull(autonomousSystemReaction);
		
		Actor autoReactionActor = getUseCaseModel().getSystemActor();
		
		UseCaseStep.SystemPart<?> systemPart =
			actor(autoReactionActor, SystemEvent.class).
				system((o) -> autonomousSystemReaction.run());
		
		return systemPart;
	}
	
	public <T> ActorPart<T> handle(Class<T> exceptionOrEventClass) {
		Objects.requireNonNull(exceptionOrEventClass);

		Actor autoReactionActor = getUseCaseModel().getSystemActor();
		ActorPart<T> newActorPart = actor(autoReactionActor, exceptionOrEventClass);
		this.actorPart = newActorPart;
		
		return newActorPart;
	}
	
	public UseCaseFlow getUseCaseFlow() {
		return useCaseFlow;
	}
	
	public UseCase getUseCase() {
		return getUseCaseFlow().getUseCase();
	}
	
	public Optional<UseCaseStep> getPreviousStep() {
		return previousStep;
	}
	
	public ActorPart<?> getActorPart() {
		return actorPart;
	}
	
	public SystemPart<?> getSystemPart() {
		return systemPart;
	}
	
	public Predicate<UseCaseRunner> getPredicate() {
		return predicate.orElse(afterPreviousStepWhenNoOtherStepIsEnabled());
	} 
	
	private Predicate<UseCaseRunner> afterPreviousStepWhenNoOtherStepIsEnabled() {
		Predicate<UseCaseRunner> afterPreviousStepPredicate = 
			previousStep.map(s -> afterStep(previousStep)).orElse(atFirstStep());
		return afterPreviousStepPredicate.and(noOtherStepIsEnabledThan(this));
	}
	
	public class ActorPart<T>{
		private Actor namedActor;
		private Class<T> eventClass;
		
		private ActorPart(Actor actor, Class<T> eventClass) {
			this.namedActor = actor;
			this.eventClass = eventClass;
			connectActorToThisStep(namedActor);		
		}

		private void connectActorToThisStep(Actor actor) {
			actor.newStep(getUseCase(), UseCaseStep.this);
		}
		
		public SystemPart<T> system(Consumer<T> systemReaction) {
			Objects.requireNonNull(systemReaction);
			
			SystemPart<T> newSystemPart = new SystemPart<>(systemReaction);
			UseCaseStep.this.systemPart = newSystemPart;
			return newSystemPart;		
		}
		
		public Actor getActor() {
			return namedActor;
		}
		
		public Class<T> getEventClass() {
			return eventClass;
		}
	}
	
	public class SystemPart<T>{
		private Consumer<T> systemReaction;

		private SystemPart(Consumer<T> systemReaction) {
			this.systemReaction = systemReaction;
		}
		
		public Consumer<T> getSystemReaction() {
			return systemReaction;
		}
		
		public UseCase newUseCase(String useCaseName) {
			Objects.requireNonNull(useCaseName);

			UseCase newUseCase = getUseCaseModel().newUseCase(useCaseName);
			return newUseCase;
		}
		
		public UseCaseFlow newFlow(String flowName) {
			Objects.requireNonNull(flowName);

			UseCaseFlow newFlow = getUseCase().newFlow(flowName);
			return newFlow;
		}

		public UseCaseStep newStep(String stepName) {			
			Objects.requireNonNull(stepName);

			UseCaseStep newStep = 
				getUseCase().newStep(stepName, getUseCaseFlow(), Optional.of(UseCaseStep.this), Optional.empty());
			
			return newStep; 
		}
		
		public SystemPart<T> repeatWhile(Predicate<UseCaseRunner> condition) {
			Objects.requireNonNull(condition);
			
			String thisStepName = getName();
			String newRepeatStepName = uniqueRepeatStepName();
			
			UseCaseStep newRepeatStep = newFlow(newRepeatStepName)
				.after(thisStepName).when(condition).newStep(newRepeatStepName);
			
			makeRepeatStepBehaveLikeThisStep(newRepeatStep);
			
			getUseCaseFlow().continueAfter(thisStepName, Optional.of(newRepeatStep), Optional.empty());
			
			return this;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void makeRepeatStepBehaveLikeThisStep(UseCaseStep newRepeatStep) {
			newRepeatStep
				.actor(getActorPart().getActor(), getActorPart().getEventClass())
				.system((Consumer)getSystemPart().getSystemReaction());
		}
		
		public UseCase continueAfter(String stepName) {
			Objects.requireNonNull(stepName);
			
			getUseCaseFlow().continueAfter(stepName, Optional.of(UseCaseStep.this), Optional.empty());
			return getUseCase();
		}

		public void reset() {
			newStep(uniqueResetStepName()).system(
				() -> getUseCaseModel().getUseCaseRunner().reset());
		}
	}
	
	private String uniqueRepeatStepName() {
		return uniqueStepName(getName(), "REPEAT");
	}
	
	private String uniqueResetStepName() {
		return uniqueStepName("Use Case Run resets");
	}
}
